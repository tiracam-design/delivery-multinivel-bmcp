// ═══════════════════════════════════════════════════════════════════════
// Delivery Multinivel BMCP — Tulcán, Ecuador
// Fondo de Mapa Geográfico Interactivo con Efecto Parallax y Nodos GPS
// ═══════════════════════════════════════════════════════════════════════

import React, { useState, useEffect, useRef } from 'react';

export default function InteractiveMapBackground({ activeField = null }) {
  // Posición del cursor normalizada (-1 a 1) para efecto Parallax
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });
  const [smoothedPos, setSmoothedPos] = useState({ x: 0, y: 0 });
  const [hoveredNode, setHoveredNode] = useState(null);
  const animFrameRef = useRef(null);
  const targetPosRef = useRef({ x: 0, y: 0 });
  const currentPosRef = useRef({ x: 0, y: 0 });

  // Nodos geográficos en ruta activa en Tulcán, Carchi
  const activeUnits = [
    {
      id: 'unit-1',
      name: 'Unidad 01 • Carlos Mendoza',
      role: 'Repartidor',
      vehicle: '🛵 Moto Yamaha 125',
      location: 'Parque de la Independencia (Centro)',
      coords: '0.8116° N, -77.7172° W',
      status: 'En ruta con pedido #ORD-8491',
      speed: '34 km/h',
      color: '#10b981', // Emerald
      badgeBg: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
      x: 740,
      y: 440
    },
    {
      id: 'unit-2',
      name: 'Unidad 02 • Valeria / Entrega Express',
      role: 'Cliente',
      vehicle: '🏠 Domicilio Calle Bolívar',
      location: 'Sector Mercado Central Tulcán',
      coords: '0.8130° N, -77.7150° W',
      status: 'Esperando arribo de orden (PIN: 8492)',
      speed: '0 km/h',
      color: '#f59e0b', // Amber
      badgeBg: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
      x: 980,
      y: 360
    },
    {
      id: 'unit-3',
      name: 'Unidad 03 • Móvil Panamericana E35',
      role: 'Logística',
      vehicle: '🚗 Furgón Rápido',
      location: 'Av. Veintimilla • Rumichaca',
      coords: '0.8175° N, -77.7120° W',
      status: 'Transferencia de insumos alimenticios',
      speed: '48 km/h',
      color: '#06b6d4', // Cyan
      badgeBg: 'bg-cyan-500/20 text-cyan-400 border-cyan-500/30',
      x: 380,
      y: 280
    },
    {
      id: 'hub-gad',
      name: 'Hub Central GAD Municipal Tulcán',
      role: 'Centro de Despacho',
      vehicle: '🏢 Servidor PostGIS + Redis',
      location: 'Calle 10 de Agosto y Olmedo',
      coords: '0.8122° N, -77.7165° W',
      status: 'Despacho automático y radar STOMP activo',
      speed: 'Operativo',
      color: '#818cf8', // Indigo
      badgeBg: 'bg-indigo-500/20 text-indigo-400 border-indigo-500/30',
      x: 620,
      y: 560
    }
  ];

  // Escuchar movimiento del mouse para Parallax suave
  useEffect(() => {
    const handleMouseMove = (e) => {
      const { innerWidth, innerHeight } = window;
      // Normalizar de -1 a 1 desde el centro de la pantalla
      const nx = (e.clientX / innerWidth - 0.5) * 2;
      const ny = (e.clientY / innerHeight - 0.5) * 2;
      targetPosRef.current = { x: nx, y: ny };
    };

    window.addEventListener('mousemove', handleMouseMove);

    // Animación de interpolación (lerp) para suavidad analógica
    const loop = () => {
      const speed = 0.05;
      currentPosRef.current.x += (targetPosRef.current.x - currentPosRef.current.x) * speed;
      currentPosRef.current.y += (targetPosRef.current.y - currentPosRef.current.y) * speed;

      setSmoothedPos({
        x: currentPosRef.current.x,
        y: currentPosRef.current.y
      });

      animFrameRef.current = requestAnimationFrame(loop);
    };

    animFrameRef.current = requestAnimationFrame(loop);

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    };
  }, []);

  // Desplazamientos calculados para cada capa de Parallax
  const deepOffsetX = smoothedPos.x * 12;
  const deepOffsetY = smoothedPos.y * 12;

  const midOffsetX = smoothedPos.x * 24;
  const midOffsetY = smoothedPos.y * 24;

  const nearOffsetX = smoothedPos.x * 40;
  const nearOffsetY = smoothedPos.y * 40;

  // Resplandor ambiental de fondo según el campo activo del formulario
  const getAmbientHalo = () => {
    if (activeField === 'email') {
      return 'bg-amber-500/15 dark:bg-amber-500/20 transition-all duration-700';
    }
    if (activeField === 'password') {
      return 'bg-emerald-500/15 dark:bg-emerald-500/20 transition-all duration-700';
    }
    return 'bg-indigo-500/10 dark:bg-indigo-500/15 transition-all duration-700';
  };

  return (
    <div className="absolute inset-0 pointer-events-none select-none overflow-hidden z-0">
      
      {/* ── 1. RESPLANDOR AMBIENTAL DINÁMICO REACTIVO AL FOCO DEL FORMULARIO ── */}
      <div
        className={`absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[700px] rounded-full blur-[140px] pointer-events-none ${getAmbientHalo()}`}
      />

      {/* ── 2. CAPA PARALLAX: MAPA VECTORIAL DE TULCÁN CON RUTAS ACTIVAS ─────── */}
      <svg
        className="w-full h-full object-cover transition-transform duration-100 ease-out"
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 1440 900"
        preserveAspectRatio="xMidYMid slice"
        style={{
          transform: `translate3d(${deepOffsetX}px, ${deepOffsetY}px, 0) scale(1.04)`
        }}
      >
        <defs>
          {/* Grilla técnica urbana */}
          <pattern id="gisGrid" width="60" height="60" patternUnits="userSpaceOnUse">
            <path d="M 60 0 L 0 0 0 60" fill="none" stroke="currentColor" strokeWidth="0.5" strokeOpacity="0.18" />
            <circle cx="0" cy="0" r="1.2" fill="currentColor" opacity="0.3" />
          </pattern>

          {/* Gradiente de ruta activa */}
          <linearGradient id="routeGradient1" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#10b981" stopOpacity="0.8" />
            <stop offset="50%" stopColor="#f59e0b" stopOpacity="0.9" />
            <stop offset="100%" stopColor="#06b6d4" stopOpacity="0.8" />
          </linearGradient>

          {/* Radar Sweep con desvanecimiento angular */}
          <radialGradient id="radarBeam" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="#10b981" stopOpacity="0.3" />
            <stop offset="70%" stopColor="#10b981" stopOpacity="0.08" />
            <stop offset="100%" stopColor="#10b981" stopOpacity="0" />
          </radialGradient>
        </defs>

        {/* Grilla Geográfica Base */}
        <rect width="100%" height="100%" fill="url(#gisGrid)" />

        {/* ── 3. RED VIAL PRINCIPAL DE TULCÁN (CAPA MEDIA PARALLAX) ───────────── */}
        <g
          style={{
            transform: `translate3d(${midOffsetX - deepOffsetX}px, ${midOffsetY - deepOffsetY}px, 0)`
          }}
        >
          {/* Red Vial Secundaria */}
          <g stroke="currentColor" strokeOpacity="0.2" fill="none" strokeWidth="1.5">
            <path d="M -100 180 L 1600 240" strokeDasharray="3 3" />
            <path d="M -100 620 L 1600 680" strokeDasharray="4 4" />
            <path d="M 220 -100 L 320 1000" />
            <path d="M 1120 -100 L 1220 1000" strokeDasharray="5 5" />
          </g>

          {/* Vías Arteriales Principales */}
          <g fill="none">
            {/* Vía Panamericana E35 (Corredor Internacional Rumichaca - Tulcán) */}
            <path
              d="M -80 150 C 350 280, 680 120, 1550 480"
              stroke="#06b6d4"
              strokeWidth="3.5"
              strokeOpacity="0.45"
            />
            {/* Av. Veintimilla (Eje Comercial) */}
            <path
              d="M 280 -60 L 520 960"
              stroke="#f59e0b"
              strokeWidth="3"
              strokeOpacity="0.5"
            />
            {/* Calle Sucre (Centro Histórico) */}
            <path
              d="M -50 510 L 1500 460"
              stroke="#10b981"
              strokeWidth="2.5"
              strokeOpacity="0.45"
            />
            {/* Calle Bolívar (Sector Mercados) */}
            <path
              d="M -50 420 L 1500 370"
              stroke="#818cf8"
              strokeWidth="2.5"
              strokeOpacity="0.45"
            />
          </g>

          {/* ── 4. LÍNEAS DE RUTA VECTORIALES ACTIVAS CON FLUJO DE PUNTOS ─────── */}
          {/* Ruta en curso: Hub GAD -> Parque Independencia -> Cliente */}
          <path
            d="M 620 560 L 740 440 L 980 360"
            fill="none"
            stroke="url(#routeGradient1)"
            strokeWidth="3"
            className="animate-dashFlow"
          />

          {/* Ruta de soporte: Móvil Panamericana -> Hub GAD */}
          <path
            d="M 380 280 Q 480 440, 620 560"
            fill="none"
            stroke="#06b6d4"
            strokeWidth="2"
            strokeOpacity="0.6"
            className="animate-dashFlowFast"
          />

          {/* Radar Concéntrico en Hub Central GAD Tulcán */}
          <g transform="translate(620, 560)">
            <circle cx="0" cy="0" r="140" fill="none" stroke="#818cf8" strokeWidth="1" strokeOpacity="0.15" strokeDasharray="4 4" />
            <circle cx="0" cy="0" r="90" fill="none" stroke="#818cf8" strokeWidth="1.2" strokeOpacity="0.25" />
            <circle cx="0" cy="0" r="45" fill="none" stroke="#818cf8" strokeWidth="1.5" strokeOpacity="0.35" />
            
            {/* Haz de Radar Rotatorio */}
            <g className="animate-radarSweep origin-center">
              <path
                d="M 0 0 L 140 -20 A 140 140 0 0 1 140 20 Z"
                fill="url(#radarBeam)"
              />
              <line x1="0" y1="0" x2="140" y2="0" stroke="#10b981" strokeWidth="1.5" strokeOpacity="0.7" />
            </g>
          </g>
        </g>

        {/* ── 5. CAPA SUPERIOR: NODOS GEOGRÁFICOS ANIMADOS CON PULSOS DE LUZ ─── */}
        <g
          style={{
            transform: `translate3d(${nearOffsetX - deepOffsetX}px, ${nearOffsetY - deepOffsetY}px, 0)`
          }}
        >
          {activeUnits.map((unit) => {
            const isHovered = hoveredNode === unit.id;

            return (
              <g
                key={unit.id}
                transform={`translate(${unit.x}, ${unit.y})`}
                className="pointer-events-auto cursor-pointer group"
                onMouseEnter={() => setHoveredNode(unit.id)}
                onMouseLeave={() => setHoveredNode(null)}
              >
                {/* 1. Halo Pulsante Exterior */}
                <circle
                  cx="0"
                  cy="0"
                  r="24"
                  fill="none"
                  stroke={unit.color}
                  strokeWidth="1.5"
                  className="animate-ping origin-center opacity-60"
                />

                {/* 2. Onda Intermedia */}
                <circle
                  cx="0"
                  cy="0"
                  r="14"
                  fill={unit.color}
                  fillOpacity="0.25"
                  className="transition-all duration-300 group-hover:scale-125"
                />

                {/* 3. Núcleo Brillante */}
                <circle
                  cx="0"
                  cy="0"
                  r={isHovered ? "8" : "6"}
                  fill={unit.color}
                  className="transition-all duration-300 drop-shadow-md"
                />

                {/* 4. Etiqueta de Telemetría Dinámica */}
                <foreignObject
                  x="-90"
                  y="16"
                  width="180"
                  height="75"
                  className="overflow-visible"
                >
                  <div
                    className={`transition-all duration-300 transform ${
                      isHovered
                        ? 'scale-105 opacity-100 -translate-y-1'
                        : 'scale-95 opacity-85 hover:opacity-100'
                    }`}
                  >
                    <div className="bg-slate-900/90 dark:bg-slate-900/90 bg-white/95 backdrop-blur-md p-1.5 px-2.5 rounded-xl border border-slate-700/80 dark:border-slate-700/80 border-slate-200 shadow-xl text-[10px] leading-tight text-center">
                      <div className="flex items-center justify-center gap-1 font-extrabold text-slate-800 dark:text-white truncate">
                        <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: unit.color }} />
                        <span className="truncate">{unit.name.split('•')[0]}</span>
                      </div>
                      <div className="text-[9px] text-slate-400 dark:text-slate-400 font-mono mt-0.5 truncate">
                        {unit.coords} • <strong className="text-amber-400 font-bold">{unit.speed}</strong>
                      </div>
                    </div>
                  </div>
                </foreignObject>
              </g>
            );
          })}
        </g>
      </svg>

      {/* ── 6. TELEMETRÍA DE ZONA OPERATIVA EN ESQUINA INFERIOR IZQUIERDA ────── */}
      <div className="absolute bottom-5 left-5 z-10 hidden sm:flex flex-col gap-1 text-[11px] font-mono text-slate-400/80 bg-slate-900/60 dark:bg-slate-900/60 bg-white/70 backdrop-blur-md px-3.5 py-2.5 rounded-2xl border border-slate-700/50 dark:border-slate-800 border-slate-300/60 shadow-lg">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
          <span className="font-extrabold text-slate-200 dark:text-slate-200 text-slate-800 tracking-wider">
            ZONA LOGÍSTICA TULCÁN
          </span>
        </div>
        <div className="text-[10px] text-slate-400">
          Coords: <span className="text-sky-400">0.8116° N, -77.7172° W</span> • Elev: <span className="text-amber-400">2,980m</span>
        </div>
        <div className="text-[10px] text-emerald-400/90 flex items-center gap-1.5">
          <span>📡 4 Unidades en Enlace PostGIS</span>
          <span>•</span>
          <span>Redis TTL 15s</span>
        </div>
      </div>

    </div>
  );
}
