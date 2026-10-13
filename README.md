# MAD-Delivery (Proyecto ATT)

Sistema Delivery Multinivel BMCP con Arquitectura Hexagonal, PostGIS, Redis y Frontend Tulcán.

---

## 🏛️ Estructura del Proyecto

El repositorio está organizado como un monorepo modular:

```text
├── backend/                  # Servidor Spring Boot 3.x (Java 21)
│   ├── src/main/java/com/delivery/
│   │   ├── domain/           # Entidades de dominio, puertos y lógica core
│   │   ├── application/      # Casos de uso
│   │   ├── infrastructure/   # Adaptadores REST, JPA, PostGIS, Redis, Seguridad JWT
│   │   └── modules/          # Módulos de negocio (Admin, Client, Driver, Merchant)
│   └── pom.xml
├── frontend/                 # Aplicación Web React 18 + Vite + Tailwind CSS
│   ├── src/
│   │   ├── modules/          # Vistas (Admin, Client, Driver, Merchant)
│   │   └── utils/
│   └── package.json
├── docs/                     # Registros de decisiones de arquitectura (ADRs)
└── docker-compose.yml        # PostgreSQL / PostGIS y Redis
```

---

## 🚀 Requisitos Previos

- **Java**: 21 (OpenJDK / Eclipse Temurin)
- **Maven**: 3.9+
- **Node.js**: 18+ y npm
- **Docker & Docker Compose**: Para PostgreSQL con PostGIS y Redis

---

## 🛠️ Puesta en Marcha

### 1. Infraestructura con Docker
```bash
docker compose up -d
```

### 2. Backend (Spring Boot)
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 3. Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```

---

## 📦 Repositorios Sincronizados

- **GitLab UPEC Principal**: `https://gitlab.upec.edu.ec/Tenshii/proyecto-att.git`
- **GitLab UPEC Mirror**: `https://gitlab.upec.edu.ec/michaelt123/mad-delivery.git`
