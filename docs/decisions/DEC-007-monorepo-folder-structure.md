# [DEC-007] Estructura Canónica de Carpetas del Proyecto: Monorepo Hexagonal + Frontend Modular

- **Estado**: CONFIRMADA Y VINCULANTE
- **Fecha**: 2026-10-12
- **Dominio**: Arquitectura de Software y Organización del Repositorio
- **Actores del Sistema**: `Client`, `Merchant`, `Driver`, `Admin`

---

## 1. Justificación Arquitectural (Rationale)
1. **Mantenibilidad & Aislamiento Estricto de Roles**: Separación física y lógica de los flujos de `Client`, `Merchant`, `Driver` y `Admin` para erradicar acoplamientos indeseados.
2. **Dominio Agnóstico de Frameworks (Java Puro)**: La capa `domain/` no contiene anotaciones de Spring Boot, JPA o bibliotecas de infraestructura, garantizando inmunidad a cambios tecnológicos.
3. **Frontend Especializado por Actor**: Cada módulo en `frontend/src/modules/` cuenta con su propia tríada `pages/`, `components/` y `hooks/`, evitando fugas de dependencias y de interfaces.
4. **Prevención de Mezcla de Dependencias**: El código de cocina/tablets (`merchant`), el tracking GPS móvil (`driver`), el checkout anti-fraude (`client`) y el panel de auditoría fiscal (`admin`) no comparten bundles innecesarios.

---

## 2. Árbol de Directorios Canónico

```text
sistema-delivery-root/
│
├── backend/                             # BACKEND SPRING BOOT 3.X (ARQUITECTURA HEXAGONAL)
│   └── src/main/java/com/delivery/
│       ├── domain/                      # 1. CAPA DE DOMINIO (JAVA PURO - SIN SPRING)
│       │   ├── model/
│       │   │   ├── user/                # [MÓDULO COMPARTIDO]: User, Role, Credentials
│       │   │   ├── client/              # [MÓDULO CLIENTE]: Client, SavedAddress, PaymentMethod
│       │   │   ├── merchant/            # [MÓDULO RESTAURANTE]: Merchant, Product, Category, Schedule
│       │   │   ├── driver/              # [MÓDULO REPARTIDOR]: Driver, Vehicle, DriverStatus, GPSLocation
│       │   │   ├── admin/               # [MÓDULO ADMIN]: AuditLog, SystemConfig, CommissionRate
│       │   │   └── order/               # [MÓDULO PEDIDOS]: Order, OrderItem, OrderStatus, DeliveryPin
│       │   ├── ports/
│       │   │   ├── in/                  # Puertos de Entrada (Casos de Uso por Rol)
│       │   │   │   ├── client/          # CreateOrderUseCase, SearchMerchantsUseCase
│       │   │   │   ├── merchant/        # ManageCatalogUseCase, AcceptOrderUseCase, ToggleStockUseCase
│       │   │   │   ├── driver/          # AcceptDeliveryUseCase, UpdateGpsLocationUseCase, ValidatePinUseCase
│       │   │   │   └── admin/           # ApproveMerchantUseCase, ApproveDriverUseCase, LiquidateFundsUseCase
│       │   │   └── out/                 # Puertos de Salida (Repositorios / Notificaciones / Pagos)
│       │   │       ├── OrderRepositoryPort.java
│       │   │       ├── MerchantRepositoryPort.java
│       │   │       ├── DriverRepositoryPort.java
│       │   │       ├── NotificationPort.java
│       │   │       └── PaymentGatewayPort.java
│       │   └── exception/               # Excepciones de Dominio (DomainException)
│       │
│       ├── application/                 # 2. CAPA DE APLICACIÓN (SERVICIOS Y CASOS DE USO)
│       │   ├── service/
│       │   │   ├── client/              # ClientOrderService, ClientCatalogService
│       │   │   ├── merchant/            # MerchantOrderService, MerchantProductService
│       │   │   ├── driver/              # DriverDeliveryService, DriverLocationService
│       │   │   └── admin/               # AdminOnboardingService, AdminFinanceService
│       │   └── dto/                     # Commands y Queries puros
│       │
│       └── infrastructure/              # 3. CAPA DE INFRAESTRUCTURA (ADAPTADORES OUT Y CONTROLLERS IN)
│           ├── adapters/
│           │   ├── in/
│           │   │   ├── rest/
│           │   │   │   ├── client/      # ClientOrderController, ClientCatalogController
│           │   │   │   ├── merchant/    # MerchantOrderController, MerchantProductController
│           │   │   │   ├── driver/      # DriverDeliveryController, DriverLocationController
│           │   │   │   └── admin/       # AdminManagementController, AdminFinanceController
│           │   │   └── websocket/       # DriverGpsWebSocketHandler, MerchantAlarmWebSocketHandler
│           │   └── out/
│           │       ├── persistence/     # Entities JPA, Repositorios Spring Data, PostGIS Adapters
│           │       ├── messaging/       # WebSocket Adapters, Firebase Push Notifications
│           │       └── payment/         # Stripe / PayPal Adapters
│           └── config/                  # Spring Security, JWT, WebSockets, PostGIS Configuration
│
└── frontend/                            # FRONTEND MODULAR (REACT + TAILWIND CSS)
    ├── src/
    │   ├── assets/                      # Estilos globales de Tailwind, imágenes, sonidos (alarma.mp3)
    │   ├── components/ui/               # Componentes UI reutilizables (Botones Tailwind, Modales, Inputs)
    │   ├── context/                     # AuthContext, WebSocketContext
    │   │
    │   └── modules/                     # MÓDULOS DE USUARIO SEPARADOS
    │       ├── client/                  # MÓDULO 1: APP / WEB CLIENTE
    │       │   ├── pages/               # Home, RestaurantDetail, Cart, Checkout, TrackingPage
    │       │   ├── components/          # ProductCard, OtpPinDisplay, LiveMapTracker
    │       │   └── hooks/               # useCart, useOrderTracking
    │       │
    │       ├── merchant/                # MÓDULO 2: DASHBOARD RESTAURANTE (COCINA / TABLET)
    │       │   ├── pages/               # KitchenDashboard, MenuManager, SalesHistory
    │       │   ├── components/          # OrderAlarmCard, StockToggleSwitch, KitchenTimePicker
    │       │   └── hooks/               # useKitchenOrders, useSoundAlarm
    │       │
    │       ├── driver/                  # MÓDULO 3: APP REPARTIDOR (MOBILE FIRST)
    │       │   ├── pages/               # AvailableDeliveries, ActiveTrip, PinValidationPage
    │       │   ├── components/          # RouteMap, OtpInputModal, EmergencyButton
    │       │   └── hooks/               # useGpsBroadcaster, useDriverTrip
    │       │
    │       └── admin/                   # MÓDULO 4: PANEL DE ADMINISTRACIÓN GLOBAL
    │           ├── pages/               # MerchantApprovals, DriverApprovals, SystemMetrics, Dispatches
    │           ├── components/          # VerificationTable, CommissionEditor, LiveFleetMap
    │           └── hooks/               # useAdminApprovals, useFleetMonitoring
    │
    ├── tailwind.config.js               # Configuración de Tailwind CSS
    └── vite.config.js                   # Configuración de Vite / React
```

---

## 3. Estado de Correspondencia con Código Existente

### Frontend
- [`TrackingPage.jsx`](file:///c:/Users/tirac/Documents/Proyecto-att/src/modules/client/pages/TrackingPage.jsx) ➔ Mapea a `frontend/src/modules/client/pages/TrackingPage.jsx`.
- [`ActiveTrip.jsx`](file:///c:/Users/tirac/Documents/Proyecto-att/src/modules/driver/pages/ActiveTrip.jsx) ➔ Mapea a `frontend/src/modules/driver/pages/ActiveTrip.jsx`.
- [`MerchantApprovals.jsx`](file:///c:/Users/tirac/Documents/Proyecto-att/src/modules/admin/pages/MerchantApprovals.jsx) ➔ Mapea a `frontend/src/modules/admin/pages/MerchantApprovals.jsx`.
- Módulo `merchant` (Dashboard Restaurante/Cocina): listo para alojar `KitchenDashboard.jsx`, `MenuManager.jsx` y `useSoundAlarm.js`.

### Backend
- Modelos centrales como `DeliveryPin`, `Order`, `CreateOrderCommand` y `ValidateDeliveryPinUseCase` quedan estructurados bajo `domain/model/order/` y `domain/ports/in/driver/`.
- Repositorios y adaptadores de notificación (`NotificationPort`) quedan en `domain/ports/out/` e `infrastructure/adapters/out/`.
