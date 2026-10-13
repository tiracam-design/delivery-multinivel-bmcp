import React, { useState } from 'react';
import TrackingPage from './modules/client/pages/TrackingPage.jsx';
import RestaurantCheckout from './modules/client/pages/RestaurantCheckout.jsx';
import ActiveTrip from './modules/driver/pages/ActiveTrip.jsx';
import MerchantApprovals from './modules/admin/pages/MerchantApprovals.jsx';

export default function App() {
  const [activeModule, setActiveModule] = useState('client'); // 'client' | 'driver' | 'admin'
  const [clientSubView, setClientSubView] = useState('checkout'); // 'checkout' | 'tracking'
  const [createdOrder, setCreatedOrder] = useState(null);

  const handleOrderConfirmed = (order) => {
    setCreatedOrder(order);
    setClientSubView('tracking');
  };

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col font-sans">
      {/* ── BARRA SUPERIOR DE SELECTOR DE MÓDULOS ─────────────────────────── */}
      <nav className="bg-slate-900 text-white px-4 py-2.5 shadow-md sticky top-0 z-50 border-b border-slate-800">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-3">
          {/* Logo y Nombre del Sistema */}
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-amber-500 to-emerald-500 flex items-center justify-center font-black text-white text-base shadow-lg shadow-amber-500/20">
              ⚡
            </div>
            <div>
              <span className="font-extrabold text-sm sm:text-base tracking-tight text-white block">
                Delivery Multinivel BMCP • Tulcán 🇪🇨
              </span>
              <div className="flex items-center space-x-2 text-[10px] text-slate-400">
                <span className="flex items-center gap-1 text-emerald-400 font-semibold">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping"></span>
                  Spring Boot :8080 (JWT Active)
                </span>
                <span>•</span>
                <span className="text-sky-300">PostGIS :5432</span>
                <span>•</span>
                <span className="text-amber-300">Redis :6379</span>
              </div>
            </div>
          </div>

          {/* Selector de Vistas / Roles */}
          <div className="flex items-center bg-slate-800 p-1 rounded-xl border border-slate-700 flex-wrap gap-1">
            <button
              onClick={() => setActiveModule('client')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                activeModule === 'client'
                  ? 'bg-amber-500 text-slate-950 shadow-sm'
                  : 'text-slate-300 hover:text-white'
              }`}
            >
              <span>👤</span>
              <span>1. Cliente (Checkout & Tracking)</span>
            </button>

            <button
              onClick={() => setActiveModule('driver')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                activeModule === 'driver'
                  ? 'bg-emerald-500 text-slate-950 shadow-sm'
                  : 'text-slate-300 hover:text-white'
              }`}
            >
              <span>🛵</span>
              <span>2. Repartidor (Active Trip)</span>
            </button>

            <button
              onClick={() => setActiveModule('admin')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                activeModule === 'admin'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-300 hover:text-white'
              }`}
            >
              <span>🛡️</span>
              <span>3. Admin (Aprobaciones & RUC)</span>
            </button>
          </div>
        </div>

        {/* Sub-barra de navegación cuando el módulo Cliente está activo */}
        {activeModule === 'client' && (
          <div className="max-w-7xl mx-auto mt-2 pt-2 border-t border-slate-800/80 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
              <button
                onClick={() => setClientSubView('checkout')}
                className={`px-3 py-1 rounded-md font-bold transition-all flex items-center gap-1.5 ${
                  clientSubView === 'checkout'
                    ? 'bg-slate-700 text-amber-400 border border-amber-500/30'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <span>🛒</span>
                <span>Menú & Checkout ($ USD)</span>
              </button>

              <button
                onClick={() => setClientSubView('tracking')}
                className={`px-3 py-1 rounded-md font-bold transition-all flex items-center gap-1.5 ${
                  clientSubView === 'tracking'
                    ? 'bg-slate-700 text-emerald-400 border border-emerald-500/30'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <span>📍</span>
                <span>Seguimiento GPS & PIN de Entrega</span>
              </button>
            </div>

            <span className="hidden sm:inline text-[11px] text-slate-400">
              Ubicación: <strong className="text-slate-200">Tulcán, Carchi</strong> • Moneda: <strong className="text-emerald-400">$ USD</strong>
            </span>
          </div>
        )}
      </nav>

      {/* ── CONTENIDO DEL MÓDULO ACTIVO ────────────────────────────────────── */}
      <main className="flex-1">
        {activeModule === 'client' && (
          clientSubView === 'checkout' ? (
            <RestaurantCheckout onOrderConfirmed={handleOrderConfirmed} />
          ) : (
            <TrackingPage initialOrder={createdOrder} onBackToMenu={() => setClientSubView('checkout')} />
          )
        )}

        {activeModule === 'driver' && (
          <div className="max-w-md mx-auto shadow-2xl min-h-screen">
            <ActiveTrip />
          </div>
        )}

        {activeModule === 'admin' && <MerchantApprovals />}
      </main>
    </div>
  );
}
