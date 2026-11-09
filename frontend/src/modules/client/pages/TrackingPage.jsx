import React, { useState, useEffect, useRef, useMemo } from 'react';
import { getWebSocketUrl, buildApiUrl, getAuthToken } from '../../../services/api';

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * TRACKING PAGE — MÓDULO DEL CLIENTE (SISTEMA DE DELIVERY MULTINIVEL BMCP)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Características principales:
 * 1. Encabezado dinámico con stepper reactivo de estados (CREADO, EN PREPARACIÓN, EN CAMINO, ENTREGADO).
 * 2. Tarjeta Anti-Fraude destacada con PIN OTP gigante (caja amarilla con borde ámbar de alta visibilidad).
 * 3. Mapa interactivo en tiempo real con suscripción STOMP WebSocket a `/topic/orders/{orderId}/tracking`
 *    y motor de simulación de respaldo para pruebas locales.
 * 4. Botón de Acción Flotante (FAB) con menú desplegable para llamada telefónica y chat en vivo con repartidor.
 */

// ── ICONOS SVG EMBEBIDOS (Cero dependencias externas requeridas) ─────────────
const Icons = {
  ShieldCheck: () => (
    <svg className="w-6 h-6 text-yellow-700" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  ),
  Lock: () => (
    <svg className="w-5 h-5 text-yellow-800" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
    </svg>
  ),
  Check: () => (
    <svg className="w-4 h-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
    </svg>
  ),
  Clock: () => (
    <svg className="w-5 h-5 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  ),
  Phone: () => (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
    </svg>
  ),
  Chat: () => (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
    </svg>
  ),
  Close: () => (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
    </svg>
  ),
  Send: () => (
    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
    </svg>
  ),
  Store: () => (
    <svg className="w-5 h-5 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
    </svg>
  ),
  Home: () => (
    <svg className="w-5 h-5 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
    </svg>
  ),
  Motorcycle: () => (
    <svg className="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 24 24">
      <path d="M19.5 12c-1.38 0-2.5 1.12-2.5 2.5s1.12 2.5 2.5 2.5 2.5-1.12 2.5-2.5-1.12-2.5-2.5-2.5zm0 3.5c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-15-3.5c-1.38 0-2.5 1.12-2.5 2.5s1.12 2.5 2.5 2.5 2.5-1.12 2.5-2.5-1.12-2.5-2.5-2.5zm0 3.5c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm12.3-6.5l-2.8-2.8c-.38-.38-.88-.6-1.41-.6H9c-.55 0-1 .45-1 1s.45 1 1 1h3.17l1.7 1.7-4.14 4.14c-.38-.34-.88-.54-1.43-.54-1.93 0-3.5 1.57-3.5 3.5s1.57 3.5 3.5 3.5c1.78 0 3.25-1.33 3.47-3.06l3.96-2.64 1.77 1.77c-.55.77-.9 1.71-.9 2.73 0 2.48 2.02 4.5 4.5 4.5s4.5-2.02 4.5-4.5-2.02-4.5-4.5-4.5c-.34 0-.67.04-1 .11l-2.03-3.05z" />
    </svg>
  ),
  Navigation: () => (
    <svg className="w-4 h-4 text-indigo-500 animate-pulse" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
  ),
  Star: () => (
    <svg className="w-4 h-4 text-amber-400 fill-amber-400" viewBox="0 0 20 20">
      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
    </svg>
  )
};

// ── DEFINICIÓN DE PASOS SEGÚN BMCP ──────────────────────────────────────────
const ORDER_STEPS = [
  { key: 'CREADO', label: 'CREADO', sublabel: 'Orden confirmada' },
  { key: 'EN_PREPARACION', label: 'EN PREPARACIÓN', sublabel: 'Cocina preparando' },
  { key: 'EN_CAMINO', label: 'EN CAMINO', sublabel: 'Repartidor en ruta' },
  { key: 'ENTREGADO', label: 'ENTREGADO', sublabel: 'PIN validado' }
];

export default function TrackingPage({
  orderId = 'a47b198c-8f12-4e9b-b6d3-2e40bf1d3902',
  initialOrder = null,
  onBackToMenu = null
}) {
  // Helper para recuperar la orden activa persistida o inicial
  const getActiveOrderData = () => {
    if (initialOrder) return initialOrder;
    try {
      const stored = localStorage.getItem('delivery_active_order');
      if (stored) return JSON.parse(stored);
    } catch (e) {
      console.warn('Error leyendo delivery_active_order:', e);
    }
    return null;
  };

  // ── ESTADOS DEL PEDIDO ───────────────────────────────────────────────────
  const [orderState, setOrderState] = useState(() => {
    const active = getActiveOrderData();
    if (active) {
      return {
        id: active.id || orderId,
        orderNumber: active.orderNumber || '#1048',
        status: active.status || 'EN_CAMINO',
        deliveryPin: active.deliveryPin || '4829',
        merchant: {
          name: active.restaurantName || (active.pickupAddress ? active.pickupAddress.split(',')[0] : 'Asadero El Leñador'),
          address: active.restaurantAddress || active.pickupAddress || 'Av. Sucre y Bolívar, Centro Comercial, Tulcán',
          lat: 0.8116,
          lon: -77.7172
        },
        customer: {
          name: active.customerName || 'Valeria Narváez',
          address: active.deliveryAddress || 'Calle Brasil y Av. Tulcanaza, Sector Terminal Terrestre, Tulcán',
          lat: 0.8035,
          lon: -77.7245
        },
        driver: {
          name: 'Carlos Mendoza',
          phone: '+593 99 491 2384',
          level: 'SENIOR',
          rating: 4.95,
          vehicle: 'Shineray XY200 • Placas IC-482Y (Carchi)',
          deliveries: 1240,
          avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=250'
        },
        items: active.items && active.items.length > 0 ? active.items : [
          { name: 'Combo Pollo Asado Completo', quantity: 1, price: '11.00' }
        ],
        subtotal: active.subtotal || active.purchasePrice || '11.00',
        shippingCost: active.shippingCost || active.shippingFee || '1.50',
        totalAmount: active.totalAmount || '12.50',
        paymentMethod: active.paymentMethod || 'EFECTIVO',
        currency: '$ USD',
        createdAt: active.createdAt || new Date().toISOString()
      };
    }
    return {
      id: orderId,
      orderNumber: '#1048',
      status: 'EN_CAMINO', // CREADO | EN_PREPARACION | EN_CAMINO | ENTREGADO
      deliveryPin: '4829', // PIN OTP de 4 dígitos generado en el backend
      merchant: {
        name: 'Asadero El Leñador',
        address: 'Calle Sucre 14-20 y Pichincha, Parque Central, Tulcán, Carchi',
        lat: 0.8116,
        lon: -77.7172
      },
      customer: {
        name: 'Valeria Narváez',
        address: 'Av. Coral 450 y Av. Manabí, Sector Terminal Terrestre, Tulcán, Carchi',
        lat: 0.8035,
        lon: -77.7245
      },
      driver: {
        name: 'Carlos Mendoza',
        phone: '+593 99 491 2384',
        level: 'SENIOR',
        rating: 4.95,
        vehicle: 'Shineray XY200 • Placas IC-482Y (Carchi)',
        deliveries: 1240,
        avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=250'
      },
      etaMinutes: 10,
      distanceKm: 1.6,
      currency: '$ USD',
      subtotal: '11.00',
      shippingCost: '1.50',
      totalAmount: '12.50',
      paymentMethod: 'EFECTIVO',
      items: [
        { name: 'Combo Pollo Asado Familiar + Papas Nativas', quantity: 1, price: '9.50' },
        { name: 'Gaseosa Tradicional 1.5L', quantity: 1, price: '1.50' }
      ]
    };
  });

  // Coordenadas dinámicas del repartidor (actualizadas por WebSocket STOMP)
  const [driverLocation, setDriverLocation] = useState({
    lat: 0.8080,
    lon: -77.7205,
    heading: 45,
    speedKmH: 28.5,
    lastUpdate: new Date()
  });

  // Estado de conexión WebSocket
  const [wsConnected, setWsConnected] = useState(false);
  const [simulationActive, setSimulationActive] = useState(true);

  // Estados interactivos de UI
  const [showChatModal, setShowChatModal] = useState(false);
  const [chatMessages, setChatMessages] = useState([
    { sender: 'driver', text: '¡Hola Valeria! Ya retiré tu pedido en Asadero El Leñador, voy en camino hacia la Terminal.', time: '14:22' },
    { sender: 'client', text: 'Excelente Carlos, muchas gracias. Aquí te espero en la entrada principal.', time: '14:23' }
  ]);
  const [inputMessage, setInputMessage] = useState('');
  const [pinCopied, setPinCopied] = useState(false);
  const [isRegeneratingPin, setIsRegeneratingPin] = useState(false);
  const [pinChangeMessage, setPinChangeMessage] = useState(null);
  const [fabOpen, setFabOpen] = useState(false);

  // ── SINCRONIZACIÓN REACTIVA CON EL PEDIDO ACTIVO & BACKEND ───────────────
  useEffect(() => {
    const syncActiveOrder = async (orderData) => {
      const order = orderData || getActiveOrderData();
      if (!order) return;

      setOrderState((prev) => ({
        ...prev,
        id: order.id || prev.id,
        orderNumber: order.orderNumber || prev.orderNumber,
        status: order.status || prev.status,
        deliveryPin: order.deliveryPin || prev.deliveryPin,
        subtotal: order.subtotal || order.purchasePrice || prev.subtotal,
        shippingCost: order.shippingCost || order.shippingFee || prev.shippingCost,
        totalAmount: order.totalAmount || prev.totalAmount,
        paymentMethod: order.paymentMethod || prev.paymentMethod,
        items: order.items && order.items.length > 0 ? order.items : prev.items,
        merchant: {
          ...prev.merchant,
          name: order.restaurantName || prev.merchant.name,
          address: order.restaurantAddress || order.pickupAddress || prev.merchant.address
        },
        customer: {
          ...prev.customer,
          name: order.customerName || prev.customer.name,
          address: order.deliveryAddress || prev.customer.address
        }
      }));

      // Si el pedido tiene un UUID real, consultar el backend para obtener el estado y PIN más reciente
      const isUuid = (v) => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(v);
      if (order.id && isUuid(order.id)) {
        try {
          const token = getAuthToken();
          const res = await fetch(buildApiUrl(`/v1/orders/${order.id}`), {
            headers: {
              'Accept': 'application/json',
              'ngrok-skip-browser-warning': 'true',
              ...(token ? { Authorization: `Bearer ${token}` } : {})
            }
          });
          if (res.ok) {
            const data = await res.json();
            if (data && data.deliveryPin) {
              setOrderState((prev) => ({
                ...prev,
                deliveryPin: data.deliveryPin,
                status: data.status || prev.status,
                totalAmount: data.totalAmount ? Number(data.totalAmount).toFixed(2) : prev.totalAmount
              }));
            }
          }
        } catch (e) {
          console.warn('Backend order sync fallback:', e);
        }
      }
    };

    if (initialOrder) {
      syncActiveOrder(initialOrder);
    } else {
      syncActiveOrder();
    }

    const onOrderUpdated = (e) => {
      syncActiveOrder(e?.detail);
    };

    window.addEventListener('order-updated', onOrderUpdated);
    window.addEventListener('storage', onOrderUpdated);
    return () => {
      window.removeEventListener('order-updated', onOrderUpdated);
      window.removeEventListener('storage', onOrderUpdated);
    };
  }, [initialOrder, orderId]);

  // ── ÍNDICE ACTIVO EN EL STEPPER ─────────────────────────────────────────
  const activeStepIndex = useMemo(() => {
    switch (orderState.status) {
      case 'CREADO': return 0;
      case 'EN_PREPARACION': return 1;
      case 'EN_CAMINO': return 2;
      case 'ENTREGADO': return 3;
      default: return 2;
    }
  }, [orderState.status]);

  // ── CONEXIÓN WEBSOCKET STOMP /topic/orders/{orderId}/tracking ───────────
  useEffect(() => {
    let stompClient = null;
    const socketUrl = getWebSocketUrl('/ws-delivery');
    const topicPath = `/topic/orders/${orderId}/tracking`;

    // Conexión reactiva si @stomp/stompjs estuviese instalado en el bundler
    if (typeof window !== 'undefined' && window.Stomp) {
      try {
        const client = window.Stomp.client(socketUrl);
        client.connect({}, () => {
          setWsConnected(true);
          client.subscribe(topicPath, (message) => {
            if (message.body) {
              const data = JSON.parse(message.body);
              setDriverLocation({
                lat: data.latitude ?? data.lat,
                lon: data.longitude ?? data.lon,
                heading: data.heading ?? 0,
                speedKmH: data.speed ?? 25.0,
                lastUpdate: new Date()
              });
              if (data.status) {
                setOrderState((prev) => ({ ...prev, status: data.status }));
              }
            }
          });
        }, (err) => {
          console.warn('⚠️ WebSocket no disponible, activando simulación continua:', err);
          setWsConnected(false);
        });
        stompClient = client;
      } catch (e) {
        console.warn('Fallback a simulación local de movimiento:', e);
      }
    }

    return () => {
      if (stompClient && stompClient.disconnect) {
        stompClient.disconnect();
      }
    };
  }, [orderId]);

  // ── MOTOR DE MOVIMIENTO GEODÉSICO SIMULADO (Fallback interactivo) ─────────
  useEffect(() => {
    if (!simulationActive || orderState.status !== 'EN_CAMINO') return;

    let progress = 0.35;
    const interval = setInterval(() => {
      progress += 0.015;
      if (progress > 0.95) progress = 0.15; // Loop suave de prueba

      const mLat = orderState.merchant.lat;
      const mLon = orderState.merchant.lon;
      const cLat = orderState.customer.lat;
      const cLon = orderState.customer.lon;

      // Interpolación lineal entre comercio y cliente
      const newLat = mLat + (cLat - mLat) * progress;
      const newLon = mLon + (cLon - mLon) * progress;
      const remainingDistance = ((1 - progress) * 3.5).toFixed(1);
      const remainingEta = Math.max(1, Math.round((1 - progress) * 15));

      setDriverLocation({
        lat: newLat,
        lon: newLon,
        heading: 55,
        speedKmH: 26 + Math.round(Math.random() * 8),
        lastUpdate: new Date()
      });

      setOrderState((prev) => ({
        ...prev,
        distanceKm: parseFloat(remainingDistance),
        etaMinutes: remainingEta
      }));
    }, 2500);

    return () => clearInterval(interval);
  }, [simulationActive, orderState.status]);

  // ── COPIAR PIN AL PORTAPAPELES ───────────────────────────────────────────
  const copyPinToClipboard = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(orderState.deliveryPin);
      setPinCopied(true);
      setTimeout(() => setPinCopied(false), 2200);
    }
  };

  // ── REGENERAR / CAMBIAR CÓDIGO DE VERIFICACIÓN (ANTI-FRAUDE) ─────────────
  const handleRegeneratePin = async () => {
    setIsRegeneratingPin(true);
    setPinChangeMessage(null);
    let newPin = null;

    try {
      const token = getAuthToken();
      const isUuid = (v) => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(v);

      if (orderState.id && isUuid(orderState.id)) {
        try {
          const res = await fetch(buildApiUrl(`/v1/orders/${orderState.id}/regenerate-pin`), {
            method: 'POST',
            headers: {
              'Accept': 'application/json',
              'ngrok-skip-browser-warning': 'true',
              ...(token ? { Authorization: `Bearer ${token}` } : {})
            }
          });
          if (res.ok) {
            const data = await res.json();
            if (data && data.deliveryPin) {
              newPin = data.deliveryPin;
            }
          }
        } catch (err) {
          console.warn('Backend regenerate-pin unreachable, usando generador local seguro:', err);
        }
      }

      // Si no hay respuesta del backend o estamos en offline/demo, generar 4 dígitos seguros distintos
      if (!newPin) {
        let candidate = String(Math.floor(1000 + Math.random() * 9000));
        while (candidate === orderState.deliveryPin) {
          candidate = String(Math.floor(1000 + Math.random() * 9000));
        }
        newPin = candidate;
      }

      // Actualizar estado reactivo
      setOrderState((prev) => ({
        ...prev,
        deliveryPin: newPin
      }));

      // Guardar y notificar a la pestaña/módulo del repartidor
      try {
        const stored = localStorage.getItem('delivery_active_order');
        let parsed = stored ? JSON.parse(stored) : {};
        parsed.deliveryPin = newPin;
        localStorage.setItem('delivery_active_order', JSON.stringify(parsed));
        window.dispatchEvent(new CustomEvent('order-updated', { detail: parsed }));
      } catch (e) {
        console.warn('Error al sincronizar nuevo PIN en storage:', e);
      }

      setPinChangeMessage(`¡Código OTP actualizado a ${newPin}! Sincronizado con repartidor.`);
      setTimeout(() => setPinChangeMessage(null), 3500);
    } catch (err) {
      console.error('Error al regenerar PIN:', err);
    } finally {
      setIsRegeneratingPin(false);
    }
  };

  // ── ENVIAR MENSAJE AL CHAT ───────────────────────────────────────────────
  const handleSendMessage = (e) => {
    e.preventDefault();
    if (!inputMessage.trim()) return;

    const newMsg = {
      sender: 'client',
      text: inputMessage.trim(),
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setChatMessages((prev) => [...prev, newMsg]);
    setInputMessage('');

    // Respuesta simulada del repartidor
    setTimeout(() => {
      setChatMessages((prev) => [
        ...prev,
        {
          sender: 'driver',
          text: 'Entendido, estoy llegando al semáforo de tu calle.',
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ]);
    }, 1800);
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-white transition-colors duration-200 pb-28 font-sans antialiased">
      {/* ── BARRA SUPERIOR DE NAVEGACIÓN ─────────────────────────────────── */}
      <header className="sticky top-0 z-30 bg-white/90 dark:bg-slate-900/90 backdrop-blur-md border-b border-slate-200 dark:border-slate-700 px-4 py-3 shadow-sm transition-colors">
        <div className="max-w-4xl mx-auto flex items-center justify-between">
          <div className="flex items-center space-x-3">
            {onBackToMenu && (
              <button
                onClick={onBackToMenu}
                className="px-2.5 py-1.5 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-bold transition-all flex items-center gap-1 border border-slate-200 dark:border-slate-700 shadow-sm"
              >
                <span>← Menú</span>
              </button>
            )}
            <span className="inline-flex items-center justify-center p-2 rounded-xl bg-amber-500/10 text-amber-600">
              <Icons.Navigation />
            </span>
            <div>
              <h1 className="text-lg font-extrabold tracking-tight text-slate-900 dark:text-white">
                Seguimiento en Vivo
              </h1>
              <p className="text-xs text-slate-500 dark:text-slate-400 font-mono">
                Orden #{orderState.id ? orderState.id.slice(0, 8) : '4829'}
              </p>
            </div>
          </div>

          {/* Badge de estado en tiempo real */}
          <div className="flex items-center space-x-2">
            <span className="text-xs font-semibold text-slate-700 dark:text-slate-200 bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 px-2.5 py-1 rounded-full hidden md:inline-flex items-center gap-1.5">
              <span>🇪🇨</span> Tulcán, Carchi
            </span>
            <span className="relative flex h-3 w-3">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
            </span>
            <span className="text-xs font-semibold text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/30 px-2.5 py-1 rounded-full hidden sm:inline-block">
              {wsConnected ? 'WebSocket Activo' : 'GPS Simulado'}
            </span>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 pt-6 space-y-6">
        {/* ═══════════════════════════════════════════════════════════════════
            REQUISITO 1: ENCABEZADO DINÁMICO CON ESTADO DE LA ORDEN
            ═══════════════════════════════════════════════════════════════════ */}
        <section className="bg-white dark:bg-slate-900 rounded-3xl p-6 border border-slate-200 dark:border-slate-700 shadow-sm transition-colors">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6 pb-6 border-b border-slate-100 dark:border-slate-800">
            <div>
              <span className="text-xs uppercase tracking-wider font-bold text-amber-600 bg-amber-50 px-3 py-1 rounded-full">
                Estado Actual
              </span>
              <h2 className="text-2xl font-black text-slate-900 dark:text-white mt-2">
                {ORDER_STEPS[activeStepIndex]?.label}
              </h2>
              <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">
                {ORDER_STEPS[activeStepIndex]?.sublabel}
              </p>
            </div>

            {/* ETA Estimada */}
            <div className="flex items-center space-x-3 bg-slate-50 dark:bg-slate-800 border border-slate-200/80 dark:border-slate-700 px-4 py-3 rounded-2xl transition-colors">
              <Icons.Clock />
              <div>
                <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Llegada estimada</p>
                <p className="text-xl font-black text-slate-900 dark:text-white">
                  {orderState.etaMinutes} min <span className="text-xs font-medium text-slate-400">({orderState.distanceKm} km)</span>
                </p>
              </div>
            </div>
          </div>

          {/* Stepper dinámico horizontal */}
          <div className="relative">
            <div className="overflow-hidden h-2 mb-6 flex rounded-full bg-slate-100 relative">
              <div
                style={{ width: `${(activeStepIndex / (ORDER_STEPS.length - 1)) * 100}%` }}
                className="shadow-none flex flex-col text-center whitespace-nowrap text-white justify-center bg-gradient-to-r from-amber-500 to-emerald-500 transition-all duration-700 ease-out"
              />
            </div>

            <div className="grid grid-cols-4 gap-2">
              {ORDER_STEPS.map((step, idx) => {
                const isCompleted = idx < activeStepIndex;
                const isCurrent = idx === activeStepIndex;
                return (
                  <div key={step.key} className="flex flex-col items-center text-center">
                    <div
                      className={`w-9 h-9 rounded-2xl flex items-center justify-center font-bold text-xs transition-all duration-300 shadow-sm ${
                        isCompleted
                          ? 'bg-emerald-500 text-white shadow-emerald-200'
                          : isCurrent
                          ? 'bg-amber-500 text-white ring-4 ring-amber-100 shadow-amber-200 animate-pulse'
                          : 'bg-slate-100 text-slate-400 border border-slate-200'
                      }`}
                    >
                      {isCompleted ? <Icons.Check /> : idx + 1}
                    </div>
                    <span
                      className={`text-xs font-bold mt-2 truncate w-full ${
                        isCurrent
                          ? 'text-slate-900'
                          : isCompleted
                          ? 'text-emerald-700'
                          : 'text-slate-400'
                      }`}
                    >
                      {step.label}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        </section>

        {/* ═══════════════════════════════════════════════════════════════════
            REQUISITO 2: TARJETA DESTACADA DE SEGURIDAD ANTI-FRAUDE
            "PIN de Entrega con números gigantes en caja bg-yellow-100 border-2 border-yellow-400..."
            ═══════════════════════════════════════════════════════════════════ */}
        <section className="bg-white dark:bg-slate-900 rounded-3xl p-6 border border-slate-200 dark:border-slate-700 shadow-sm transition-colors">
          <div className="flex items-center space-x-3 mb-4">
            <span className="p-2 rounded-xl bg-yellow-50 border border-yellow-200">
              <Icons.ShieldCheck />
            </span>
            <div>
              <h3 className="text-lg font-black text-slate-900 dark:text-white">
                Seguridad Anti-Fraude (BMCP RULE-002)
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Verificación requerida para completar la entrega
              </p>
            </div>
          </div>

          {/* CAJA OBLIGATORIA CON LAS CLASES EXACTAS DE TAILWIND */}
          <div className="bg-yellow-100 border-2 border-yellow-400 p-6 rounded-2xl text-center text-4xl font-bold tracking-widest text-gray-900 shadow-inner relative group transition-all duration-200">
            <div className="flex items-center justify-center space-x-4">
              <span>{orderState.deliveryPin}</span>
            </div>

            <div className="mt-3 flex flex-wrap items-center justify-center gap-2">
              <button
                onClick={copyPinToClipboard}
                type="button"
                className="inline-flex items-center space-x-1.5 text-xs font-bold text-yellow-900 bg-yellow-200/80 hover:bg-yellow-300 px-3 py-1.5 rounded-lg transition-colors cursor-pointer"
              >
                <Icons.Lock />
                <span>{pinCopied ? '¡PIN Copiado!' : 'Copiar PIN'}</span>
              </button>

              <button
                onClick={handleRegeneratePin}
                disabled={isRegeneratingPin}
                type="button"
                className="inline-flex items-center space-x-1.5 text-xs font-bold text-amber-950 bg-amber-300 hover:bg-amber-400 border border-amber-500 px-3 py-1.5 rounded-lg transition-all shadow-sm cursor-pointer disabled:opacity-50"
                title="Genera un nuevo código OTP anti-fraude para este pedido"
              >
                <span>{isRegeneratingPin ? '⏳ Cambiando...' : '🔄 Cambiar Código de Verificación'}</span>
              </button>
            </div>

            {pinChangeMessage && (
              <div className="mt-2 text-xs font-bold text-emerald-800 bg-emerald-100/90 border border-emerald-300 rounded-lg py-1 px-3 inline-block animate-fadeIn">
                {pinChangeMessage}
              </div>
            )}
          </div>

          <div className="mt-4 p-3.5 bg-amber-50/70 border border-amber-200 rounded-xl flex items-start space-x-3">
            <span className="text-amber-700 text-sm mt-0.5">⚠️</span>
            <p className="text-xs text-amber-900 leading-relaxed font-medium">
              <strong>Importante:</strong> Dicta este código de 4 dígitos <span className="underline">únicamente</span> al repartidor cuando tengas tu pedido físicamente en mano. Nunca lo envíes por mensaje ni por llamada.
            </p>
          </div>
        </section>

        {/* ═══════════════════════════════════════════════════════════════════
            REQUISITO 3: COMPONENTE DE MAPA SIMULADO/REAL
            "Movimiento del repartidor en tiempo real consumiendo coordenadas WebSocket"
            ═══════════════════════════════════════════════════════════════════ */}
        <section className="bg-white dark:bg-slate-900 rounded-3xl overflow-hidden border border-slate-200 dark:border-slate-700 shadow-sm transition-colors">
          <div className="p-4 bg-slate-800 dark:bg-slate-900 text-white flex items-center justify-between transition-colors">
            <div className="flex items-center space-x-2">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping"></span>
              <span className="text-xs font-bold uppercase tracking-wider text-slate-300">
                Tracking PostGIS en Vivo
              </span>
            </div>
            <div className="text-right text-xs text-slate-400 font-mono">
              STOMP: /topic/orders/{orderState.id.slice(0, 8)}.../tracking
            </div>
          </div>

          {/* Lienzo del Mapa (Renderizador SVG interactivo de alta fidelidad) */}
          <div className="relative w-full h-80 sm:h-96 bg-slate-100 overflow-hidden select-none">
            {/* Cuadrícula de calles y avenidas simuladas */}
            <svg className="absolute inset-0 w-full h-full" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <pattern id="grid-pattern" width="60" height="60" patternUnits="userSpaceOnUse">
                  <path d="M 60 0 L 0 0 0 60" fill="none" stroke="#e2e8f0" strokeWidth="1" />
                </pattern>
              </defs>
              <rect width="100%" height="100%" fill="url(#grid-pattern)" />

              {/* Calles principales */}
              <line x1="0" y1="90" x2="100%" y2="90" stroke="#cbd5e1" strokeWidth="10" />
              <line x1="0" y1="230" x2="100%" y2="230" stroke="#cbd5e1" strokeWidth="8" />
              <line x1="120" y1="0" x2="120" y2="100%" stroke="#cbd5e1" strokeWidth="10" />
              <line x1="320" y1="0" x2="320" y2="100%" stroke="#cbd5e1" strokeWidth="8" />

              {/* Ruta trazada entre Comercio y Cliente */}
              <path
                d="M 80 90 L 220 90 L 220 230 L 360 230"
                fill="none"
                stroke="#6366f1"
                strokeWidth="5"
                strokeDasharray="8 6"
                className="animate-pulse"
              />
            </svg>

            {/* Marcador 1: Comercio / Restaurante */}
            <div className="absolute top-[68px] left-[60px] -translate-x-1/2 -translate-y-1/2 flex flex-col items-center">
              <div className="w-10 h-10 rounded-2xl bg-white dark:bg-slate-200 border-2 border-emerald-500 shadow-lg flex items-center justify-center transform hover:scale-110 transition-transform">
                <Icons.Store />
              </div>
              <span className="mt-1 bg-white/95 dark:bg-slate-800/95 text-slate-800 dark:text-slate-100 text-[10px] font-bold px-2 py-0.5 rounded-full shadow border border-slate-200 dark:border-slate-700 whitespace-nowrap">
                {orderState.merchant.name}
              </span>
            </div>

            {/* Marcador 2: Destino / Cliente */}
            <div className="absolute top-[208px] left-[340px] -translate-x-1/2 -translate-y-1/2 flex flex-col items-center">
              <div className="w-10 h-10 rounded-2xl bg-white dark:bg-slate-200 border-2 border-indigo-600 shadow-lg flex items-center justify-center transform hover:scale-110 transition-transform">
                <Icons.Home />
              </div>
              <span className="mt-1 bg-white/95 dark:bg-slate-800/95 text-slate-800 dark:text-slate-100 text-[10px] font-bold px-2 py-0.5 rounded-full shadow border border-slate-200 dark:border-slate-700 whitespace-nowrap">
                Mi Ubicación
              </span>
            </div>

            {/* Marcador Dinámico: REPARTIDOR EN TIEMPO REAL */}
            {/* Se posiciona según la interpolación de coordenadas */}
            <div
              className="absolute transition-all duration-1000 ease-linear flex flex-col items-center"
              style={{
                top: `${90 + (driverLocation.lat - orderState.merchant.lat) / (orderState.customer.lat - orderState.merchant.lat) * 140}px`,
                left: `${80 + (driverLocation.lon - orderState.merchant.lon) / (orderState.customer.lon - orderState.merchant.lon) * 280}px`,
                transform: 'translate(-50%, -50%)'
              }}
            >
              {/* Onda expansiva de geolocalización activa */}
              <span className="absolute w-14 h-14 rounded-full bg-amber-400 opacity-40 animate-ping"></span>

              <div className="relative w-12 h-12 rounded-2xl bg-slate-900 dark:bg-slate-950 border-2 border-amber-400 shadow-2xl flex items-center justify-center">
                <Icons.Motorcycle />
              </div>

              {/* Tag flotante con telemetría en vivo */}
              <div className="mt-1.5 bg-slate-900/90 dark:bg-slate-950/90 backdrop-blur text-white text-[10px] font-bold px-2.5 py-1 rounded-full shadow-lg border border-slate-700 flex items-center space-x-1 whitespace-nowrap">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                <span>{orderState.driver.name.split(' ')[0]} • {driverLocation.speedKmH} km/h</span>
              </div>
            </div>

            {/* Widget flotante de telemetría GPS */}
            <div className="absolute bottom-4 left-4 right-4 sm:right-auto bg-white/95 dark:bg-slate-900/95 backdrop-blur-md border border-slate-200 dark:border-slate-700 p-3.5 rounded-2xl shadow-xl flex items-center space-x-4 transition-colors">
              <img
                src={orderState.driver.avatar}
                alt={orderState.driver.name}
                className="w-12 h-12 rounded-xl object-cover border border-slate-200 shadow-sm"
              />
              <div className="text-left">
                <div className="flex items-center space-x-1.5">
                  <span className="font-bold text-sm text-slate-900 dark:text-white">{orderState.driver.name}</span>
                  <span className="text-[10px] font-extrabold uppercase px-1.5 py-0.5 rounded bg-amber-100 text-amber-800">
                    {orderState.driver.level}
                  </span>
                </div>
                <p className="text-xs text-slate-500 dark:text-slate-400">{orderState.driver.vehicle}</p>
                <div className="flex items-center space-x-1 mt-0.5 text-xs text-amber-500 font-bold">
                  <Icons.Star />
                  <span>{orderState.driver.rating}</span>
                  <span className="text-slate-400 font-normal">({orderState.driver.deliveries} entregas)</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Resumen del Pedido y Direcciones */}
        <section className="bg-white dark:bg-slate-900 rounded-3xl p-6 border border-slate-200 dark:border-slate-700 shadow-sm transition-colors">
          <h4 className="text-sm font-extrabold uppercase tracking-wider text-slate-400 mb-4">
            Detalles de Entrega
          </h4>
          <div className="space-y-4">
            <div className="flex items-start space-x-3">
              <span className="p-1.5 rounded-lg bg-emerald-50 text-emerald-600 mt-0.5">
                <Icons.Store />
              </span>
              <div>
                <p className="text-xs font-bold text-slate-500">Recogida (Comercio)</p>
                <p className="text-sm font-semibold text-slate-900 dark:text-white">{orderState.merchant.name}</p>
                <p className="text-xs text-slate-500 dark:text-slate-400">{orderState.merchant.address}</p>
              </div>
            </div>

            <div className="flex items-start space-x-3">
              <span className="p-1.5 rounded-lg bg-indigo-50 text-indigo-600 mt-0.5">
                <Icons.Home />
              </span>
              <div>
                <p className="text-xs font-bold text-slate-500">Destino (Tu dirección)</p>
                <p className="text-sm font-semibold text-slate-900 dark:text-white">{orderState.customer.name}</p>
                <p className="text-xs text-slate-500 dark:text-slate-400">{orderState.customer.address}</p>
              </div>
            </div>
          </div>
        </section>

        {/* ═══════════════════════════════════════════════════════════════════
            REQUISITO 1 & 2: RESUMEN DE COSTO ($ USD) — TULCÁN, CARCHI
            ═══════════════════════════════════════════════════════════════════ */}
        <section className="bg-white dark:bg-slate-900 rounded-3xl p-6 border border-slate-200 dark:border-slate-700 shadow-sm transition-colors">
          <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-100 dark:border-slate-800">
            <div>
              <h4 className="text-sm font-extrabold uppercase tracking-wider text-slate-800 dark:text-white flex items-center gap-2">
                <span>Resumen de Costo ($ USD)</span>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
                  Dólar Estadounidense
                </span>
              </h4>
              <p className="text-xs text-slate-400 mt-0.5">
                Facturación oficial • Cobertura Urbana Tulcán, Carchi (Ecuador)
              </p>
            </div>
            <span className="text-xs font-mono font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-lg">
              $ USD
            </span>
          </div>

          <div className="space-y-3 text-sm">
            {orderState.items.map((item, idx) => (
              <div key={idx} className="flex items-center justify-between text-slate-700 dark:text-slate-300">
                <span className="flex items-center gap-2">
                  <span className="w-5 h-5 rounded-md bg-slate-100 text-slate-600 flex items-center justify-center text-xs font-bold font-mono">
                    {item.quantity}x
                  </span>
                  <span>{item.name}</span>
                </span>
                <span className="font-mono font-medium">${item.price} USD</span>
              </div>
            ))}

            <div className="pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-slate-600 dark:text-slate-400 text-xs">
              <span>Subtotal Productos</span>
              <span className="font-mono font-semibold">${orderState.subtotal} USD</span>
            </div>

            <div className="flex items-center justify-between text-slate-600 dark:text-slate-400 text-xs">
              <span className="flex items-center gap-1.5">
                <span>Tarifa de Envío Urbana (Tulcán &lt; 3.0 km)</span>
                <span className="text-[10px] px-1.5 py-0.2 rounded bg-amber-50 text-amber-700 border border-amber-200 font-medium">PostGIS</span>
              </span>
              <span className="font-mono font-semibold">${orderState.shippingCost} USD</span>
            </div>

            <div className="flex items-center justify-between text-slate-600 dark:text-slate-400 text-xs">
              <span>Tarifa de Servicio / Plataforma</span>
              <span className="font-mono font-semibold text-emerald-600">$0.00 USD (Exento)</span>
            </div>

            <div className="pt-3 border-t-2 border-slate-200 dark:border-slate-700 flex items-center justify-between text-base">
              <div>
                <span className="font-black text-slate-900 dark:text-white block">Total a Pagar</span>
                <span className="text-[11px] text-slate-400 font-medium">Pago en Efectivo / DeUna / Banco Pichincha</span>
              </div>
              <div className="text-right">
                <span className="text-2xl font-black text-emerald-600 font-mono">
                  ${orderState.totalAmount} USD
                </span>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* ═══════════════════════════════════════════════════════════════════
          REQUISITO 4: BOTÓN FLOTANTE (FAB) PARA LLAMAR O CHATEAR CON REPARTIDOR
          ═══════════════════════════════════════════════════════════════════ */}
      <div className="fixed bottom-6 right-6 z-40 flex flex-col items-end space-y-3">
        {/* Sub-menú flotante desplegable */}
        {fabOpen && (
          <div className="flex flex-col items-end space-y-2 mb-1 animate-fade-in">
            {/* Botón: Llamar al Repartidor */}
            <a
              href={`tel:${orderState.driver.phone}`}
              className="flex items-center space-x-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm px-4 py-2.5 rounded-full shadow-lg hover:shadow-xl transition-all transform hover:-translate-y-0.5"
            >
              <Icons.Phone />
              <span>Llamar ({orderState.driver.phone})</span>
            </a>

            {/* Botón: Abrir Chat en Vivo */}
            <button
              onClick={() => {
                setShowChatModal(true);
                setFabOpen(false);
              }}
              type="button"
              className="flex items-center space-x-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm px-4 py-2.5 rounded-full shadow-lg hover:shadow-xl transition-all transform hover:-translate-y-0.5"
            >
              <Icons.Chat />
              <span>Chatear con {orderState.driver.name.split(' ')[0]}</span>
            </button>
          </div>
        )}

        {/* Botón Principal Disparador (FAB) */}
        <button
          onClick={() => setFabOpen(!fabOpen)}
          type="button"
          aria-label="Contactar repartidor"
          className="w-14 h-14 rounded-full bg-slate-800 dark:bg-slate-900 hover:bg-slate-700 dark:hover:bg-slate-800 text-white flex items-center justify-center shadow-2xl ring-4 ring-slate-200 dark:ring-slate-700 transition-colors transform hover:scale-105 active:scale-95"
        >
          {fabOpen ? (
            <Icons.Close />
          ) : (
            <div className="relative">
              <Icons.Chat />
              <span className="absolute -top-1 -right-1 w-3 h-3 bg-emerald-500 border-2 border-slate-900 rounded-full"></span>
            </div>
          )}
        </button>
      </div>

      {/* ── MODAL DE CHAT EN TIEMPO REAL ──────────────────────────────────── */}
      {showChatModal && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/40 dark:bg-slate-900/60 backdrop-blur-sm p-0 sm:p-4 animate-fade-in">
          <div className="w-full sm:max-w-md bg-white dark:bg-slate-900 rounded-t-3xl sm:rounded-3xl shadow-2xl flex flex-col h-[520px] overflow-hidden border border-slate-200 dark:border-slate-700 transition-colors">
            {/* Header del Chat */}
            <div className="p-4 bg-slate-800 dark:bg-slate-950 text-white flex items-center justify-between transition-colors">
              <div className="flex items-center space-x-3">
                <img
                  src={orderState.driver.avatar}
                  alt={orderState.driver.name}
                  className="w-10 h-10 rounded-xl object-cover border border-slate-700"
                />
                <div>
                  <h4 className="font-bold text-sm">{orderState.driver.name}</h4>
                  <p className="text-[11px] text-emerald-400 font-medium flex items-center space-x-1">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                    <span>En ruta hacia tu domicilio</span>
                  </p>
                </div>
              </div>
              <button
                onClick={() => setShowChatModal(false)}
                className="text-slate-400 hover:text-white p-1 rounded-lg"
              >
                <Icons.Close />
              </button>
            </div>

            {/* Historial de Mensajes */}
            <div className="flex-1 p-4 overflow-y-auto space-y-3 bg-slate-50 dark:bg-slate-800">
              {chatMessages.map((msg, i) => {
                const isClient = msg.sender === 'client';
                return (
                  <div
                    key={i}
                    className={`flex flex-col ${isClient ? 'items-end' : 'items-start'}`}
                  >
                    <div
                      className={`max-w-[75%] px-4 py-2.5 rounded-2xl text-xs sm:text-sm font-medium leading-relaxed ${
                        isClient
                          ? 'bg-slate-700 dark:bg-slate-900 text-white rounded-br-xs'
                          : 'bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 border border-slate-200 dark:border-slate-700 rounded-bl-xs shadow-sm'
                      }`}
                    >
                      {msg.text}
                    </div>
                    <span className="text-[10px] text-slate-400 mt-1 px-1">{msg.time}</span>
                  </div>
                );
              })}
            </div>

            {/* Input para redactar mensaje */}
            <form onSubmit={handleSendMessage} className="p-3 bg-white dark:bg-slate-900 border-t border-slate-200 dark:border-slate-700 flex items-center space-x-2 transition-colors">
              <input
                type="text"
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                placeholder="Escribe un mensaje al repartidor..."
                className="flex-1 text-sm bg-slate-100 border-none rounded-xl px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-slate-900"
              />
              <button
                type="submit"
                className="p-2.5 bg-slate-700 dark:bg-slate-900 hover:bg-slate-600 dark:hover:bg-slate-800 text-white rounded-xl transition-colors"
              >
                <Icons.Send />
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
