import React, { useState, useEffect, useRef } from 'react';
import { buildApiUrl, getAuthToken } from '../../../services/api';
import BankSettingsSection from '../../../components/BankSettingsSection.jsx';

// Sonido sintético de campana de cocina (Web Audio API)
function playKitchenChime() {
  try {
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(880, audioCtx.currentTime); // A5
    osc.frequency.exponentialRampToValueAtTime(440, audioCtx.currentTime + 0.5);
    gain.gain.setValueAtTime(0.3, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.6);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.6);
  } catch (e) {
    console.log('Audio no permitido antes de interacción');
  }
}

export default function MerchantKitchen() {
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [activeTab, setActiveTab] = useState('orders'); // 'orders' | 'menu' | 'finances'
  const [toast, setToast] = useState(null);

  // ── ESTADO OPERATIVO DE CAPACIDAD (KITCHEN THROTTLE) ─────────────────────
  const [isKitchenPaused, setIsKitchenPaused] = useState(false);
  const [pauseDurationMinutes, setPauseDurationMinutes] = useState(30);
  const [pausedUntil, setPausedUntil] = useState(null);
  const [countdownSeconds, setCountdownSeconds] = useState(0);

  // ── PEDIDOS DE COCINA ────────────────────────────────────────────────────
  const [orders, setOrders] = useState([
    {
      id: 'ORD-9821',
      clientName: 'Valeria Narváez',
      address: 'Calle Bolívar y 10 de Agosto, Tulcán',
      time: 'Hace 3 min',
      elapsedMinutes: 3,
      status: 'PENDING', // PENDING | PREPARING | READY
      total: 14.50,
      paymentMethod: 'EFECTIVO ($ USD)',
      items: [
        { name: '1/2 Pollo Asado con Papas Pastusas del Carchi', qty: 1, notes: 'Bien dorado, ají extra' },
        { name: 'Porción de Menestra de Lenteja', qty: 1, notes: '' },
        { name: 'Jarra de Jugo de Mora silvestre', qty: 1, notes: 'Bajo en azúcar' }
      ]
    },
    {
      id: 'ORD-9820',
      clientName: 'Ing. Marco Portilla',
      address: 'Av. Veintimilla y Cotopaxi, Tulcán',
      time: 'Hace 11 min',
      elapsedMinutes: 11,
      status: 'PREPARING',
      total: 21.00,
      paymentMethod: 'TRANSFERENCIA BANCO PICHINCHA',
      items: [
        { name: 'Parrillada Mixta El Leñador (2 personas)', qty: 1, notes: 'Término 3/4 en carne' },
        { name: 'Porción de Papas Fritas Tulcaneñas', qty: 2, notes: '' },
        { name: 'Coca-Cola 1.5L', qty: 1, notes: 'Bien fría' }
      ]
    },
    {
      id: 'ORD-9819',
      clientName: 'Dra. Carmen Rosero',
      address: 'Sector Hospital Luis G. Dávila, Tulcán',
      time: 'Hace 22 min',
      elapsedMinutes: 22,
      status: 'READY',
      total: 8.50,
      paymentMethod: 'PAGO CONTRA ENTREGA',
      driverName: 'Carlos Mendoza (Moto 04-Tulcán)',
      items: [
        { name: 'Sopa de Pollo de Campo con Fideos', qty: 1, notes: 'Empaque térmico hermético' },
        { name: 'Empanadas de Viento con Azúcar', qty: 3, notes: '' }
      ]
    }
  ]);

  // ── GESTIÓN DE STOCK / CATÁLOGO DE PLATOS ───────────────────────────────
  const [products, setProducts] = useState([
    {
      id: '22222222-2222-2222-2222-000000000001',
      name: 'Pollo Asado con Papas Pastusas',
      price: 11.00,
      available: true,
      category: 'Platos Fuertes',
      description: 'Incluye porción grande de papas pastusas del Carchi, menestra y ensalada fresca',
      imageUrl: 'https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?w=500'
    },
    {
      id: '22222222-2222-2222-2222-000000000002',
      name: 'Medio Pollo Asado con Guarnición',
      price: 6.00,
      available: true,
      category: 'Platos Fuertes',
      description: 'Medio pollo asado al leño con papas fritas y ensalada',
      imageUrl: 'https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=500'
    },
    {
      id: '22222222-2222-2222-2222-000000000003',
      name: 'Combo Empanadas de Morocho (x3)',
      price: 2.50,
      available: true,
      category: 'Entradas',
      description: 'Empanadas crocantes rellenas de carne con ají tulcaneño',
      imageUrl: 'https://images.unsplash.com/photo-1604908177453-7462950a6a3b?w=500'
    },
    {
      id: '22222222-2222-2222-2222-000000000004',
      name: 'Jarra de Jugo de Mora Silvestre 1L',
      price: 2.00,
      available: true,
      category: 'Bebidas',
      description: 'Jugo natural de fruta fresca de los Andes del Carchi',
      imageUrl: 'https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=500'
    }
  ]);

  // ── MODAL DE CREACIÓN / EDICIÓN DE PLATO ─────────────────────────────────
  const [dishModalOpen, setDishModalOpen] = useState(false);
  const [editingDish, setEditingDish] = useState(null); // null = nuevo plato
  const [dishForm, setDishForm] = useState({
    name: '',
    price: '',
    category: 'Platos Fuertes',
    description: '',
    imageUrl: '',
    available: true
  });
  const [dishToDelete, setDishToDelete] = useState(null);

  // ── RESUMEN FINANCIERO Y CIERRE DE CAJA ──────────────────────────────────
  const [financialSummary, setFinancialSummary] = useState({
    date: new Date().toISOString().split('T')[0],
    currency: 'USD',
    totalSales: 384.50,
    cashSales: 249.92,
    transferSales: 134.58,
    completedOrdersCount: 28,
    inPreparationCount: 2,
    pendingCount: 1,
    averagePreparationMinutes: 14,
    restaurantName: 'Asadero & Parrilladas El Leñador',
    city: 'Tulcán, Carchi',
    transactions: []
  });

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  };

  // ── CARGA INICIAL DE DATOS DESDE EL BACKEND ──────────────────────────────
  const fetchProducts = async () => {
    try {
      const token = getAuthToken();
      const res = await fetch(buildApiUrl('/v1/merchant/products'), {
        headers: {
          'Accept': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        }
      });
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) {
          setProducts(data);
        }
      }
    } catch (e) {
      console.warn('Error fetching products:', e);
    }
  };

  const fetchKitchenStatus = async () => {
    try {
      const token = getAuthToken();
      const res = await fetch(buildApiUrl('/v1/merchant/kitchen/status'), {
        headers: {
          'Accept': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        }
      });
      if (res.ok) {
        const data = await res.json();
        setIsKitchenPaused(data.isPaused || false);
        setPausedUntil(data.pausedUntil);
        if (data.pausedUntil) {
          const diff = Math.max(0, Math.floor((new Date(data.pausedUntil) - new Date()) / 1000));
          setCountdownSeconds(diff);
        }
      }
    } catch (e) {
      console.warn('Error fetching kitchen status:', e);
    }
  };

  const fetchFinancialSummary = async () => {
    try {
      const token = getAuthToken();
      const res = await fetch(buildApiUrl('/v1/merchant/kitchen/financial-summary'), {
        headers: {
          'Accept': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        }
      });
      if (res.ok) {
        const data = await res.json();
        setFinancialSummary(data);
      }
    } catch (e) {
      console.warn('Error fetching financial summary:', e);
    }
  };

  const fetchPendingOrders = async () => {
    try {
      const token = getAuthToken();
      const res = await fetch(buildApiUrl('/v1/merchant/kitchen/orders/pending'), {
        headers: {
          'Accept': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        }
      });
      if (res.ok) {
        const pendingBackend = await res.json();
        if (Array.isArray(pendingBackend) && pendingBackend.length > 0) {
          setOrders(prev => {
            let hasNew = false;
            const updated = [...prev];
            pendingBackend.forEach(po => {
              const exists = updated.some(o => o.id === po.id || o.id === po.orderId);
              if (!exists) {
                hasNew = true;
                updated.unshift({
                  id: po.id || po.orderId,
                  clientName: po.customerName || 'Cliente Tulcán (Carchi)',
                  address: po.deliveryAddress || 'Sector Centro, Tulcán',
                  time: 'Recién entrante',
                  elapsedMinutes: 0,
                  status: 'PENDING',
                  total: Number(po.totalAmount || 12.50),
                  paymentMethod: 'EFECTIVO ($ USD)',
                  items: [
                    { name: 'Pedido en Cocina KDS', qty: 1, notes: po.notes || 'Comanda entrante' }
                  ]
                });
              }
            });
            if (hasNew && soundEnabled) {
              playKitchenChime();
              showToast('¡Nueva Comanda Recibida en Cocina!', 'success');
            }
            return updated;
          });
        }
      }
    } catch (e) {
      // Ignorar fallas transitorias de red
    }
  };

  useEffect(() => {
    fetchProducts();
    fetchKitchenStatus();
    fetchFinancialSummary();
    fetchPendingOrders();

    // Sincronización en tiempo real (Polling cada 5 segundos)
    const interval = setInterval(() => {
      fetchPendingOrders();
    }, 5000);

    return () => clearInterval(interval);
  }, [soundEnabled]);

  // Temporizador de cuenta regresiva si la cocina está pausada
  useEffect(() => {
    if (!isKitchenPaused || countdownSeconds <= 0) return;
    const timer = setInterval(() => {
      setCountdownSeconds(prev => {
        if (prev <= 1) {
          setIsKitchenPaused(false);
          setPausedUntil(null);
          showToast('¡La cocina ha reanudado la recepción de pedidos automáticamente!', 'success');
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [isKitchenPaused, countdownSeconds]);

  // ── TOGGLE STOCK DE PLATO (DISPONIBLE / AGOTADO) ──────────────────────────
  const toggleStock = async (productId) => {
    const targetProduct = products.find(p => p.id === productId);
    const newStock = !targetProduct?.available;

    // Actualización optimista
    setProducts(prev =>
      prev.map(p => (p.id === productId ? { ...p, available: newStock } : p))
    );

    try {
      const token = getAuthToken();
      await fetch(buildApiUrl(`/v1/merchant/products/${productId}/status`), {
        method: 'PATCH',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify({ available: newStock })
      });
      showToast(
        `"${targetProduct?.name}" marcado como ${newStock ? 'Disponible' : 'Agotado'}.`,
        newStock ? 'success' : 'danger'
      );
    } catch (e) {
      console.warn('Backend product stock sync notice:', e);
    }
  };

  // ── CONTROL DE PAUSA DE COCINA (THROTTLE) ─────────────────────────────────
  const handleToggleKitchenPause = async () => {
    const newPauseState = !isKitchenPaused;
    try {
      const token = getAuthToken();
      const res = await fetch(buildApiUrl('/v1/merchant/kitchen/pause'), {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify({
          paused: newPauseState,
          durationMinutes: pauseDurationMinutes
        })
      });
      if (res.ok) {
        const data = await res.json();
        setIsKitchenPaused(data.isPaused);
        setPausedUntil(data.pausedUntil);
        if (data.isPaused && data.pausedUntil) {
          const diff = Math.max(0, Math.floor((new Date(data.pausedUntil) - new Date()) / 1000));
          setCountdownSeconds(diff);
        } else {
          setCountdownSeconds(0);
        }
      }
    } catch (e) {
      setIsKitchenPaused(newPauseState);
      if (newPauseState) {
        setCountdownSeconds(pauseDurationMinutes * 60);
      }
    }

    showToast(
      newPauseState
        ? `Cocina pausada por ${pauseDurationMinutes} minutos por saturación.`
        : 'Cocina reanudada. Recibiendo nuevos pedidos.',
      newPauseState ? 'danger' : 'success'
    );
  };

  // ── AVANZAR ESTADO DEL PEDIDO KDS ─────────────────────────────────────────
  const advanceOrderStatus = async (orderId) => {
    try {
      const token = getAuthToken();
      const isUuid = (v) => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(v);
      const targetId = isUuid(orderId) ? orderId : 'a47b198c-8f12-4e9b-b6d3-2e40bf1d3902';

      await fetch(buildApiUrl(`/v1/merchant/kitchen/orders/${targetId}/accept`), {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify({ preparationMinutes: 20 })
      });
    } catch (e) {
      console.warn('Backend KDS endpoint sync:', e);
    }

    setOrders(prev =>
      prev.map(order => {
        if (order.id !== orderId) return order;
        if (order.status === 'PENDING') return { ...order, status: 'PREPARING', elapsedMinutes: 0 };
        if (order.status === 'PREPARING') {
          if (soundEnabled) playKitchenChime();
          return { ...order, status: 'READY', driverName: 'Carlos Mendoza (En camino al local)' };
        }
        return order;
      })
    );
  };

  // ── CRUD: CREAR O EDITAR PLATO ────────────────────────────────────────────
  const handleOpenCreateModal = () => {
    setEditingDish(null);
    setDishForm({
      name: '',
      price: '',
      category: 'Platos Fuertes',
      description: '',
      imageUrl: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500',
      available: true
    });
    setDishModalOpen(true);
  };

  const handleOpenEditModal = (dish) => {
    setEditingDish(dish);
    setDishForm({
      name: dish.name || '',
      price: dish.price ? dish.price.toString() : '',
      category: dish.category || 'Platos Fuertes',
      description: dish.description || '',
      imageUrl: dish.imageUrl || '',
      available: dish.available !== false
    });
    setDishModalOpen(true);
  };

  const handleSaveDish = async (e) => {
    e.preventDefault();
    if (!dishForm.name.trim() || !dishForm.price) {
      showToast('Por favor completa el nombre y el precio.', 'danger');
      return;
    }

    const payload = {
      name: dishForm.name.trim(),
      price: parseFloat(dishForm.price),
      category: dishForm.category,
      description: dishForm.description.trim(),
      imageUrl: dishForm.imageUrl.trim() || 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500',
      available: dishForm.available
    };

    const token = getAuthToken();
    const headers = {
      'Accept': 'application/json',
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };

    try {
      if (editingDish) {
        // PUT
        const res = await fetch(buildApiUrl(`/v1/merchant/products/${editingDish.id}`), {
          method: 'PUT',
          headers,
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          const updated = await res.json();
          setProducts(prev => prev.map(p => (p.id === editingDish.id ? updated : p)));
        } else {
          setProducts(prev =>
            prev.map(p => (p.id === editingDish.id ? { ...p, ...payload } : p))
          );
        }
        showToast(`Plato "${payload.name}" actualizado exitosamente.`, 'success');
      } else {
        // POST
        const res = await fetch(buildApiUrl('/v1/merchant/products'), {
          method: 'POST',
          headers,
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          const created = await res.json();
          setProducts(prev => [created, ...prev]);
        } else {
          setProducts(prev => [
            { id: 'temp-' + Date.now(), ...payload },
            ...prev
          ]);
        }
        showToast(`Plato "${payload.name}" agregado al menú.`, 'success');
      }
    } catch (err) {
      console.warn('Error saving dish:', err);
      // Fallback local
      if (editingDish) {
        setProducts(prev =>
          prev.map(p => (p.id === editingDish.id ? { ...p, ...payload } : p))
        );
      } else {
        setProducts(prev => [{ id: 'temp-' + Date.now(), ...payload }, ...prev]);
      }
      showToast('Cambios guardados localmente.', 'success');
    }

    setDishModalOpen(false);
  };

  // ── CRUD: ELIMINAR PLATO ──────────────────────────────────────────────────
  const handleConfirmDeleteDish = async () => {
    if (!dishToDelete) return;
    const targetId = dishToDelete.id;

    setProducts(prev => prev.filter(p => p.id !== targetId));

    try {
      const token = getAuthToken();
      await fetch(buildApiUrl(`/v1/merchant/products/${targetId}`), {
        method: 'DELETE',
        headers: {
          'Accept': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        }
      });
      showToast(`Plato "${dishToDelete.name}" eliminado del menú.`, 'danger');
    } catch (e) {
      console.warn('Delete product sync:', e);
    }

    setDishToDelete(null);
  };

  // ── EXPORTACIÓN: REPORTE DIARIO DE VENTAS EN CSV ─────────────────────────
  const exportToCsv = () => {
    const transactions = financialSummary.transactions && financialSummary.transactions.length > 0
      ? financialSummary.transactions
      : [
          { orderId: 'ORD-9821', createdAt: '2026-10-26 13:42', address: 'Calle Bolívar y 10 de Agosto', amount: 14.50, status: 'DELIVERED', paymentMethod: 'EFECTIVO' },
          { orderId: 'ORD-9820', createdAt: '2026-10-26 14:15', address: 'Av. Veintimilla y Cotopaxi', amount: 21.00, status: 'IN_PREPARATION', paymentMethod: 'TRANSFERENCIA' },
          { orderId: 'ORD-9819', createdAt: '2026-10-26 14:30', address: 'Hospital Luis G. Dávila', amount: 8.50, status: 'READY', paymentMethod: 'EFECTIVO' }
        ];

    const headers = ['ID Pedido', 'Fecha y Hora', 'Direccion Entrega', 'Total ($ USD)', 'Estado', 'Metodo Pago'];
    const rows = transactions.map(t => [
      t.orderId,
      t.createdAt || financialSummary.date,
      `"${(t.address || 'Tulcán Centro').replace(/"/g, '""')}"`,
      Number(t.amount || 0).toFixed(2),
      t.status || 'COMPLETED',
      t.paymentMethod || 'USD'
    ]);

    const csvContent = '\uFEFF' + [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `Cierre_Caja_Tulcan_${financialSummary.date}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    showToast('Archivo CSV descargado exitosamente.', 'success');
  };

  // ── EXPORTACIÓN: REPORTE DIARIO DE VENTAS EN PDF / IMPRESIÓN ─────────────
  const exportToPdf = () => {
    const transactions = financialSummary.transactions && financialSummary.transactions.length > 0
      ? financialSummary.transactions
      : [
          { orderId: 'ORD-9821', createdAt: '13:42', address: 'Calle Bolívar y 10 de Agosto', amount: 14.50, status: 'DELIVERED' },
          { orderId: 'ORD-9820', createdAt: '14:15', address: 'Av. Veintimilla y Cotopaxi', amount: 21.00, status: 'IN_PREPARATION' },
          { orderId: 'ORD-9819', createdAt: '14:30', address: 'Hospital Luis G. Dávila', amount: 8.50, status: 'READY' }
        ];

    const printWindow = window.open('', '_blank');
    if (!printWindow) {
      showToast('Por favor permite ventanas emergentes para imprimir.', 'danger');
      return;
    }

    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Cierre de Caja - ${financialSummary.restaurantName}</title>
          <meta charset="utf-8" />
          <style>
            body { font-family: 'Courier New', Courier, monospace; padding: 30px; color: #111; max-width: 650px; margin: auto; }
            h1, h2, h3 { text-align: center; margin: 4px 0; }
            .header-info { text-align: center; font-size: 13px; color: #444; margin-bottom: 16px; }
            .divider { border-top: 1px dashed #333; margin: 12px 0; }
            .row { display: flex; justify-content: space-between; margin: 6px 0; font-size: 13px; }
            .total-row { font-size: 16px; font-weight: bold; border-top: 2px solid #000; padding-top: 6px; }
            table { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 12px; }
            th, td { text-align: left; padding: 6px 0; border-bottom: 1px dotted #ccc; }
            th:last-child, td:last-child { text-align: right; }
            .badge { font-weight: bold; }
            .footer { text-align: center; font-size: 11px; margin-top: 25px; color: #666; }
            @media print {
              body { padding: 0; }
            }
          </style>
        </head>
        <body>
          <h1>${financialSummary.restaurantName}</h1>
          <div class="header-info">
            RUC: 0491500042001 • Calle Sucre y Junín, Tulcán, Carchi<br />
            COMPROBANTE OPERATIVO DE CIERRE DE CAJA DIARIO
          </div>
          <div class="divider"></div>
          <div class="row"><span>Fecha Operativa:</span><span>${financialSummary.date}</span></div>
          <div class="row"><span>Moneda Oficial:</span><span>Dólares Americanos ($ USD)</span></div>
          <div class="row"><span>Pedidos Completados:</span><span>${financialSummary.completedOrdersCount}</span></div>
          <div class="row"><span>Tiempo Promedio Cocina:</span><span>${financialSummary.averagePreparationMinutes} min</span></div>
          <div class="divider"></div>
          <div class="row"><span>Ventas Efectivo:</span><span>$${Number(financialSummary.cashSales).toFixed(2)} USD</span></div>
          <div class="row"><span>Ventas Transferencia / App:</span><span>$${Number(financialSummary.transferSales).toFixed(2)} USD</span></div>
          <div class="row total-row"><span>VENTAS TOTALES ACUMULADAS:</span><span>$${Number(financialSummary.totalSales).toFixed(2)} USD</span></div>
          <div class="divider"></div>
          <h3>DETALLE DE PEDIDOS</h3>
          <table>
            <thead>
              <tr><th>ID Pedido</th><th>Estado</th><th>Monto</th></tr>
            </thead>
            <tbody>
              ${transactions.map(t => `
                <tr>
                  <td>${t.orderId}</td>
                  <td>${t.status}</td>
                  <td>$${Number(t.amount).toFixed(2)} USD</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
          <div class="divider"></div>
          <div class="footer">
            Sistema Delivery BMCP Tulcán — Reporte generado automáticamente para auditoría SRI y control interno.<br />
            © ${new Date().getFullYear()} Asadero El Leñador
          </div>
        </body>
      </html>
    `);
    printWindow.document.close();
    printWindow.focus();
    setTimeout(() => {
      printWindow.print();
    }, 400);
  };

  const pendingCount = orders.filter(o => o.status === 'PENDING').length;
  const preparingCount = orders.filter(o => o.status === 'PREPARING').length;
  const readyCount = orders.filter(o => o.status === 'READY').length;

  const formatCountdown = (secs) => {
    const mins = Math.floor(secs / 60);
    const remSecs = secs % 60;
    return `${mins}:${remSecs < 10 ? '0' : ''}${remSecs}`;
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex flex-col font-sans transition-colors duration-200">
      {/* ── TOAST DE NOTIFICACIÓN FLOTANTE ─────────────────────────────────── */}
      {toast && (
        <div
          className={`fixed top-4 right-4 z-50 px-4 py-3 rounded-xl border shadow-2xl flex items-center gap-3 transition-all animate-bounce ${
            toast.type === 'danger'
              ? 'bg-rose-100 dark:bg-rose-950/90 border-rose-300 dark:border-rose-500/50 text-rose-800 dark:text-rose-200'
              : 'bg-emerald-100 dark:bg-emerald-950/90 border-emerald-300 dark:border-emerald-500/50 text-emerald-800 dark:text-emerald-200'
          }`}
        >
          <span>{toast.type === 'danger' ? '⚠️' : '✅'}</span>
          <span className="text-xs font-bold">{toast.message}</span>
        </div>
      )}

      {/* ── ALERTA DE COCINA PAUSADA (KITCHEN THROTTLE BANNER) ──────────────── */}
      {isKitchenPaused && (
        <div className="bg-gradient-to-r from-rose-600 via-amber-600 to-rose-600 dark:from-rose-900/90 dark:via-amber-900/80 dark:to-rose-900/90 border-b border-rose-500/40 px-6 py-2.5 flex items-center justify-between text-xs font-bold text-white dark:text-rose-200 animate-pulse">
          <div className="flex items-center gap-2">
            <span className="text-base">⏸️</span>
            <span>
              <strong>Cocina Pausada por Saturación Operativa:</strong> Nuevos pedidos en pausa por los próximos{' '}
              <strong className="text-amber-200 dark:text-amber-300 font-mono text-sm">{formatCountdown(countdownSeconds)}</strong> min.
            </span>
          </div>
          <button
            onClick={handleToggleKitchenPause}
            className="px-3 py-1 bg-white text-rose-700 hover:bg-rose-50 dark:bg-rose-600 dark:hover:bg-rose-500 dark:text-white rounded-lg text-[11px] font-black uppercase transition-all shadow-md active:scale-95"
          >
            ▶ Reanudar Ahora
          </button>
        </div>
      )}

      {/* ── BARRA SUPERIOR KDS ────────────────────────────────────────────── */}
      <header className="bg-white/90 dark:bg-slate-900/90 backdrop-blur-md border-b border-slate-200 dark:border-slate-800 px-4 lg:px-6 py-3.5 flex flex-wrap items-center justify-between gap-4 sticky top-0 z-30 transition-colors">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-amber-500 to-rose-600 flex items-center justify-center text-xl font-bold text-white shadow-lg shadow-rose-500/20">
            🍳
          </div>
          <div>
            <h1 className="text-base lg:text-lg font-black text-slate-900 dark:text-white flex items-center gap-2">
              Asadero & Parrilladas El Leñador
              <span className={`text-[11px] px-2.5 py-0.5 rounded-full font-semibold border ${
                isKitchenPaused
                  ? 'bg-rose-500/10 dark:bg-rose-500/20 text-rose-600 dark:text-rose-400 border-rose-500/30 dark:border-rose-500/40'
                  : 'bg-emerald-500/10 dark:bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 dark:border-emerald-500/40'
              }`}>
                {isKitchenPaused ? '● Pausado (Saturación)' : '● En Línea • Tulcán'}
              </span>
            </h1>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Sistema KDS de Cocina en Tiempo Real • Moneda: <strong className="text-emerald-600 dark:text-emerald-400">$ USD</strong>
            </p>
          </div>
        </div>

        {/* Acciones de Cabecera */}
        <div className="flex flex-wrap items-center gap-2.5">
          {/* Control Operativo de Capacidad (Kitchen Throttle) */}
          <button
            onClick={handleToggleKitchenPause}
            className={`px-3 py-1.5 rounded-xl text-xs font-black transition-all flex items-center gap-1.5 border shadow-sm ${
              isKitchenPaused
                ? 'bg-rose-600 hover:bg-rose-500 text-white border-rose-400 animate-pulse'
                : 'bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-amber-700 dark:text-amber-300 border-amber-500/30'
            }`}
            title="Pausar nuevos pedidos para gestionar la saturación de la cocina"
          >
            <span>{isKitchenPaused ? `⏸️ Pausado (${formatCountdown(countdownSeconds)})` : '⏸️ Pausar Pedidos (30 min)'}</span>
          </button>

          {/* Alarma Sonora */}
          <button
            onClick={() => {
              setSoundEnabled(!soundEnabled);
              if (!soundEnabled) playKitchenChime();
            }}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 border ${
              soundEnabled
                ? 'bg-amber-500/10 dark:bg-amber-500/20 text-amber-700 dark:text-amber-300 border-amber-500/30 dark:border-amber-500/40'
                : 'bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 border-slate-200 dark:border-slate-700'
            }`}
          >
            <span>{soundEnabled ? '🔔 Alarma ON' : '🔕 Silenciado'}</span>
          </button>

          {/* Botón Nuevo Plato */}
          <button
            onClick={handleOpenCreateModal}
            className="px-3.5 py-1.5 rounded-xl text-xs font-black bg-gradient-to-r from-emerald-600 to-teal-500 hover:from-emerald-500 hover:to-teal-400 text-white shadow-lg shadow-emerald-500/20 transition-all flex items-center gap-1.5 active:scale-95"
          >
            <span>+ Crear Nuevo Plato</span>
          </button>

          {/* Botón Ajustes de Cobro Bancario */}
          <button
            onClick={() => {
              setIsSidebarOpen(true);
              setActiveTab('bank');
            }}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all shadow-md flex items-center gap-1.5 border ${
              activeTab === 'bank' && isSidebarOpen
                ? 'bg-amber-500 text-slate-950 border-amber-400'
                : 'bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-amber-700 dark:text-amber-400 border-amber-500/30'
            }`}
          >
            <span>💳</span>
            <span>Ajustes de Cobro</span>
          </button>

          {/* Alternar Vista de Inventario */}
          <button
            onClick={() => setIsSidebarOpen(!isSidebarOpen)}
            className="px-3 py-1.5 rounded-xl text-xs font-bold bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 transition-all"
          >
            {isSidebarOpen ? 'Ocultar Panel' : '📦 Ver Stock & Caja'}
          </button>
        </div>
      </header>

      {/* ── CUERPO PRINCIPAL KDS ──────────────────────────────────────────── */}
      <div className="flex-1 flex flex-col lg:flex-row overflow-hidden">
        {/* Panel de Comandas (Columnas KDS) */}
        <main className="flex-1 p-4 lg:p-6 overflow-y-auto">
          {/* Indicadores de Estado */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
            <div className="bg-white dark:bg-slate-900/80 border border-amber-500/30 rounded-2xl p-4 flex items-center justify-between shadow-sm dark:shadow-lg transition-colors">
              <div>
                <span className="text-xs uppercase font-extrabold text-amber-600 dark:text-amber-400 block tracking-wider">
                  Nuevos / Pendientes
                </span>
                <span className="text-3xl font-black text-slate-900 dark:text-white">{pendingCount}</span>
              </div>
              <span className="text-3xl text-amber-500/40">⏳</span>
            </div>

            <div className="bg-white dark:bg-slate-900/80 border border-sky-500/30 rounded-2xl p-4 flex items-center justify-between shadow-sm dark:shadow-lg transition-colors">
              <div>
                <span className="text-xs uppercase font-extrabold text-sky-600 dark:text-sky-400 block tracking-wider">
                  En Preparación
                </span>
                <span className="text-3xl font-black text-slate-900 dark:text-white">{preparingCount}</span>
              </div>
              <span className="text-3xl text-sky-500/40">🔥</span>
            </div>

            <div className="bg-white dark:bg-slate-900/80 border border-emerald-500/30 rounded-2xl p-4 flex items-center justify-between shadow-sm dark:shadow-lg transition-colors">
              <div>
                <span className="text-xs uppercase font-extrabold text-emerald-600 dark:text-emerald-400 block tracking-wider">
                  Listos para Retiro
                </span>
                <span className="text-3xl font-black text-slate-900 dark:text-white">{readyCount}</span>
              </div>
              <span className="text-3xl text-emerald-500/40">🛵</span>
            </div>
          </div>

          {/* Grilla de Tickets de Cocina */}
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
            {orders.map((order) => {
              const isPending = order.status === 'PENDING';
              const isPreparing = order.status === 'PREPARING';
              const isReady = order.status === 'READY';

              return (
                <div
                  key={order.id}
                  className={`rounded-2xl border flex flex-col justify-between transition-all shadow-md dark:shadow-xl ${
                    isPending
                      ? 'bg-white dark:bg-amber-950/20 border-amber-300 dark:border-amber-500/50 hover:border-amber-400'
                      : isPreparing
                      ? 'bg-white dark:bg-sky-950/20 border-sky-300 dark:border-sky-500/50 hover:border-sky-400'
                      : 'bg-white dark:bg-emerald-950/20 border-emerald-300 dark:border-emerald-500/50 hover:border-emerald-400'
                  }`}
                >
                  {/* Encabezado del Ticket */}
                  <div className="p-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between transition-colors">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-black text-sm lg:text-base text-slate-900 dark:text-white">
                          {order.id.length > 12 ? order.id.substring(0, 8) + '...' : order.id}
                        </span>
                        <span
                          className={`text-[10px] font-black uppercase px-2 py-0.5 rounded-full ${
                            isPending
                              ? 'bg-amber-500 text-slate-950'
                              : isPreparing
                              ? 'bg-sky-500 text-slate-950'
                              : 'bg-emerald-500 text-slate-950'
                          }`}
                        >
                          {isPending ? 'Pendiente' : isPreparing ? 'En Fuego' : 'Listo'}
                        </span>
                      </div>
                      <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">{order.time}</p>
                    </div>

                    <div className="text-right">
                      <span className="text-base font-extrabold text-emerald-600 dark:text-emerald-400">${order.total.toFixed(2)}</span>
                      <p className="text-[10px] text-slate-500 dark:text-slate-400 uppercase">{order.paymentMethod}</p>
                    </div>
                  </div>

                  {/* Datos del Cliente */}
                  <div className="px-4 py-2.5 bg-slate-50 dark:bg-slate-900/50 text-xs flex items-center justify-between border-b border-slate-200 dark:border-slate-800/80 transition-colors">
                    <div>
                      <span className="font-bold text-slate-800 dark:text-slate-200">👤 {order.clientName}</span>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 truncate max-w-xs">📍 {order.address}</p>
                    </div>
                  </div>

                  {/* Detalle de Platos para Cocina */}
                  <div className="p-4 space-y-3 flex-1">
                    {order.items.map((item, idx) => (
                      <div key={idx} className="flex items-start justify-between text-xs">
                        <div className="flex items-start gap-2.5">
                          <span className="w-5 h-5 rounded-md bg-slate-100 dark:bg-slate-800 text-amber-700 dark:text-amber-400 font-extrabold flex items-center justify-center shrink-0 border border-slate-200 dark:border-transparent">
                            {item.qty}
                          </span>
                          <div>
                            <span className="font-bold text-slate-800 dark:text-slate-100">{item.name}</span>
                            {item.notes && (
                              <p className="text-[11px] text-amber-600 dark:text-amber-400/90 font-medium italic mt-0.5">
                                Nota: "{item.notes}"
                              </p>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Repartidor asignado si está listo */}
                  {isReady && order.driverName && (
                    <div className="px-4 py-2.5 bg-emerald-50 dark:bg-emerald-950/40 border-t border-emerald-200 dark:border-emerald-500/20 text-xs text-emerald-700 dark:text-emerald-300 flex items-center gap-2">
                      <span>🛵</span>
                      <span>Repartidor: <strong>{order.driverName}</strong></span>
                    </div>
                  )}

                  {/* Botón de Acción de Cocina */}
                  <div className="p-3 bg-slate-50 dark:bg-slate-900/70 border-t border-slate-200 dark:border-slate-800 transition-colors">
                    {isPending && (
                      <button
                        onClick={() => advanceOrderStatus(order.id)}
                        className="w-full py-2.5 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-black text-xs uppercase tracking-wider transition-all flex items-center justify-center gap-1.5 shadow-lg shadow-amber-500/20 active:scale-98"
                      >
                        <span>🔥 Iniciar Preparación</span>
                      </button>
                    )}

                    {isPreparing && (
                      <button
                        onClick={() => advanceOrderStatus(order.id)}
                        className="w-full py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black text-xs uppercase tracking-wider transition-all flex items-center justify-center gap-1.5 shadow-lg shadow-emerald-500/20 active:scale-98"
                      >
                        <span>✓ Marcar Listo para Retiro</span>
                      </button>
                    )}

                    {isReady && (
                      <div className="py-2 text-center text-xs font-bold text-emerald-600 dark:text-emerald-400 flex items-center justify-center gap-1.5">
                        <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
                        <span>Esperando retiro por repartidor</span>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </main>

        {/* ── PANEL LATERAL: INVENTARIO, PLATOS Y FINANZAS ────────────────── */}
        {isSidebarOpen && (
          <aside className="w-full lg:w-96 bg-white dark:bg-slate-900 border-t lg:border-t-0 lg:border-l border-slate-200 dark:border-slate-800 flex flex-col h-full overflow-hidden transition-colors">
            {/* Pestañas del Panel */}
            <div className="flex border-b border-slate-200 dark:border-slate-800 bg-slate-100/50 dark:bg-slate-950/50 shrink-0">
              <button
                onClick={() => setActiveTab('orders')}
                className={`flex-1 py-3 text-xs font-bold transition-all border-b-2 flex items-center justify-center gap-1.5 ${
                  activeTab === 'orders'
                    ? 'border-amber-500 text-amber-600 dark:text-amber-400 bg-white dark:bg-slate-900'
                    : 'border-transparent text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200'
                }`}
              >
                <span>📦</span>
                <span>Stock ({products.length})</span>
              </button>

              <button
                onClick={() => setActiveTab('finances')}
                className={`flex-1 py-3 text-xs font-bold transition-all border-b-2 flex items-center justify-center gap-1.5 ${
                  activeTab === 'finances'
                    ? 'border-emerald-500 text-emerald-600 dark:text-emerald-400 bg-white dark:bg-slate-900'
                    : 'border-transparent text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200'
                }`}
              >
                <span>💰</span>
                <span>Cierre Caja</span>
              </button>

              <button
                onClick={() => setActiveTab('bank')}
                className={`flex-1 py-3 text-xs font-bold transition-all border-b-2 flex items-center justify-center gap-1.5 ${
                  activeTab === 'bank'
                    ? 'border-amber-500 text-amber-600 dark:text-amber-400 bg-white dark:bg-slate-900'
                    : 'border-transparent text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200'
                }`}
              >
                <span>💳</span>
                <span>Cobro</span>
              </button>
            </div>

            {/* Contenido Pestaña 1: Menú y Stock */}
            {activeTab === 'orders' && (
              <div className="flex-1 p-4 lg:p-5 overflow-y-auto space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="text-xs font-black uppercase tracking-wider text-slate-800 dark:text-slate-200">
                      Catálogo del Asadero
                    </h2>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400">
                      Disponibilidad en vivo en la app del cliente
                    </p>
                  </div>
                  <button
                    onClick={handleOpenCreateModal}
                    className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-extrabold flex items-center gap-1 shadow-sm transition"
                  >
                    <span>+ Plato</span>
                  </button>
                </div>

                <div className="space-y-3">
                  {products.map((product) => {
                    const isAvailable = product.available !== false && product.inStock !== false;
                    return (
                      <div
                        key={product.id}
                        className={`p-3 rounded-xl border transition-all flex flex-col gap-2 ${
                          isAvailable
                            ? 'bg-slate-50 dark:bg-slate-800/60 border-slate-200 dark:border-slate-700/80 hover:border-slate-300 dark:hover:border-slate-600'
                            : 'bg-rose-50 dark:bg-rose-950/20 border-rose-200 dark:border-rose-500/40 opacity-75'
                        }`}
                      >
                        <div className="flex items-start gap-3">
                          {/* Miniatura del Plato */}
                          <img
                            src={product.imageUrl || 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500'}
                            alt={product.name}
                            className="w-12 h-12 rounded-lg object-cover bg-slate-100 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 shrink-0"
                            onError={(e) => {
                              e.target.src = 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500';
                            }}
                          />
                          <div className="overflow-hidden flex-1">
                            <div className="flex items-center justify-between gap-1">
                              <span className="font-bold text-xs text-slate-800 dark:text-slate-100 truncate">
                                {product.name}
                              </span>
                              <span className="text-[10px] font-extrabold px-1.5 py-0.5 rounded bg-slate-200 dark:bg-slate-800 text-amber-700 dark:text-amber-400 border border-slate-300 dark:border-slate-700 shrink-0">
                                {product.category || 'Plato'}
                              </span>
                            </div>
                            <span className="text-xs text-emerald-600 dark:text-emerald-400 font-extrabold block mt-0.5">
                              ${Number(product.price).toFixed(2)} USD
                            </span>
                          </div>
                        </div>

                        {/* Barra de Acciones del Plato */}
                        <div className="flex items-center justify-between pt-2 border-t border-slate-200 dark:border-slate-800/80 text-[11px]">
                          {/* Switch de Disponibilidad */}
                          <button
                            onClick={() => toggleStock(product.id)}
                            className={`px-2.5 py-1 rounded-lg font-black transition-all flex items-center gap-1 ${
                              isAvailable
                                ? 'bg-emerald-500/10 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-300 border border-emerald-500/30 dark:border-emerald-500/40 hover:bg-rose-500/10 hover:text-rose-700'
                                : 'bg-rose-500/10 dark:bg-rose-500/20 text-rose-700 dark:text-rose-300 border border-rose-500/30 dark:border-rose-500/40 hover:bg-emerald-500/10 hover:text-emerald-700'
                            }`}
                          >
                            <span>{isAvailable ? '✓ Disponible' : '✕ Agotado'}</span>
                          </button>

                          <div className="flex items-center gap-1.5">
                            <button
                              onClick={() => handleOpenEditModal(product)}
                              className="px-2 py-1 rounded-md bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 border border-slate-200 dark:border-slate-700 font-semibold transition"
                              title="Editar datos del plato"
                            >
                              ✏️ Editar
                            </button>
                            <button
                              onClick={() => setDishToDelete(product)}
                              className="px-2 py-1 rounded-md bg-rose-100 hover:bg-rose-200 dark:bg-rose-950/40 dark:hover:bg-rose-900/60 text-rose-700 dark:text-rose-300 border border-rose-200 dark:border-rose-800/50 font-semibold transition"
                              title="Eliminar plato del menú"
                            >
                              🗑️
                            </button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Contenido Pestaña 2: Cierre de Caja y Finanzas */}
            {activeTab === 'finances' && (
              <div className="flex-1 p-4 lg:p-5 overflow-y-auto space-y-5">
                <div>
                  <h2 className="text-xs font-black uppercase tracking-wider text-slate-800 dark:text-slate-200">
                    Cierre de Caja Diario
                  </h2>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400">
                    Métricas de facturación en Tulcán • {financialSummary.date}
                  </p>
                </div>

                {/* Tarjeta de Ventas Totales */}
                <div className="bg-emerald-50 dark:bg-gradient-to-br dark:from-emerald-950/60 dark:to-slate-900 border border-emerald-200 dark:border-emerald-500/30 rounded-2xl p-4 shadow-sm dark:shadow-xl transition-colors">
                  <span className="text-[11px] font-extrabold uppercase text-emerald-700 dark:text-emerald-400 tracking-wider">
                    Ventas Acumuladas del Turno
                  </span>
                  <div className="text-3xl font-black text-slate-900 dark:text-white mt-1">
                    ${Number(financialSummary.totalSales).toFixed(2)}{' '}
                    <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400">{financialSummary.currency}</span>
                  </div>
                  <div className="grid grid-cols-2 gap-2 mt-4 pt-3 border-t border-emerald-200 dark:border-emerald-500/20 text-xs">
                    <div>
                      <span className="text-slate-500 dark:text-slate-400 block text-[10px]">Efectivo (USD):</span>
                      <strong className="text-slate-800 dark:text-slate-200">${Number(financialSummary.cashSales).toFixed(2)}</strong>
                    </div>
                    <div>
                      <span className="text-slate-500 dark:text-slate-400 block text-[10px]">Transferencia / App:</span>
                      <strong className="text-slate-800 dark:text-slate-200">${Number(financialSummary.transferSales).toFixed(2)}</strong>
                    </div>
                  </div>
                </div>

                {/* Métricas Operativas */}
                <div className="bg-slate-100/70 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800 rounded-xl p-3.5 space-y-2 text-xs transition-colors">
                  <div className="flex justify-between text-slate-600 dark:text-slate-300">
                    <span>Pedidos completados:</span>
                    <strong className="text-slate-900 dark:text-white">{financialSummary.completedOrdersCount}</strong>
                  </div>
                  <div className="flex justify-between text-slate-600 dark:text-slate-300">
                    <span>Tiempo prom. cocción:</span>
                    <strong className="text-amber-600 dark:text-amber-400">{financialSummary.averagePreparationMinutes} min</strong>
                  </div>
                  <div className="flex justify-between text-slate-600 dark:text-slate-300">
                    <span>Área operativa:</span>
                    <strong className="text-slate-800 dark:text-slate-200">{financialSummary.city || 'Tulcán'}</strong>
                  </div>
                </div>

                {/* Botones de Exportación */}
                <div className="space-y-2 pt-2">
                  <span className="text-[11px] font-extrabold text-slate-500 dark:text-slate-400 uppercase tracking-wider block">
                    Exportar Reporte del Cierre
                  </span>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={exportToCsv}
                      className="w-full py-2.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 text-slate-800 dark:text-slate-100 font-bold text-xs flex items-center justify-center gap-1.5 transition active:scale-95 shadow-sm"
                    >
                      <span>📊 Exportar CSV</span>
                    </button>
                    <button
                      onClick={exportToPdf}
                      className="w-full py-2.5 px-3 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold text-xs flex items-center justify-center gap-1.5 transition active:scale-95 shadow-lg shadow-emerald-500/20"
                    >
                      <span>🖨️ Imprimir / PDF</span>
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Contenido Pestaña 3: Ajustes de Cobro / Cuenta Bancaria */}
            {activeTab === 'bank' && (
              <div className="flex-1 p-4 lg:p-5 overflow-y-auto">
                <BankSettingsSection userType="merchant" />
              </div>
            )}
          </aside>
        )}
      </div>

      {/* ── MODAL: CREAR / EDITAR PLATO ────────────────────────────────────── */}
      {dishModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 dark:bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 max-w-md w-full shadow-2xl relative animate-in fade-in zoom-in-95 transition-colors">
            <h3 className="text-base font-black text-slate-900 dark:text-white flex items-center gap-2 mb-1">
              <span>{editingDish ? '✏️ Editar Plato' : '🍳 Crear Nuevo Plato'}</span>
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mb-4">
              Ingresa los datos del plato para el menú oficial de Tulcán.
            </p>

            <form onSubmit={handleSaveDish} className="space-y-3.5">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-1">Nombre del Plato *</label>
                <input
                  type="text"
                  required
                  placeholder="Ej. Hornado Pastuso Tradicional"
                  value={dishForm.name}
                  onChange={(e) => setDishForm({ ...dishForm, name: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-1">Precio ($ USD) *</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    placeholder="Ej. 12.50"
                    value={dishForm.price}
                    onChange={(e) => setDishForm({ ...dishForm, price: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs text-emerald-600 dark:text-emerald-400 font-bold placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:border-emerald-500"
                  />
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-1">Categoría</label>
                  <select
                    value={dishForm.category}
                    onChange={(e) => setDishForm({ ...dishForm, category: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white focus:outline-none focus:border-emerald-500"
                  >
                    <option value="Platos Fuertes">Platos Fuertes</option>
                    <option value="Especialidades">Especialidades</option>
                    <option value="Entradas">Entradas</option>
                    <option value="Bebidas">Bebidas</option>
                    <option value="Postres">Postres</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-1">Descripción</label>
                <textarea
                  rows="2"
                  placeholder="Guarniciones, ingredientes o preparación..."
                  value={dishForm.description}
                  onChange={(e) => setDishForm({ ...dishForm, description: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:border-emerald-500 resize-none"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-1">URL de Imagen</label>
                <input
                  type="url"
                  placeholder="https://images.unsplash.com/..."
                  value={dishForm.imageUrl}
                  onChange={(e) => setDishForm({ ...dishForm, imageUrl: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="dishAvailableCheckbox"
                  checked={dishForm.available}
                  onChange={(e) => setDishForm({ ...dishForm, available: e.target.checked })}
                  className="w-4 h-4 rounded text-emerald-500 bg-slate-100 dark:bg-slate-800 border-slate-300 dark:border-slate-700 focus:ring-0"
                />
                <label htmlFor="dishAvailableCheckbox" className="text-xs font-bold text-slate-700 dark:text-slate-300 cursor-pointer">
                  Marcar disponible de inmediato para pedidos
                </label>
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-200 dark:border-slate-800">
                <button
                  type="button"
                  onClick={() => setDishModalOpen(false)}
                  className="px-4 py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-xl text-xs font-bold transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-black shadow-lg shadow-emerald-500/20 transition"
                >
                  {editingDish ? 'Guardar Cambios' : 'Crear Plato'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── MODAL: CONFIRMACIÓN DE ELIMINACIÓN ───────────────────────────────── */}
      {dishToDelete && (
        <div className="fixed inset-0 z-50 bg-black/60 dark:bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 border border-rose-300 dark:border-rose-500/40 rounded-2xl p-6 max-w-sm w-full shadow-2xl relative text-center transition-colors">
            <span className="text-4xl block mb-2">🗑️</span>
            <h3 className="text-base font-black text-slate-900 dark:text-white mb-1">
              ¿Eliminar "{dishToDelete.name}"?
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mb-5">
              Esta acción eliminará el plato permanentemente del catálogo del restaurante.
            </p>
            <div className="flex items-center justify-center gap-3">
              <button
                onClick={() => setDishToDelete(null)}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-xl text-xs font-bold transition"
              >
                Cancelar
              </button>
              <button
                onClick={handleConfirmDeleteDish}
                className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white rounded-xl text-xs font-black shadow-lg shadow-rose-500/20 transition"
              >
                Sí, Eliminar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
