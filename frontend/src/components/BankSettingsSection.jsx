// ═══════════════════════════════════════════════════════════════════════
// Delivery Multinivel BMCP — Tulcán, Ecuador
// Componente de Ajustes de Perfil / Cuenta Bancaria y Código QR de Cobro
// ═══════════════════════════════════════════════════════════════════════

import React, { useState, useEffect } from 'react';
import { buildApiUrl, getAuthToken } from '../services/api';
import { getCurrentUser } from '../utils/auth';

const ECUADOR_BANKS = [
  'Banco Pichincha',
  'Banco Guayaquil',
  'Produbanco',
  'Banco del Pacífico',
  'Cooperativa Tulcán',
  'Cooperativa Pablo Muñoz Vega',
  'Banco Bolivariano',
  'Banco Internacional'
];

export default function BankSettingsSection({ userType = 'merchant', onClose, onSaved }) {
  const [bankName, setBankName] = useState('Banco Pichincha');
  const [accountType, setAccountType] = useState('Ahorros');
  const [accountNumber, setAccountNumber] = useState('');
  const [accountHolderId, setAccountHolderId] = useState('');
  const [accountHolderName, setAccountHolderName] = useState('');
  const [qrCodeUrl, setQrCodeUrl] = useState('');

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [toastMessage, setToastMessage] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);
  const [copiedField, setCopiedField] = useState(null);

  const currentUser = getCurrentUser();
  const isMerchant = userType === 'merchant';
  const endpointPath = isMerchant ? '/v1/merchant/bank-details' : '/v1/driver/bank-details';

  // Cargar datos actuales desde Spring Boot PostgreSQL
  useEffect(() => {
    let isMounted = true;

    async function loadBankDetails() {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const token = getAuthToken();
        const res = await fetch(buildApiUrl(endpointPath), {
          method: 'GET',
          headers: {
            'Accept': 'application/json',
            'ngrok-skip-browser-warning': 'true',
            'Bypass-Tunnel-Reminder': 'true',
            ...(token ? { Authorization: `Bearer ${token}` } : {})
          }
        });

        if (res.ok) {
          const data = await res.json();
          if (isMounted && data) {
            setBankName(data.bankName || 'Banco Pichincha');
            setAccountType(data.accountType || 'Ahorros');
            setAccountNumber(data.accountNumber || '');
            setAccountHolderId(data.accountHolderId || '');
            setAccountHolderName(data.accountHolderName || '');
            setQrCodeUrl(data.qrCodeUrl || '');
          }
        } else {
          // Intentar cargar de localStorage si el backend aún no respondió
          const fallback = localStorage.getItem('active_merchant_bank_info');
          if (fallback && isMounted) {
            const parsed = JSON.parse(fallback);
            setBankName(parsed.bankName || 'Banco Pichincha');
            setAccountType(parsed.accountType || 'Ahorros');
            setAccountNumber(parsed.accountNumber || '');
            setAccountHolderId(parsed.accountHolderId || '');
            setAccountHolderName(parsed.accountHolderName || '');
            setQrCodeUrl(parsed.qrCodeUrl || '');
          }
        }
      } catch (err) {
        console.warn('Cargando configuración bancaria previa:', err);
      } finally {
        if (isMounted) setIsLoading(false);
      }
    }

    loadBankDetails();
    return () => {
      isMounted = false;
    };
  }, [endpointPath]);

  // Copiar al portapapeles
  const handleCopy = (text, field) => {
    if (navigator?.clipboard?.writeText) {
      navigator.clipboard.writeText(text);
      setCopiedField(field);
      setTimeout(() => setCopiedField(null), 2000);
    }
  };

  // Guardar cambios en el backend y persistir en PostgreSQL
  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage(null);
    setToastMessage(null);

    // Validaciones requeridas
    if (!accountNumber.trim()) {
      setErrorMessage('Por favor ingresa el número de cuenta bancaria.');
      return;
    }
    if (!accountHolderId.trim()) {
      setErrorMessage('Por favor ingresa la Cédula o RUC del titular de la cuenta.');
      return;
    }
    if (!accountHolderName.trim()) {
      setErrorMessage('Por favor ingresa el nombre completo del titular.');
      return;
    }

    setIsSaving(true);

    const payload = {
      bankName: bankName.trim(),
      accountType: accountType.trim(),
      accountNumber: accountNumber.trim(),
      accountHolderId: accountHolderId.trim(),
      accountHolderName: accountHolderName.trim(),
      qrCodeUrl: qrCodeUrl.trim() || null
    };

    try {
      const token = getAuthToken();
      const res = await fetch(buildApiUrl(endpointPath), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
          'ngrok-skip-browser-warning': 'true',
          'Bypass-Tunnel-Reminder': 'true',
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: JSON.stringify(payload)
      });

      if (!res.ok) {
        const errData = await res.json().catch(() => ({}));
        throw new Error(errData.message || 'Error al actualizar los datos bancarios en el servidor.');
      }

      const updated = await res.json();

      // Guardar también en localStorage para persistencia visual inmediata en Checkout
      try {
        localStorage.setItem('active_merchant_bank_info', JSON.stringify(updated));
        if (currentUser?.email) {
          localStorage.setItem(`bank_account_${currentUser.email}`, JSON.stringify(updated));
        }
      } catch (storageErr) {
        console.warn('Storage sync warn:', storageErr);
      }

      setToastMessage('🎉 ¡Datos bancarios actualizados correctamente en PostgreSQL!');
      setTimeout(() => setToastMessage(null), 4000);

      if (onSaved) {
        onSaved(updated);
      }
    } catch (err) {
      setErrorMessage(err.message || 'Error al conectar con el servidor.');
    } finally {
      setIsSaving(false);
    }
  };

  const isFormIncomplete = !accountNumber || !accountHolderId || !accountHolderName;

  return (
    <div className="bg-white dark:bg-slate-900 rounded-3xl p-6 sm:p-8 shadow-2xl border border-slate-200 dark:border-slate-800 text-slate-900 dark:text-slate-100 transition-all max-w-3xl mx-auto animate-fadeIn">
      
      {/* Encabezado */}
      <div className="flex flex-wrap items-center justify-between gap-4 pb-6 border-b border-slate-200 dark:border-slate-800">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-amber-500 to-emerald-500 shadow-lg shadow-amber-500/20 flex items-center justify-center text-2xl font-black text-white">
            💳
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-xl sm:text-2xl font-black tracking-tight text-slate-900 dark:text-white">
                Ajustes de Cobro / Datos Bancarios
              </h2>
              <span className="text-[10px] px-2 py-0.5 rounded-full font-bold bg-amber-500/20 text-amber-700 dark:text-amber-400 border border-amber-500/30">
                {isMerchant ? '🏪 Restaurante' : '🛵 Repartidor'}
              </span>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Configura tu cuenta bancaria y código QR para recibir transferencias directas de clientes en Tulcán ($ USD)
            </p>
          </div>
        </div>

        {onClose && (
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-500 dark:text-slate-400 flex items-center justify-center font-bold text-sm transition-all"
            title="Cerrar ajustes"
          >
            ✕
          </button>
        )}
      </div>

      {/* Alerta de datos incompletos */}
      {isFormIncomplete && !isLoading && (
        <div className="mt-4 p-3.5 rounded-2xl bg-amber-500/10 border border-amber-500/30 text-amber-800 dark:text-amber-300 text-xs flex items-start gap-3 animate-fadeIn">
          <span className="text-lg">⚠️</span>
          <div>
            <strong className="block font-bold">Configuración de cobro requerida</strong>
            <p className="text-[11px] text-amber-700 dark:text-amber-400 mt-0.5 leading-relaxed">
              Completa tu número de cuenta y titular para que los clientes puedan seleccionarte en el checkout al pagar con Banco Pichincha o DeUna.
            </p>
          </div>
        </div>
      )}

      {/* Toast de Éxito */}
      {toastMessage && (
        <div className="mt-4 p-3.5 rounded-2xl bg-emerald-500/15 border border-emerald-500/40 text-emerald-800 dark:text-emerald-300 text-xs font-bold flex items-center gap-2 animate-bounce">
          <span>✓</span>
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Mensaje de Error */}
      {errorMessage && (
        <div className="mt-4 p-3.5 rounded-2xl bg-rose-500/15 border border-rose-500/40 text-rose-800 dark:text-rose-300 text-xs font-bold flex items-center gap-2 animate-shake">
          <span>⚠️</span>
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Formulario */}
      {isLoading ? (
        <div className="py-16 text-center space-y-3">
          <div className="w-8 h-8 border-4 border-amber-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
          <p className="text-xs text-slate-400">Cargando información bancaria desde PostgreSQL...</p>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="mt-6 space-y-5">
          
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            
            {/* Banco / Cooperativa */}
            <div>
              <label className="block text-[11px] font-extrabold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1.5">
                Entidad Financiera *
              </label>
              <select
                value={bankName}
                onChange={(e) => setBankName(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/90 border border-slate-300 dark:border-slate-700 text-sm font-semibold text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500"
              >
                {ECUADOR_BANKS.map((bank) => (
                  <option key={bank} value={bank}>
                    {bank}
                  </option>
                ))}
              </select>
            </div>

            {/* Tipo de Cuenta */}
            <div>
              <label className="block text-[11px] font-extrabold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1.5">
                Tipo de Cuenta *
              </label>
              <select
                value={accountType}
                onChange={(e) => setAccountType(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/90 border border-slate-300 dark:border-slate-700 text-sm font-semibold text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500"
              >
                <option value="Ahorros">Cuenta de Ahorros</option>
                <option value="Corriente">Cuenta Corriente</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            
            {/* Número de Cuenta */}
            <div>
              <div className="flex justify-between items-center mb-1.5">
                <label className="block text-[11px] font-extrabold uppercase tracking-wider text-slate-700 dark:text-slate-300">
                  Número de Cuenta Bancaria *
                </label>
                {accountNumber && (
                  <button
                    type="button"
                    onClick={() => handleCopy(accountNumber, 'acc')}
                    className="text-[10px] font-bold text-amber-600 dark:text-amber-400 hover:underline flex items-center gap-1"
                  >
                    {copiedField === 'acc' ? '✓ Copiado' : '📋 Copiar'}
                  </button>
                )}
              </div>
              <input
                type="text"
                required
                value={accountNumber}
                onChange={(e) => setAccountNumber(e.target.value)}
                placeholder="ej. 2201928374"
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/90 border border-slate-300 dark:border-slate-700 text-sm font-mono font-bold text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>

            {/* Cédula / RUC del Titular */}
            <div>
              <div className="flex justify-between items-center mb-1.5">
                <label className="block text-[11px] font-extrabold uppercase tracking-wider text-slate-700 dark:text-slate-300">
                  Cédula / RUC del Titular *
                </label>
                {accountHolderId && (
                  <button
                    type="button"
                    onClick={() => handleCopy(accountHolderId, 'ruc')}
                    className="text-[10px] font-bold text-amber-600 dark:text-amber-400 hover:underline flex items-center gap-1"
                  >
                    {copiedField === 'ruc' ? '✓ Copiado' : '📋 Copiar'}
                  </button>
                )}
              </div>
              <input
                type="text"
                required
                value={accountHolderId}
                onChange={(e) => setAccountHolderId(e.target.value)}
                placeholder="ej. 0401829371 o 0491500042001"
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/90 border border-slate-300 dark:border-slate-700 text-sm font-mono font-bold text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
          </div>

          {/* Nombre Completo del Titular */}
          <div>
            <label className="block text-[11px] font-extrabold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1.5">
              Nombre Completo del Titular *
            </label>
            <input
              type="text"
              required
              value={accountHolderName}
              onChange={(e) => setAccountHolderName(e.target.value)}
              placeholder="ej. Patricio Benavides Cárdenas"
              className="w-full px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/90 border border-slate-300 dark:border-slate-700 text-sm font-bold text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>

          {/* Código QR de DeUna / Pichincha */}
          <div className="p-4 rounded-2xl bg-purple-500/10 border border-purple-500/20 space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <label className="block text-[11px] font-extrabold uppercase tracking-wider text-purple-900 dark:text-purple-300">
                Código QR DeUna! / Pichincha Mi Vecino (Opcional)
              </label>
              <button
                type="button"
                onClick={() => setQrCodeUrl(`https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=DEUNA_TULCAN_${userType.toUpperCase()}_${accountNumber || '2201928374'}`)}
                className="text-[11px] font-bold text-purple-700 dark:text-purple-300 hover:underline flex items-center gap-1"
              >
                ⚡ Usar QR Demo Tulcán
              </button>
            </div>

            <input
              type="url"
              value={qrCodeUrl}
              onChange={(e) => setQrCodeUrl(e.target.value)}
              placeholder="https://... URL de tu código QR generado por tu app bancaria"
              className="w-full px-3.5 py-2.5 rounded-xl bg-white dark:bg-slate-800 border border-purple-300 dark:border-purple-500/40 text-xs text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
            />

            {qrCodeUrl ? (
              <div className="p-3 bg-white dark:bg-slate-950/70 rounded-xl border border-purple-200 dark:border-purple-500/30 flex items-center gap-4">
                <img
                  src={qrCodeUrl}
                  alt="Vista previa QR"
                  className="w-16 h-16 object-contain rounded-lg border border-purple-300 bg-white p-1 shrink-0"
                />
                <div className="text-xs">
                  <span className="font-extrabold text-purple-900 dark:text-purple-300 block">
                    ✓ Código QR Listo
                  </span>
                  <span className="text-[11px] text-slate-500 dark:text-slate-400 block mt-0.5">
                    Este código será escaneado en vivo por tus clientes durante el checkout al seleccionar DeUna.
                  </span>
                </div>
              </div>
            ) : (
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                Pega el link de la imagen de tu código QR de DeUna o genera uno de prueba para que los clientes puedan escanearlo.
              </p>
            )}
          </div>

          {/* Tarjeta de Vista Previa Resumen */}
          <div className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700/80 space-y-2">
            <span className="text-[10px] font-extrabold uppercase tracking-wider text-slate-400 block">
              Vista Previa que Verá el Cliente en Tulcán:
            </span>
            <div className="flex flex-wrap items-center justify-between text-xs gap-2">
              <div>
                <strong className="text-slate-900 dark:text-white block font-bold">
                  {bankName} • {accountType}
                </strong>
                <span className="text-slate-500 dark:text-slate-400 text-[11px]">
                  Titular: {accountHolderName || 'No especificado'} • RUC/Cédula: {accountHolderId || 'No especificado'}
                </span>
              </div>
              <span className="font-mono font-bold text-amber-600 dark:text-amber-400 bg-amber-500/10 px-2.5 py-1 rounded-lg border border-amber-500/20 text-xs">
                {accountNumber || 'Cuenta no configurada'}
              </span>
            </div>
          </div>

          {/* Botón de Guardado */}
          <button
            type="submit"
            disabled={isSaving}
            className="w-full py-3.5 px-4 rounded-xl text-sm font-black text-slate-950 bg-gradient-to-r from-amber-400 via-amber-500 to-emerald-400 hover:from-amber-300 hover:to-emerald-300 transition-all shadow-lg shadow-amber-500/25 active:scale-[0.99] flex items-center justify-center gap-2 cursor-pointer disabled:opacity-60"
          >
            {isSaving ? (
              <>
                <div className="w-4 h-4 border-2 border-slate-950 border-t-transparent rounded-full animate-spin"></div>
                <span>Guardando en PostgreSQL...</span>
              </>
            ) : (
              <>
                <span>💾 Guardar Cambios de Cuenta</span>
                <span>➔</span>
              </>
            )}
          </button>
        </form>
      )}

    </div>
  );
}
