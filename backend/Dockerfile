# ═══════════════════════════════════════════════════════════════════════
# Dockerfile — Backend Spring Boot (Delivery Multinivel BMCP)
# Multi-stage build optimizado para Java 21 y producción segura
# ═══════════════════════════════════════════════════════════════════════

# ── Etapa 1: Compilación y Empaquetado ────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Cachear dependencias de Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compilar código fuente y generar JAR ejecutable
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Etapa 2: Imagen de Ejecución Ligera y Segura ──────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios para ejecución segura
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar el JAR compilado desde la etapa de compilación
COPY --from=builder /workspace/target/delivery-multinivel-*.jar app.jar

# Asignar permisos al usuario de aplicación
RUN chown -R appuser:appgroup /app
USER appuser

# Variables de entorno por defecto (personalizables en despliegue)
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
