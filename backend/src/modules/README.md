# MÓDULOS DEL BACKEND — SPRING BOOT 3.X (JAVA 21)

En este servidor backend, todos los módulos de negocio se construyen **estrictamente en Java 21 y Spring Boot**, organizados bajo el paquete principal:

```text
backend/src/main/java/com/delivery/modules/
├── admin/       --> MerchantApprovalsController, MerchantApprovalsService, RUC Validator
├── client/      --> ClientTrackingController, ClientTrackingService, OrderTracking
├── driver/      --> DriverActiveTripController, DriverActiveTripService, PIN OTP
└── merchant/    --> MerchantKitchenController, MerchantKitchenService, Stock Toggle
```

> **Aviso de Arquitectura:**
> Los componentes visuales de interfaz en React (`.jsx`) pertenecen única y exclusivamente al directorio `frontend/src/modules/`.
> La capa de servidor no utiliza JavaScript ni Node.js.
