// ═══════════════════════════════════════════════════════════════════════
// Delivery Multinivel BMCP — Tulcán, Ecuador
// Modal de Registro Intencional por Rol con Glassmorphism y Asignación Dinámica
// ═══════════════════════════════════════════════════════════════════════

import React, { useState } from 'react';
import { register } from '../../utils/auth.js';

export default function RegisterModal({ isOpen, onClose, onRegisterSuccess }) {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [role, setRole] = useState('CLIENT'); // 'CLIENT' | 'DRIVER' | 'MERCHANT' | 'ADMIN'
  const [adminCode, setAdminCode] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  // Campos de cuenta bancaria para DRIVER y MERCHANT (Cobro en Tulcán)
  const [bankName, setBankName] = useState('Banco Pichincha');
  const [accountType, setAccountType] = useState('Ahorros');
  const [accountNumber, setAccountNumber] = useState('');
  const [accountHolderId, setAccountHolderId] = useState('');
  const [accountHolderName, setAccountHolderName] = useState('');
  const [qrCodeUrl, setQrCodeUrl] = useState('');

  if (!isOpen) return null;

  const handleRoleSelect = (selectedRole) => {
    setRole(selectedRole);
    setErrorMessage(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage(null);

    // Validaciones básicas
    if (!fullName.trim()) {
      setErrorMessage('Por favor ingresa tu nombre completo.');
      return;
    }
    if (!email.trim() || !email.includes('@')) {
      setErrorMessage('Por favor ingresa un correo electrónico válido.');
      return;
    }
    if (password !== confirmPassword) {
      setErrorMessage('Las contraseñas no coinciden. Por favor verifícalas.');
      return;
    }
    if (!password || password.length < 6) {
      setErrorMessage('La contraseña debe tener al menos 6 caracteres.');
      return;
    }

    // Validación de datos bancarios para Repartidor o Restaurante
    if (role === 'MERCHANT' || role === 'DRIVER') {
      if (!accountNumber.trim()) {
        setErrorMessage(`Por favor ingresa el número de cuenta bancaria para recibir tus pagos como ${role === 'MERCHANT' ? 'Restaurante' : 'Repartidor'}.`);
        return;
      }
      if (!accountHolderId.trim()) {
        setErrorMessage('Por favor ingresa la Cédula o RUC del titular de la cuenta.');
        return;
      }
    }

    setIsLoading(true);

    try {
      const user = await register({
        fullName,
        email,
        password,
        role,
        adminCode,
        phone,
        bankName,
        accountType,
        accountNumber,
        accountHolderId,
        accountHolderName: accountHolderName.trim() || fullName.trim(),
        qrCodeUrl
      });

      if (onRegisterSuccess) {
        onRegisterSuccess(user);
      }
      onClose();
    } catch (err) {
      setErrorMessage(err.message || 'Error al completar el registro.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black/75 backdrop-blur-md flex items-center justify-center p-3 sm:p-4">
      <div className="relative w-full max-w-lg bg-slate-900/95 dark:bg-slate-900/95 bg-white/95 text-slate-100 dark:text-slate-100 text-slate-900 rounded-3xl shadow-2xl border border-slate-700/70 dark:border-slate-800 border-slate-200 p-6 sm:p-8 animate-fadeIn max-h-[92vh] overflow-y-auto">
        
        {/* Botón de Cierre */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-5 right-5 w-8 h-8 rounded-full bg-slate-800/80 dark:bg-slate-800/80 bg-slate-100 hover:bg-slate-700 dark:hover:bg-slate-700 hover:bg-slate-200 text-slate-400 dark:text-slate-400 text-slate-600 hover:text-white dark:hover:text-white flex items-center justify-center text-sm font-bold transition-all"
          title="Cerrar modal"
        >
          ✕
        </button>

        {/* Encabezado */}
        <div className="text-center mb-6 pr-6">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-gradient-to-tr from-amber-500 to-emerald-500 shadow-lg shadow-amber-500/20 mb-2">
            <span className="text-2xl font-black text-white">⚡</span>
          </div>
          <h2 className="text-xl sm:text-2xl font-black tracking-tight text-white dark:text-white text-slate-900">
            Crear Nueva Cuenta
          </h2>
          <p className="text-xs text-slate-400 dark:text-slate-400 text-slate-500 mt-1">
            Plataforma Delivery Tulcán • Selecciona tu perfil operativo
          </p>
        </div>

        {/* Alerta de Error */}
        {errorMessage && (
          <div className="mb-4 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 dark:text-rose-300 text-rose-600 text-xs flex items-start gap-2.5 animate-shake">
            <span className="text-base">⚠️</span>
            <div className="flex-1">
              <strong className="block font-bold">Atención</strong>
              <span>{errorMessage}</span>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          
          {/* Selector de Rol Intencional */}
          <div>
            <label className="block text-[11px] font-extrabold uppercase tracking-wider text-slate-300 dark:text-slate-300 text-slate-700 mb-2">
              Selecciona tu Rol Operativo:
            </label>
            <div className="grid grid-cols-2 gap-2">
              
              {/* Rol 1: Cliente */}
              <button
                type="button"
                onClick={() => handleRoleSelect('CLIENT')}
                className={`p-3 rounded-2xl text-left border transition-all flex flex-col justify-between ${
                  role === 'CLIENT'
                    ? 'bg-amber-500/15 border-amber-500 text-amber-300 ring-2 ring-amber-500/30 shadow-md'
                    : 'bg-slate-800/60 dark:bg-slate-800/60 bg-slate-50 border-slate-700/70 dark:border-slate-700/70 border-slate-200 text-slate-300 dark:text-slate-300 text-slate-700 hover:border-slate-500'
                }`}
              >
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-lg">👤</span>
                  <span className="font-extrabold text-xs">Cliente</span>
                </div>
                <span className="text-[10px] text-slate-400 dark:text-slate-400 text-slate-500 leading-tight">
                  Explorar restaurantes y pedir comida
                </span>
              </button>

              {/* Rol 2: Repartidor */}
              <button
                type="button"
                onClick={() => handleRoleSelect('DRIVER')}
                className={`p-3 rounded-2xl text-left border transition-all flex flex-col justify-between ${
                  role === 'DRIVER'
                    ? 'bg-emerald-500/15 border-emerald-500 text-emerald-300 ring-2 ring-emerald-500/30 shadow-md'
                    : 'bg-slate-800/60 dark:bg-slate-800/60 bg-slate-50 border-slate-700/70 dark:border-slate-700/70 border-slate-200 text-slate-300 dark:text-slate-300 text-slate-700 hover:border-slate-500'
                }`}
              >
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-lg">🛵</span>
                  <span className="font-extrabold text-xs">Repartidor</span>
                </div>
                <span className="text-[10px] text-slate-400 dark:text-slate-400 text-slate-500 leading-tight">
                  Entregas con GPS y validación PIN
                </span>
              </button>

              {/* Rol 3: Restaurante / Cocina */}
              <button
                type="button"
                onClick={() => handleRoleSelect('MERCHANT')}
                className={`p-3 rounded-2xl text-left border transition-all flex flex-col justify-between ${
                  role === 'MERCHANT'
                    ? 'bg-rose-500/15 border-rose-500 text-rose-300 ring-2 ring-rose-500/30 shadow-md'
                    : 'bg-slate-800/60 dark:bg-slate-800/60 bg-slate-50 border-slate-700/70 dark:border-slate-700/70 border-slate-200 text-slate-300 dark:text-slate-300 text-slate-700 hover:border-slate-500'
                }`}
              >
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-lg">🍳</span>
                  <span className="font-extrabold text-xs">Restaurante</span>
                </div>
                <span className="text-[10px] text-slate-400 dark:text-slate-400 text-slate-500 leading-tight">
                  Gestión de comandas y cocina KDS
                </span>
              </button>

              {/* Rol 4: Administrador */}
              <button
                type="button"
                onClick={() => handleRoleSelect('ADMIN')}
                className={`p-3 rounded-2xl text-left border transition-all flex flex-col justify-between ${
                  role === 'ADMIN'
                    ? 'bg-indigo-500/15 border-indigo-500 text-indigo-300 ring-2 ring-indigo-500/30 shadow-md'
                    : 'bg-slate-800/60 dark:bg-slate-800/60 bg-slate-50 border-slate-700/70 dark:border-slate-700/70 border-slate-200 text-slate-300 dark:text-slate-300 text-slate-700 hover:border-slate-500'
                }`}
              >
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-lg">🛡️</span>
                  <span className="font-extrabold text-xs">Admin GAD</span>
                </div>
                <span className="text-[10px] text-slate-400 dark:text-slate-400 text-slate-500 leading-tight">
                  Supervisión general y auditoría
                </span>
              </button>
            </div>
          </div>

          {/* Campo Extra si se elige Administrador */}
          {role === 'ADMIN' && (
            <div className="p-3.5 rounded-2xl bg-indigo-500/10 border border-indigo-500/30 text-xs">
              <div className="flex items-center gap-2 mb-1.5 text-indigo-300 dark:text-indigo-300 text-indigo-700 font-bold">
                <span>🛡️ Validación Administrativa Requerida</span>
              </div>
              <p className="text-[11px] text-slate-400 dark:text-slate-400 text-slate-600 mb-2">
                Si tu correo pertenece a una cuenta oficial de la base de datos (ej. <code className="text-indigo-300 font-mono">admin@deliverytulcan.ec</code>), se asignarán automáticamente los privilegios de <strong>ROLE_ADMIN</strong>. O ingresa tu código de autorización:
              </p>
              <input
                type="password"
                value={adminCode}
                onChange={(e) => setAdminCode(e.target.value)}
                placeholder="Código de autorización GAD (ej. TULCAN_ADMIN_2026)"
                className="w-full px-3 py-2 rounded-xl bg-slate-800/90 dark:bg-slate-800/90 bg-white border border-indigo-500/40 text-xs text-white dark:text-white text-slate-900 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          )}

          {/* Nombre Completo */}
          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-300 dark:text-slate-300 text-slate-700 mb-1">
              Nombre Completo
            </label>
            <input
              type="text"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="ej. Valeria Narváez Guerrón"
              disabled={isLoading}
              className="w-full px-3.5 py-2.5 rounded-xl bg-slate-800/80 dark:bg-slate-800/80 bg-slate-50 border border-slate-700/80 dark:border-slate-700/80 border-slate-300 text-sm text-white dark:text-white text-slate-900 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>

          {/* Correo Electrónico y Teléfono */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-300 dark:text-slate-300 text-slate-700 mb-1">
                Correo Electrónico
              </label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="ej. usuario@tulcan.ec"
                disabled={isLoading}
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-800/80 dark:bg-slate-800/80 bg-slate-50 border border-slate-700/80 dark:border-slate-700/80 border-slate-300 text-sm text-white dark:text-white text-slate-900 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-300 dark:text-slate-300 text-slate-700 mb-1">
                Teléfono / WhatsApp
              </label>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="ej. +593 99 123 4567"
                disabled={isLoading}
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-800/80 dark:bg-slate-800/80 bg-slate-50 border border-slate-700/80 dark:border-slate-700/80 border-slate-300 text-sm text-white dark:text-white text-slate-900 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
          </div>

          {/* Sección de Datos Bancarios Condicional para MERCHANT y DRIVER */}
          {(role === 'MERCHANT' || role === 'DRIVER') && (
            <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30 text-xs space-y-3 animate-fadeIn">
              <div className="flex items-center justify-between border-b border-amber-500/20 pb-2">
                <div className="flex items-center gap-2 text-amber-400 font-extrabold text-xs">
                  <span>💳</span>
                  <span>Información de Cobro / Cuenta Bancaria ({role === 'MERCHANT' ? 'Restaurante' : 'Repartidor'})</span>
                </div>
                <span className="text-[10px] px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 font-bold">
                  Tulcán • Ecuador ($ USD)
                </span>
              </div>
              <p className="text-[11px] text-slate-300 dark:text-slate-300 text-slate-600">
                Tus clientes podrán transferirte directamente a esta cuenta o escanear tu QR al seleccionar pagos digitales.
              </p>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {/* Entidad Bancaria */}
                <div>
                  <label className="block text-[10px] font-bold uppercase tracking-wider text-slate-300 dark:text-slate-300 text-slate-700 mb-1">
                    Banco / Cooperativa *
                  </label>
                  <select
                    value={bankName}
                    onChange={(e) => setBankName(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl bg-slate-800/90 dark:bg-slate-800/90 bg-white border border-amber-500/40 text-xs text-white dark:text-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-amber-500"
                  >
                    <option value="Banco Pichincha">Banco Pichincha (Pichincha Mi Vecino)</option>
                    <option value="Banco Guayaquil">Banco Guayaquil (Banco del Barrio)</option>
                    <option value="Produbanco">Produbanco / Grupo Promerica</option>
                    <option value="Banco del Pacífico">Banco del Pacífico</option>
                    <option value="Cooperativa Tulcán">Cooperativa Tulcán</option>
                    <option value="Cooperativa Pablo Muñoz Vega">Cooperativa Pablo Muñoz Vega</option>
                    <option value="Banco Bolivariano">Banco Bolivariano</option>
                  </select>
                </div>

                {/* Tipo de Cuenta */}
                <div>
                  <label className="block text-[10px] font-bold uppercase tracking-wider text-slate-300 dark:text-slate-300 text-slate-700 mb-1">
                    Tipo de Cuenta *
                  </label>
                  <select
                    value={accountType}
                    onChange={(e) => setAccountType(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl bg-slate-800/90 dark:bg-slate-800/90 bg-white border border-amber-500/40 text-xs text-white dark:text-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-amber-500"
                  >
                    <option value="Ahorros">Cuenta de Ahorros</option>
                    <option value="Corriente">Cuenta Corriente</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {/* Número de Cuenta */}
                <div>
                  <label className="block text-[10px] font-bold uppercase tracking-wider text-slate-300 dark:text-slate-300 text-slate-700 mb-1">
                    Número de Cuenta Bancaria *
                  </label>
                  <input
                    type="text"
                    required
                    value={accountNumber}
                    onChange={(e) => setAccountNumber(e.target.value)}
                    placeholder="ej. 2201928374"
                    className="w-full px-3 py-2 rounded-xl bg-slate-800/90 dark:bg-slate-800/90 bg-white border border-amber-500/40 text-xs text-white dark:text-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-amber-500"
                  />
                </div>

                {/* Cédula o RUC */}
                <div>
                  <label className="block text-[10px] font-bold uppercase tracking-wider text-slate-300 dark:text-slate-300 text-slate-700 mb-1">
                    Cédula / RUC del Titular *
                  </label>
                  <input
                    type="text"
                    required
                    value={accountHolderId}
                    onChange={(e) => setAccountHolderId(e.target.value)}
                    placeholder="ej. 0401234567 o 0491500042001"
                    className="w-full px-3 py-2 rounded-xl bg-slate-800/90 dark:bg-slate-800/90 bg-white border border-amber-500/40 text-xs text-white dark:text-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-amber-500"
                  />
                </div>
              </div>

              {/* Nombre del Titular */}
              <div>
                <label className="block text-[10px] font-bold uppercase tracking-wider text-slate-300 dark:text-slate-300 text-slate-700 mb-1">
                  Nombre Completo del Titular
                </label>
                <input
                  type="text"
                  value={accountHolderName}
                  onChange={(e) => setAccountHolderName(e.target.value)}
                  placeholder={fullName ? `ej. ${fullName}` : 'ej. Patricio Benavides Cárdenas'}
                  className="w-full px-3 py-2 rounded-xl bg-slate-800/90 dark:bg-slate-800/90 bg-white border border-amber-500/40 text-xs text-white dark:text-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>

              {/* URL de Código QR de Cobro (DeUna / Pichincha) */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-[10px] font-bold uppercase tracking-wider text-slate-300 dark:text-slate-300 text-slate-700">
                    Código QR DeUna / Pichincha (Opcional)
                  </label>
                  <button
                    type="button"
                    onClick={() => setQrCodeUrl('https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=DEUNA_TULCAN_PAGO_2201928374')}
                    className="text-[10px] text-amber-400 hover:text-amber-300 font-bold underline"
                  >
                    ⚡ Usar QR Demo Tulcán
                  </button>
                </div>
                <input
                  type="url"
                  value={qrCodeUrl}
                  onChange={(e) => setQrCodeUrl(e.target.value)}
                  placeholder="https://... URL imagen de código QR de cobro"
                  className="w-full px-3 py-2 rounded-xl bg-slate-800/90 dark:bg-slate-800/90 bg-white border border-amber-500/40 text-xs text-white dark:text-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
                {qrCodeUrl && (
                  <div className="mt-2 p-2 bg-slate-950/60 rounded-xl flex items-center gap-3">
                    <img src={qrCodeUrl} alt="Vista previa QR" className="w-12 h-12 object-contain rounded-lg border border-amber-400 bg-white p-0.5" />
                    <span className="text-[11px] text-emerald-400 font-semibold">✓ QR listo para escaneo en el checkout</span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Contraseña */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-300 dark:text-slate-300 text-slate-700 mb-1">
                Contraseña
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Mínimo 6 carácteres"
                  disabled={isLoading}
                  className="w-full px-3.5 py-2.5 rounded-xl bg-slate-800/80 dark:bg-slate-800/80 bg-slate-50 border border-slate-700/80 dark:border-slate-700/80 border-slate-300 text-sm text-white dark:text-white text-slate-900 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-300 dark:text-slate-300 text-slate-700 mb-1">
                Confirmar Clave
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Repetir clave"
                  disabled={isLoading}
                  className="w-full px-3.5 py-2.5 rounded-xl bg-slate-800/80 dark:bg-slate-800/80 bg-slate-50 border border-slate-700/80 dark:border-slate-700/80 border-slate-300 text-sm text-white dark:text-white text-slate-900 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>
            </div>
          </div>

          <div className="flex items-center justify-between text-xs text-slate-400 dark:text-slate-400 text-slate-500 pt-1">
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="text-amber-400 dark:text-amber-400 text-amber-600 hover:underline font-semibold"
            >
              {showPassword ? 'Ocultar contraseñas' : 'Mostrar contraseñas'}
            </button>
            <span>Tulcán, Carchi ($ USD)</span>
          </div>

          {/* Botón de Enviar */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full mt-3 py-3.5 px-4 rounded-xl text-sm font-black text-slate-950 bg-gradient-to-r from-amber-400 via-amber-500 to-emerald-400 hover:from-amber-300 hover:to-emerald-300 transition-all shadow-lg shadow-amber-500/25 active:scale-[0.99] flex items-center justify-center gap-2 cursor-pointer disabled:opacity-60"
          >
            {isLoading ? (
              <>
                <div className="w-4 h-4 border-2 border-slate-950 border-t-transparent rounded-full animate-spin"></div>
                <span>Registrando en Spring Boot...</span>
              </>
            ) : (
              <>
                <span>Registrarme como {role}</span>
                <span>➔</span>
              </>
            )}
          </button>
        </form>

        {/* Footer */}
        <div className="mt-5 text-center text-xs text-slate-400 dark:text-slate-400 text-slate-600">
          ¿Ya tienes una cuenta operativa?{' '}
          <button
            type="button"
            onClick={onClose}
            className="font-bold text-amber-400 dark:text-amber-400 text-amber-600 hover:underline"
          >
            Inicia sesión aquí
          </button>
        </div>

      </div>
    </div>
  );
}
