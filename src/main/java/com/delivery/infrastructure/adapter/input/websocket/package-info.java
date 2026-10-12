/**
 * ═══════════════════════════════════════════════════════════════════════
 * INFRASTRUCTURE — Adapter In: WebSocket Handlers
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Adaptadores de entrada para comunicación bidireccional en tiempo real
 * (tracking de entregas, notificaciones push, chat).
 *
 * ✅ Anotados con @MessageMapping / @Controller (STOMP) o WebSocketHandler
 * ✅ Invocan Input Ports igual que los REST Controllers
 *
 * Convención: [Feature]WebSocketHandler
 *   Ejemplos: DeliveryTrackingHandler, NotificationHandler
 */
package com.delivery.infrastructure.adapter.input.websocket;
