# [DEC-008] Uso de Redis con TTL para Gestión de Estado Temporal de Pedidos

- **Estado**: CONFIRMADA Y VINCULANTE
- **Fecha**: 2026-10-12
- **Dominio**: Infraestructura de Rendimiento y Seguridad Backend (L1 Cache & Eventos Reactivos)
- **Capa Afectada**: `backend/src/main/java/com/delivery/infrastructure/adapter/output/redis/`
- **Módulos del Sistema**: `Driver` (Tracking GPS), `Order` (Anti-Fraude PIN OTP & Timeouts de Cocina)

---

## 1. Contexto & Problemática
En una plataforma de delivery de alta concurrencia, persistir estados altamente volátiles directamente en PostgreSQL/PostGIS genera los siguientes cuellos de botella:
1. **Saturación de I/O en Base de Datos**: Cada repartidor activo emite telemetría GPS cada 3-5 segundos. Persistir cada ping directamente en disco colapsaría las IOPS y la replicación relacional.
2. **Vulnerabilidad a Ataques de Fuerza Bruta (PIN OTP)**: El PIN de entrega de 4 dígitos (0000 - 9999) requiere rate-limiting atómico y de bajísima latencia para impedir ataques por diccionario o adivinación en el domicilio.
3. **Polling Ineficiente para Cancelación por Timeout**: Detectar pedidos que el restaurante no confirmó tras 5 minutos mediante consultas periódicas (`SELECT ... WHERE status = 'CREATED' AND created_at < NOW() - INTERVAL '5 minutes'`) desperdicia CPU y bloquea tablas.

---

## 2. Decisión Arquitectural
Se adopta formalmente **Redis (v7.x en memoria)** como almacén L1 de estado temporal reactivo con tres políticas de Time-To-Live (TTL):

### 2.1. Rastreo GPS de Repartidores (TTL = 15 Segundos)
- **Clave**: `driver:location:{driverId}`
- **Formato**: JSON `{"latitude": -0.8123, "longitude": -77.7123, "updatedAt": "...", "heading": 180.0, "speed": 25.5}`
- **TTL**: 15 segundos (`Duration.ofSeconds(15)`).
- **Comportamiento**:
  - Si el repartidor está en ruta y emite cada 3 segundos, la clave se refresca continuamente en memoria.
  - Si el dispositivo pierde cobertura o se apaga, la clave expira en 15 segundos de forma automática, desapareciendo del mapa en vivo sin necesidad de tareas batch o daemons de limpieza.
- **Implementación**: [`RedisDriverLocationAdapter.java`](file:///c:/Users/tirac/Documents/Proyecto-att/src/main/java/com/delivery/infrastructure/adapter/output/redis/RedisDriverLocationAdapter.java).

### 2.2. Rate Limiting de Validación PIN OTP (Max 3 Intentos / TTL = 5 Minutos)
- **Clave**: `order:pin_attempts:{orderId}`
- **Contador**: Incremento atómico mediante comando `INCR`.
- **TTL**: 5 minutos (`Duration.ofMinutes(5)`), establecido al primer intento fallido.
- **Umbral**: Si el contador alcanza o supera 3 intentos, se arroja [`TooManyPinAttemptsException`](file:///c:/Users/tirac/Documents/Proyecto-att/src/main/java/com/delivery/domain/exception/TooManyPinAttemptsException.java).
- **Reset**: Al validar exitosamente el PIN, la clave se elimina con `DEL`.
- **Implementación**: [`RedisPinRateLimiterAdapter.java`](file:///c:/Users/tirac/Documents/Proyecto-att/src/main/java/com/delivery/infrastructure/adapter/output/redis/RedisPinRateLimiterAdapter.java).

### 2.3. Timeout de Aceptación del Restaurante (Keyspace Notifications / TTL = 5 Minutos)
- **Clave**: `order:unaccepted:{orderId}`
- **TTL**: 5 minutos tras la creación del pedido (`OrderStatus.CREATED`).
- **Mecanismo Reactivo**:
  - Redis configurado con flag de eventos `--notify-keyspace-events KEx`.
  - Spring Boot escucha el canal Pub/Sub `__keyevent@*__:expired`.
  - Si el restaurante no confirma en 5 minutos, la clave expira. El listener captura el evento, cancela el pedido en la base de datos relacional y despacha notificaciones de reembolso al cliente y de penalización al comercio.
- **Implementación**: [`OrderUnacceptedExpirationListener.java`](file:///c:/Users/tirac/Documents/Proyecto-att/src/main/java/com/delivery/infrastructure/adapter/output/redis/listener/OrderUnacceptedExpirationListener.java) y [`RedisOrderExpirationAdapter.java`](file:///c:/Users/tirac/Documents/Proyecto-att/src/main/java/com/delivery/infrastructure/adapter/output/redis/RedisOrderExpirationAdapter.java).

---

## 3. Beneficios y Racionalidad
1. **Reducción del 95% de Escrituras en PostgreSQL**: Las coordenadas GPS solo se persisten en PostGIS al completar hitos clave (recogida y entrega), no en cada milisegundo de trayecto.
2. **Mitigación Estricta de Fuerza Bruta**: Con solo 3 intentos en 5 minutos, la probabilidad matemática de vulnerar un PIN de 4 dígitos al azar es de apenas 0.03%.
3. **Cancelaciones Serverless en Tiempo Real**: Cero cron jobs en base de datos; la expiración es conducida por eventos atómicos de Redis en tiempo real.

---

## 4. Archivos Vinculados en el Repositorio

| Componente | Rol en la Decisión | Ubicación |
| :--- | :--- | :--- |
| **Configuración** | Conexión, Serializadores y MessageContainer | [`RedisConfig.java`](file:///c:/Users/tirac/Documents/Proyecto-att/src/main/java/com/delivery/infrastructure/config/RedisConfig.java) |
| **GPS Cache Adapter** | Escritura con TTL 15s y lectura volátil | [`RedisDriverLocationAdapter.java`](file:///c:/Users/tirac/Documents/Proyecto-att/src/main/java/com/delivery/infrastructure/adapter/output/redis/RedisDriverLocationAdapter.java) |
| **Rate Limiter Adapter** | INCR atómico y bloqueo por 5 min | [`RedisPinRateLimiterAdapter.java`](file:///c:/Users/tirac/Documents/Proyecto-att/src/main/java/com/delivery/infrastructure/adapter/output/redis/RedisPinRateLimiterAdapter.java) |
| **Keyspace Listener** | Manejador de eventos `__keyevent@*__:expired` | [`OrderUnacceptedExpirationListener.java`](file:///c:/Users/tirac/Documents/Proyecto-att/src/main/java/com/delivery/infrastructure/adapter/output/redis/listener/OrderUnacceptedExpirationListener.java) |
| **Expiración Port/Adapter** | Programación de timeout al crear pedido | [`RedisOrderExpirationAdapter.java`](file:///c:/Users/tirac/Documents/Proyecto-att/src/main/java/com/delivery/infrastructure/adapter/output/redis/RedisOrderExpirationAdapter.java) |
| **Docker Compose** | Contenedor Redis con flag `KEx` activada | [`docker-compose.yml`](file:///c:/Users/tirac/Documents/Proyecto-att/docker-compose.yml) |
| **Suite de Tests** | Pruebas unitarias completas de TTL y bloqueos | [`RedisPinRateLimiterAdapterTest.java`](file:///c:/Users/tirac/Documents/Proyecto-att/src/test/java/com/delivery/infrastructure/adapter/output/redis/RedisPinRateLimiterAdapterTest.java), [`RedisDriverLocationAdapterTest.java`](file:///c:/Users/tirac/Documents/Proyecto-att/src/test/java/com/delivery/infrastructure/adapter/output/redis/RedisDriverLocationAdapterTest.java), [`OrderUnacceptedExpirationListenerTest.java`](file:///c:/Users/tirac/Documents/Proyecto-att/src/test/java/com/delivery/infrastructure/adapter/output/redis/listener/OrderUnacceptedExpirationListenerTest.java) |
