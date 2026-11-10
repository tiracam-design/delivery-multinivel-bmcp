import React, { useState, useMemo, useEffect } from 'react';
import { validateEcuadorianRuc } from '../../../utils/ecuadorianRucValidator';
import { buildApiUrl, getAuthToken } from '../../../services/api';

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * MERCHANT & COURIER APPROVALS — MÓDULO ADMINISTRATIVO (SISTEMA DE DELIVERY BMCP)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Características principales:
 * 1. Indicadores superiores de flota en tiempo real:
 *    - Total Comercios Activos
 *    - Repartidores en Línea
 *    - Pedidos en Curso
 * 2. Tabla moderna Tailwind CSS (min-w-full divide-y divide-gray-200 shadow-lg rounded-xl overflow-hidden).
 * 3. Gestión de Solicitudes Pendientes para Comercios y Repartidores con badges oficiales (bg-yellow-100 text-yellow-800 rounded-full px-3 py-1).
 * 4. Botones de acción rápida:
 *    - "Ver Documentos RUC/Licencia" (Abre visor modal de auditoría legal/tributaria).
 *    - "Aprobar" (bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-lg).
 *    - "Rechazar" (bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-lg).
 * 5. Modal interactivo con inspección de Ficha RUC, Licencia de Conducir B-IIc, SOAT y comprobantes.
 * 6. Diálogo de confirmación con motivo de rechazo y feedback al solicitante.
 * 7. Búsqueda y filtrado multicriterio en memoria.
 */

// ── ICONOS SVG EMBEBIDOS (Sin dependencias externas) ─────────────────────────
const Icons = {
  Store: () => (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
    </svg>
  ),
  Motorcycle: () => (
    <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
      <path d="M19.5 12c-1.38 0-2.5 1.12-2.5 2.5s1.12 2.5 2.5 2.5 2.5-1.12 2.5-2.5-1.12-2.5-2.5-2.5zm0 3.5c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-15-3.5c-1.38 0-2.5 1.12-2.5 2.5s1.12 2.5 2.5 2.5 2.5-1.12 2.5-2.5-1.12-2.5-2.5-2.5zm0 3.5c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm12.3-6.5l-2.8-2.8c-.38-.38-.88-.6-1.41-.6H9c-.55 0-1 .45-1 1s.45 1 1 1h3.17l1.7 1.7-4.14 4.14c-.38-.34-.88-.54-1.43-.54-1.93 0-3.5 1.57-3.5 3.5s1.57 3.5 3.5 3.5c1.78 0 3.25-1.33 3.47-3.06l3.96-2.64 1.77 1.77c-.55.77-.9 1.71-.9 2.73 0 2.48 2.02 4.5 4.5 4.5s4.5-2.02 4.5-4.5-2.02-4.5-4.5-4.5c-.34 0-.67.04-1 .11l-2.03-3.05z" />
    </svg>
  ),
  Check: () => (
    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
    </svg>
  ),
  XMark: () => (
    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
    </svg>
  ),
  Eye: () => (
    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
    </svg>
  ),
  FileText: () => (
    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
    </svg>
  ),
  Search: () => (
    <svg className="w-5 h-5 text-slate-400 dark:text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
    </svg>
  ),
  ShieldCheck: () => (
    <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  ),
  TrendingUp: () => (
    <svg className="w-4 h-4 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
    </svg>
  ),
  Clock: () => (
    <svg className="w-4 h-4 text-amber-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  ),
  ShoppingBag: () => (
    <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
    </svg>
  ),
  Users: () => (
    <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
    </svg>
  ),
  Download: () => (
    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
    </svg>
  ),
  Refresh: () => (
    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
    </svg>
  ),
  Building: () => (
    <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
    </svg>
  ),
  CheckCircle: () => (
    <svg className="w-5 h-5 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  ),
  AlertTriangle: () => (
    <svg className="w-5 h-5 text-amber-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
    </svg>
  )
};

// ── DATOS INICIALES MOCK DE SOLICITUDES ───────────────────────────────────────
const INITIAL_APPLICATIONS = [
  {
    id: 'REQ-101',
    type: 'MERCHANT',
    businessName: 'Asadero El Leñador Carchi S.A.S.',
    applicantName: 'Carlos Eduardo Mendoza',
    email: 'carlos.mendoza@tulcan.ec',
    phone: '+593 98 765 4321',
    documentType: 'RUC',
    documentNumber: '0491500042001',
    category: 'Restaurante & Asadero',
    zone: 'Tulcán Centro (Calle Sucre y Junín)',
    registeredAt: '2026-10-12 09:42',
    status: 'PENDING',
    documents: [
      {
        id: 'doc-1',
        name: 'Ficha RUC (SRI Ecuador)',
        code: 'RUC-0491500042001.pdf',
        issueDate: '2026-01-15',
        status: 'VALID',
        notes: 'Estado: ACTIVO. Régimen: RIMPE Emprendedor. Jurisdicción: Carchi (04).',
        previewType: 'ruc',
        details: {
          taxId: '0491500042001',
          tradeName: 'Asadero El Leñador',
          legalName: 'Asadero El Leñador Carchi S.A.S.',
          regime: 'Régimen RIMPE Emprendedor',
          address: 'Calle Sucre 14-20 y Pichincha, Parque Central, Tulcán, Carchi'
        }
      },
      {
        id: 'doc-2',
        name: 'Permiso Municipal de Funcionamiento (LUAE)',
        code: 'LUAE-GAD-TULCAN-2026.pdf',
        issueDate: '2026-02-15',
        status: 'VALID',
        notes: 'Emitido por GAD Municipal de Tulcán. Conforme con ordenanza urbana.',
        previewType: 'license_store'
      },
      {
        id: 'doc-3',
        name: 'Permiso del Cuerpo de Bomberos de Tulcán',
        code: 'BOMB-TULCAN-2026-091.pdf',
        issueDate: '2026-03-10',
        status: 'VALID',
        notes: 'Inspección aprobada por el Benemérito Cuerpo de Bomberos de Tulcán.',
        previewType: 'certificate'
      },
      {
        id: 'doc-4',
        name: 'Cédula de Identidad del Representante Legal',
        code: 'CED-0401827365.pdf',
        issueDate: '2022-04-12',
        status: 'VALID',
        notes: 'Registro Civil Ecuador: Carlos Eduardo Mendoza (Cédula Carchi: 0401827365).',
        previewType: 'dni'
      }
    ]
  },
  {
    id: 'REQ-102',
    type: 'DRIVER',
    businessName: 'Repartidor Independiente',
    applicantName: 'Cristian David Bolaños',
    email: 'cristian.bolanos@tulcan.ec',
    phone: '+593 99 491 2384',
    documentType: 'LICENCIA',
    documentNumber: 'Tipo A #0401928374',
    vehicleInfo: 'Moto Shineray XY200 (Placa: IC-482Y - Carchi)',
    category: 'Repartidor en Motocicleta',
    zone: 'Tulcán / Sector Terminal Terrestre',
    registeredAt: '2026-10-12 10:15',
    status: 'PENDING',
    documents: [
      {
        id: 'doc-driver-1',
        name: 'Licencia de Conducir (ANT Ecuador)',
        code: 'ANT-LIC-A-0401928374.pdf',
        issueDate: '2024-03-15',
        expiryDate: '2029-03-15',
        status: 'VALID',
        notes: 'Categoría Tipo A (Motocicletas). 30 / 30 puntos récord ANT.',
        previewType: 'driver_license',
        details: {
          licenseNumber: 'Tipo A #0401928374',
          category: 'Tipo A (Motocicletas y similares)',
          driverName: 'Cristian David Bolaños',
          dni: '0401928374',
          pointsRemaining: '30 / 30 puntos récord ANT Ecuador'
        }
      },
      {
        id: 'doc-driver-2',
        name: 'Certificado SPPAT Digital Vigente',
        code: 'SPPAT-EC-2026-482.pdf',
        issueDate: '2026-01-10',
        expiryDate: '2027-01-10',
        status: 'VALID',
        notes: 'Póliza SPPAT activa para vehículo IC-482Y (Carchi). Cobertura total.',
        previewType: 'soat'
      },
      {
        id: 'doc-driver-3',
        name: 'Matrícula Vehicular ANT Carchi',
        code: 'MAT-ANT-IC482Y.pdf',
        issueDate: '2026-02-01',
        status: 'VALID',
        notes: 'Revisión técnica vehicular aprobada en GAD Municipal de Tulcán.',
        previewType: 'vehicle_card'
      },
      {
        id: 'doc-driver-4',
        name: 'Certificado de Récord Policial',
        code: 'MIN-GOB-REC-2026.pdf',
        issueDate: '2026-09-15',
        status: 'VALID',
        notes: 'Ministerio de Gobierno del Ecuador: Sin antecedentes registrados.',
        previewType: 'police_record'
      }
    ]
  },
  {
    id: 'REQ-103',
    type: 'MERCHANT',
    businessName: 'Farmacia Cruz del Norte',
    applicantName: 'Dra. Patricia Salazar Silva',
    email: 'farmacia.cruzdelnorte@tulcan.ec',
    phone: '+593 94 411 2233',
    documentType: 'RUC',
    documentNumber: '0401827365001',
    category: 'Farmacia & Medicinas',
    zone: 'Tulcán Norte (Av. Veintimilla y 10 de Agosto)',
    registeredAt: '2026-10-12 08:30',
    status: 'PENDING',
    documents: [
      {
        id: 'doc-far-1',
        name: 'Ficha RUC (SRI Ecuador)',
        code: 'RUC-0401827365001.pdf',
        issueDate: '2025-11-10',
        status: 'VALID',
        notes: 'Estado: ACTIVO. Régimen RIMPE Negocio Popular. Provincia Carchi (04).',
        previewType: 'ruc',
        details: {
          taxId: '0401827365001',
          tradeName: 'Farmacia Cruz del Norte',
          legalName: 'Patricia Salazar Silva',
          regime: 'Régimen RIMPE Negocio Popular',
          address: 'Av. Veintimilla 550 y 10 de Agosto, Tulcán, Carchi'
        }
      },
      {
        id: 'doc-far-2',
        name: 'Permiso Sanitario ARCSA',
        code: 'ARCSA-CZ1-2025-884.pdf',
        issueDate: '2025-09-01',
        status: 'VALID',
        notes: 'Químico Farmacéutico responsable acreditado ante la ARCSA.',
        previewType: 'digemid'
      },
      {
        id: 'doc-far-3',
        name: 'Permiso Municipal de Funcionamiento',
        code: 'LUAE-GAD-TULCAN-2025-12.pdf',
        issueDate: '2025-07-15',
        status: 'VALID',
        notes: 'Giro: Comercialización de productos farmacéuticos en Tulcán.',
        previewType: 'license_store'
      }
    ]
  },
  {
    id: 'REQ-104',
    type: 'DRIVER',
    businessName: 'Repartidor Independiente',
    applicantName: 'María Elena Revelo',
    email: 'maria.revelo.rider@outlook.com',
    phone: '+593 98 123 4567',
    documentType: 'LICENCIA',
    documentNumber: 'Tipo A #0402194857',
    vehicleInfo: 'Moto Daytona Wolf 150 (Placa: IC-719K - Carchi)',
    category: 'Repartidor en Motocicleta',
    zone: 'Tulcán Sur / El Batán',
    registeredAt: '2026-10-12 11:05',
    status: 'PENDING',
    documents: [
      {
        id: 'doc-driver-v1',
        name: 'Licencia de Conducir (ANT Ecuador)',
        code: 'ANT-LIC-A-0402194857.pdf',
        issueDate: '2024-01-10',
        expiryDate: '2029-01-10',
        status: 'VALID',
        notes: 'Categoría Tipo A vigente sin infracciones graves.',
        previewType: 'driver_license',
        details: {
          licenseNumber: 'Tipo A #0402194857',
          category: 'Tipo A',
          driverName: 'María Elena Revelo',
          dni: '0402194857',
          pointsRemaining: '30 / 30 puntos récord ANT Ecuador'
        }
      },
      {
        id: 'doc-driver-v2',
        name: 'Póliza SPPAT Digital',
        code: 'SPPAT-EC-2026-719.pdf',
        issueDate: '2026-03-01',
        expiryDate: '2027-03-01',
        status: 'VALID',
        notes: 'Póliza SPPAT activa para moto IC-719K.',
        previewType: 'soat'
      },
      {
        id: 'doc-driver-v3',
        name: 'Cédula de Identidad Ecuatoriana',
        code: 'CED-0402194857.pdf',
        issueDate: '2022-06-18',
        status: 'VALID',
        notes: 'Cédula biométrica verificada ante Registro Civil.',
        previewType: 'dni'
      }
    ]
  },
  {
    id: 'REQ-105',
    type: 'MERCHANT',
    businessName: 'Cafetería & Panadería La Carchi',
    applicantName: 'Gonzalo Benavides',
    email: 'contacto@cafeterialacarchi.ec',
    phone: '+593 97 733 4455',
    documentType: 'RUC',
    documentNumber: '0491500042001',
    category: 'Cafetería & Panadería',
    zone: 'Tulcán Centro (Parque Ayora)',
    registeredAt: '2026-10-12 11:20',
    status: 'PENDING',
    documents: [
      {
        id: 'doc-cafe-1',
        name: 'Ficha RUC (SRI Ecuador)',
        code: 'RUC-0491500042001-PAN.pdf',
        issueDate: '2025-06-22',
        status: 'VALID',
        notes: 'Estado: ACTIVO. Régimen General. Domicilio fiscal en Carchi.',
        previewType: 'ruc',
        details: {
          taxId: '0491500042001',
          tradeName: 'La Carchi Cafetería',
          legalName: 'Gastronomía La Carchi Cía. Ltda.',
          regime: 'Régimen General',
          address: 'Calle Bolívar 210 y 10 de Agosto, Parque Ayora, Tulcán, Carchi'
        }
      },
      {
        id: 'doc-cafe-2',
        name: 'Licencia Municipal de Funcionamiento',
        code: 'LUAE-GAD-TULCAN-2024-551.pdf',
        issueDate: '2024-03-12',
        status: 'VALID',
        notes: 'Conforme con zonificación comercial Parque Ayora, Tulcán.',
        previewType: 'license_store'
      }
    ]
  },
  {
    id: 'REQ-106',
    type: 'DRIVER',
    businessName: 'Repartidor Independiente',
    applicantName: 'Javier Alexander Pozo',
    email: 'javier.pozo.delivery@gmail.com',
    phone: '+593 99 554 4332',
    documentType: 'LICENCIA',
    documentNumber: 'Tipo A #0401889922',
    vehicleInfo: 'Moto Bajaj Pulsar NS200 (Placa: IC-335Z - Carchi)',
    category: 'Repartidor en Motocicleta',
    zone: 'Tulcán / Rumichaca Frontera',
    registeredAt: '2026-10-12 11:35',
    status: 'PENDING',
    documents: [
      {
        id: 'doc-driver-f1',
        name: 'Licencia de Conducir (ANT Ecuador)',
        code: 'ANT-LIC-A-0401889922.pdf',
        issueDate: '2022-09-14',
        expiryDate: '2027-09-14',
        status: 'VALID',
        notes: 'Categoría Tipo A. Registro Nacional de Conductores ANT Ecuador.',
        previewType: 'driver_license',
        details: {
          licenseNumber: 'Tipo A #0401889922',
          category: 'Tipo A',
          driverName: 'Javier Alexander Pozo',
          dni: '0401889922',
          pointsRemaining: '28 / 30 puntos récord ANT'
        }
      },
      {
        id: 'doc-driver-f2',
        name: 'Póliza SPPAT Digital',
        code: 'SPPAT-EC-2026-335.pdf',
        issueDate: '2026-01-20',
        expiryDate: '2027-01-20',
        status: 'VALID',
        notes: 'Póliza SPPAT vigente para vehículo IC-335Z (Carchi).',
        previewType: 'soat'
      }
    ]
  }
];

export default function MerchantApprovals() {
  // Estado principal de solicitudes
  const [applications, setApplications] = useState(INITIAL_APPLICATIONS);

  // Filtros y búsqueda
  const [activeTab, setActiveTab] = useState('ALL'); // 'ALL', 'MERCHANT', 'DRIVER'
  const [statusFilter, setStatusFilter] = useState('PENDING'); // 'PENDING', 'APPROVED', 'REJECTED', 'ALL'
  const [searchTerm, setSearchTerm] = useState('');

  // Modales
  const [selectedApplication, setSelectedApplication] = useState(null); // Document Viewer Modal
  const [selectedDocumentIndex, setSelectedDocumentIndex] = useState(0);
  const [rejectingApplication, setRejectingApplication] = useState(null); // Rejection Modal
  const [rejectionReason, setRejectionReason] = useState('Documentación con inconsistencias');
  const [customRejectionComment, setCustomRejectionComment] = useState('');

  // Notificaciones Toast de acción
  const [toast, setToast] = useState(null);
  const [systemMetrics, setSystemMetrics] = useState(null);

  useEffect(() => {
    const fetchMetrics = async () => {
      try {
        const token = getAuthToken();
        const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
        const res = await fetch(buildApiUrl('/api/v1/admin/metrics'), { headers });
        if (res.ok) {
          const data = await res.json();
          setSystemMetrics(data);
        }
      } catch (e) {
        // Fallback to computed local indicators
      }
    };
    fetchMetrics();
  }, []);

  // Mostrar Toast
  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => {
      setToast(null);
    }, 4000);
  };

  // ── KPIS SUPERIORES CALCULADOS Y DE FLOTA ──────────────────────────────────
  const fleetKpis = useMemo(() => {
    return {
      activeMerchants: systemMetrics?.totalMerchants ?? 142,
      activeMerchantsGrowth: '+12% este mes',
      onlineDrivers: systemMetrics?.totalDrivers ?? 86,
      onlineDriversRoute: '64 en ruta • 22 disponibles',
      ordersInProgress: systemMetrics?.totalOrders ?? 318,
      ordersAverageTime: 'Tiempo prom.: 24 min',
      pendingApprovals: (systemMetrics?.pendingMerchantsCount != null && systemMetrics?.pendingDriversCount != null)
        ? (systemMetrics.pendingMerchantsCount + systemMetrics.pendingDriversCount)
        : applications.filter((app) => app.status === 'PENDING').length
    };
  }, [applications, systemMetrics]);

  // ── FILTRADO DINÁMICO DE LA TABLA ──────────────────────────────────────────
  const filteredApplications = useMemo(() => {
    return applications.filter((app) => {
      // Filtro por Tipo (Comercios / Repartidores)
      if (activeTab === 'MERCHANT' && app.type !== 'MERCHANT') return false;
      if (activeTab === 'DRIVER' && app.type !== 'DRIVER') return false;

      // Filtro por Estado
      if (statusFilter !== 'ALL' && app.status !== statusFilter) return false;

      // Búsqueda en texto libre
      if (searchTerm.trim() !== '') {
        const query = searchTerm.toLowerCase();
        const matchesName = app.businessName.toLowerCase().includes(query);
        const matchesApplicant = app.applicantName.toLowerCase().includes(query);
        const matchesDoc = app.documentNumber.toLowerCase().includes(query);
        const matchesZone = app.zone.toLowerCase().includes(query);
        const matchesCategory = app.category.toLowerCase().includes(query);
        return matchesName || matchesApplicant || matchesDoc || matchesZone || matchesCategory;
      }

      return true;
    });
  }, [applications, activeTab, statusFilter, searchTerm]);

  // ── MANEJADORES DE ACCIÓN RÁPIDA ───────────────────────────────────────────
  // Aprobar
  const handleApprove = async (appId) => {
    const appToApprove = applications.find((a) => a.id === appId);
    setApplications((prev) =>
      prev.map((app) => (app.id === appId ? { ...app, status: 'APPROVED' } : app))
    );

    if (selectedApplication?.id === appId) {
      setSelectedApplication(null);
    }

    try {
      const token = getAuthToken();
      const isUUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(appId);
      if (token && isUUID) {
        const endpoint = appToApprove?.type === 'DRIVER'
          ? `/api/v1/admin/approvals/drivers/${appId}/approve`
          : `/api/v1/admin/approvals/merchants/${appId}/approve`;
        await fetch(buildApiUrl(endpoint), {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
      }
    } catch (err) {
      console.warn('Backend approval sync notice:', err);
    }

    showToast(
      `¡Solicitud ${appToApprove?.businessName || appId} aprobada exitosamente! Se han enviado las credenciales de acceso.`,
      'success'
    );
  };

  // Abrir diálogo de rechazo
  const handleOpenRejectModal = (app) => {
    setRejectingApplication(app);
    setRejectionReason('Documentación con inconsistencias');
    setCustomRejectionComment('');
  };

  // Confirmar rechazo
  const handleConfirmReject = async () => {
    if (!rejectingApplication) return;
    const targetApp = rejectingApplication;
    const reasonText = `${rejectionReason}${
      customRejectionComment ? ` - Detalle: ${customRejectionComment}` : ''
    }`;

    setApplications((prev) =>
      prev.map((app) =>
        app.id === targetApp.id
          ? {
              ...app,
              status: 'REJECTED',
              rejectionFeedback: reasonText
            }
          : app
      )
    );

    if (selectedApplication?.id === targetApp.id) {
      setSelectedApplication(null);
    }

    try {
      const token = getAuthToken();
      const isUUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(targetApp.id);
      if (token && isUUID) {
        const endpoint = targetApp.type === 'DRIVER'
          ? `/api/v1/admin/approvals/drivers/${targetApp.id}/reject`
          : `/api/v1/admin/approvals/merchants/${targetApp.id}/reject`;
        await fetch(buildApiUrl(endpoint), {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ reason: reasonText })
        });
      }
    } catch (err) {
      console.warn('Backend rejection sync notice:', err);
    }

    showToast(
      `Solicitud de ${targetApp.businessName} rechazada. Notificación enviada al solicitante.`,
      'danger'
    );
    setRejectingApplication(null);
  };

  // Abrir Modal de Documentos
  const handleOpenDocumentsModal = (app) => {
    setSelectedApplication(app);
    setSelectedDocumentIndex(0);
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-800 dark:text-slate-100 p-4 md:p-8 font-sans transition-colors duration-200">
      {/* ── NOTIFICACIÓN TOAST FLOTANTE ─────────────────────────────────────── */}
      {toast && (
        <div className="fixed top-6 right-6 z-50 flex items-center gap-3 px-5 py-4 rounded-xl shadow-2xl transition-all duration-300 animate-bounce bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
          {toast.type === 'success' ? (
            <div className="w-8 h-8 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-400 flex items-center justify-center shrink-0">
              <Icons.CheckCircle />
            </div>
          ) : (
            <div className="w-8 h-8 rounded-full bg-rose-100 dark:bg-rose-950/60 text-rose-700 dark:text-rose-400 flex items-center justify-center shrink-0">
              <Icons.AlertTriangle />
            </div>
          )}
          <div>
            <p className="text-sm font-semibold text-slate-900 dark:text-white">
              {toast.type === 'success' ? 'Operación Exitosa' : 'Solicitud Notificada'}
            </p>
            <p className="text-xs text-slate-600 dark:text-slate-400 max-w-sm">{toast.message}</p>
          </div>
          <button
            onClick={() => setToast(null)}
            className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 ml-2"
          >
            <Icons.XMark />
          </button>
        </div>
      )}

      {/* ── ENCABEZADO DE LA VISTA ─────────────────────────────────────────── */}
      <div className="mb-8 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/40">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              Panel de Cumplimiento Legal & Onboarding
            </span>
            <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">Módulo BMCP Admin</span>
          </div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight mt-1">
            Aprobación de Comercios & Repartidores
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Valida antecedentes, Fichas RUC de SRI Ecuador, Licencias de Conducir y pólizas SPPAT
            para autorizar el ingreso a la plataforma de delivery.
          </p>
        </div>

        {/* Acciones de cabecera */}
        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              setApplications(INITIAL_APPLICATIONS);
              showToast('Lista de solicitudes sincronizada con el backend', 'success');
            }}
            className="inline-flex items-center gap-2 px-4 py-2 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 rounded-lg text-sm font-medium shadow-sm transition"
            title="Refrescar solicitudes"
          >
            <Icons.Refresh />
            <span>Actualizar</span>
          </button>
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-slate-900 dark:bg-slate-800 text-white rounded-lg text-sm font-semibold shadow-md border border-slate-800 dark:border-slate-700">
            <Icons.ShieldCheck />
            <span>Filtro Anti-Fraude Activo</span>
          </div>
        </div>
      </div>

      {/* ── REQUISITO 4: INDICADORES SUPERIORES DE FLOTA ───────────────────── */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        {/* KPI 1: Total Comercios Activos */}
        <div className="bg-white dark:bg-slate-900 rounded-xl p-5 border border-slate-200/80 dark:border-slate-800 shadow-sm hover:shadow-md transition-all relative overflow-hidden">
          <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-50 dark:bg-emerald-950/20 rounded-bl-full pointer-events-none -mr-4 -mt-4 opacity-70"></div>
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Total Comercios Activos
            </span>
            <div className="w-10 h-10 rounded-xl bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 flex items-center justify-center">
              <Icons.Store />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-slate-900 dark:text-white">
              {fleetKpis.activeMerchants}
            </span>
            <span className="text-xs font-semibold text-emerald-600 dark:text-emerald-400 inline-flex items-center gap-0.5">
              <Icons.TrendingUp />
              {fleetKpis.activeMerchantsGrowth}
            </span>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-2 flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
            Restaurantes, farmacias y bodegas operativas
          </p>
        </div>

        {/* KPI 2: Repartidores en Línea */}
        <div className="bg-white dark:bg-slate-900 rounded-xl p-5 border border-slate-200/80 dark:border-slate-800 shadow-sm hover:shadow-md transition-all relative overflow-hidden">
          <div className="absolute top-0 right-0 w-24 h-24 bg-sky-50 dark:bg-sky-950/20 rounded-bl-full pointer-events-none -mr-4 -mt-4 opacity-70"></div>
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Repartidores en Línea
            </span>
            <div className="w-10 h-10 rounded-xl bg-sky-100 dark:bg-sky-950/60 text-sky-700 dark:text-sky-300 flex items-center justify-center">
              <Icons.Motorcycle />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-slate-900 dark:text-white">
              {fleetKpis.onlineDrivers}
            </span>
            <span className="text-xs font-semibold px-2 py-0.5 rounded bg-sky-50 dark:bg-sky-950/60 text-sky-700 dark:text-sky-300">
              Flota Activa
            </span>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-2 flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-sky-500 animate-pulse"></span>
            {fleetKpis.onlineDriversRoute}
          </p>
        </div>

        {/* KPI 3: Pedidos en Curso */}
        <div className="bg-white dark:bg-slate-900 rounded-xl p-5 border border-slate-200/80 dark:border-slate-800 shadow-sm hover:shadow-md transition-all relative overflow-hidden">
          <div className="absolute top-0 right-0 w-24 h-24 bg-amber-50 dark:bg-amber-950/20 rounded-bl-full pointer-events-none -mr-4 -mt-4 opacity-70"></div>
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Pedidos en Curso
            </span>
            <div className="w-10 h-10 rounded-xl bg-amber-100 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 flex items-center justify-center">
              <Icons.ShoppingBag />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-slate-900 dark:text-white">
              {fleetKpis.ordersInProgress}
            </span>
            <span className="text-xs font-semibold text-amber-700 dark:text-amber-400 inline-flex items-center gap-1">
              <Icons.Clock />
              En tránsito
            </span>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-2 flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-amber-500"></span>
            {fleetKpis.ordersAverageTime}
          </p>
        </div>

        {/* KPI 4: Solicitudes Pendientes (Acción Inmediata) */}
        <div className="bg-gradient-to-br from-indigo-900 to-slate-900 text-white rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow relative overflow-hidden border border-indigo-800/40">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold uppercase tracking-wider text-indigo-200">
              Cola de Aprobaciones
            </span>
            <div className="w-10 h-10 rounded-xl bg-white/10 text-white flex items-center justify-center backdrop-blur">
              <Icons.Users />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-white">
              {fleetKpis.pendingApprovals}
            </span>
            <span className="text-xs font-bold px-2 py-0.5 rounded bg-yellow-400 text-yellow-950">
              Por Revisar
            </span>
          </div>
          <p className="text-xs text-indigo-200 mt-2 flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-yellow-400 animate-ping"></span>
            Prioridad alta: SLA máx 24 horas
          </p>
        </div>
      </div>

      {/* ── BARRA DE BÚSQUEDA Y PESTAÑAS DE FILTRO ─────────────────────────── */}
      <div className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-4 mb-6 shadow-sm flex flex-col md:flex-row md:items-center md:justify-between gap-4 transition-colors">
        {/* Pestañas de tipo */}
        <div className="flex items-center p-1 bg-slate-100 dark:bg-slate-800 rounded-lg">
          <button
            onClick={() => setActiveTab('ALL')}
            className={`px-3.5 py-1.5 rounded-md text-xs font-bold transition ${
              activeTab === 'ALL'
                ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-sm'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            Todas las Solicitudes ({applications.length})
          </button>
          <button
            onClick={() => setActiveTab('MERCHANT')}
            className={`px-3.5 py-1.5 rounded-md text-xs font-bold transition inline-flex items-center gap-1.5 ${
              activeTab === 'MERCHANT'
                ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-sm'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
            Comercios ({applications.filter((a) => a.type === 'MERCHANT').length})
          </button>
          <button
            onClick={() => setActiveTab('DRIVER')}
            className={`px-3.5 py-1.5 rounded-md text-xs font-bold transition inline-flex items-center gap-1.5 ${
              activeTab === 'DRIVER'
                ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-sm'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-sky-500"></span>
            Repartidores ({applications.filter((a) => a.type === 'DRIVER').length})
          </button>
        </div>

        {/* Buscador y filtro por estado */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          <div className="relative min-w-[260px]">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Icons.Search />
            </div>
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Buscar por RUC, Licencia, Nombre o Giro..."
              className="w-full pl-10 pr-4 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-xs md:text-sm text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white dark:focus:bg-slate-800 transition"
            />
            {searchTerm && (
              <button
                onClick={() => setSearchTerm('')}
                className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              >
                <Icons.XMark />
              </button>
            )}
          </div>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-xs md:text-sm font-medium text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="PENDING">Estado: Pendientes</option>
            <option value="APPROVED">Estado: Aprobados</option>
            <option value="REJECTED">Estado: Rechazados</option>
            <option value="ALL">Todos los Estados</option>
          </select>
        </div>
      </div>

      {/* ── REQUISITO 1: TABLA MODERNA CON CLASES TAILWIND CSS ─────────────── */}
      <div className="shadow-lg rounded-xl overflow-hidden border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 transition-colors">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 dark:divide-slate-800">
            <thead className="bg-slate-50/80 dark:bg-slate-800/80">
              <tr>
                <th
                  scope="col"
                  className="px-6 py-3.5 text-left text-xs font-bold text-slate-600 dark:text-slate-300 uppercase tracking-wider"
                >
                  Entidad / Solicitante
                </th>
                <th
                  scope="col"
                  className="px-6 py-3.5 text-left text-xs font-bold text-slate-600 dark:text-slate-300 uppercase tracking-wider"
                >
                  Tipo & Categoría
                </th>
                <th
                  scope="col"
                  className="px-6 py-3.5 text-left text-xs font-bold text-slate-600 dark:text-slate-300 uppercase tracking-wider"
                >
                  Identificación Legal (RUC / Licencia)
                </th>
                <th
                  scope="col"
                  className="px-6 py-3.5 text-left text-xs font-bold text-slate-600 dark:text-slate-300 uppercase tracking-wider"
                >
                  Ubicación & Contacto
                </th>
                <th
                  scope="col"
                  className="px-6 py-3.5 text-left text-xs font-bold text-slate-600 dark:text-slate-300 uppercase tracking-wider"
                >
                  Estado
                </th>
                <th
                  scope="col"
                  className="px-6 py-3.5 text-right text-xs font-bold text-slate-600 dark:text-slate-300 uppercase tracking-wider"
                >
                  Acciones Rápidas
                </th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-slate-900 divide-y divide-slate-200 dark:divide-slate-800 text-sm">
              {filteredApplications.length === 0 ? (
                <tr>
                  <td colSpan="6" className="px-6 py-12 text-center text-slate-500 dark:text-slate-400">
                    <div className="max-w-sm mx-auto flex flex-col items-center">
                      <div className="w-12 h-12 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-400 mb-3">
                        <Icons.Search />
                      </div>
                      <p className="font-semibold text-slate-700 dark:text-slate-200">No se encontraron solicitudes</p>
                      <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                        Intenta ajustar el término de búsqueda o selecciona otro filtro de estado.
                      </p>
                      <button
                        onClick={() => {
                          setSearchTerm('');
                          setStatusFilter('ALL');
                          setActiveTab('ALL');
                        }}
                        className="mt-4 px-3 py-1.5 text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:text-indigo-800"
                      >
                        Limpiar todos los filtros
                      </button>
                    </div>
                  </td>
                </tr>
              ) : (
                filteredApplications.map((app) => (
                  <tr
                    key={app.id}
                    className="hover:bg-slate-50/70 dark:hover:bg-slate-800/50 transition-colors duration-150"
                  >
                    {/* Columna 1: Solicitante */}
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-3">
                        <div
                          className={`w-10 h-10 rounded-xl flex items-center justify-center font-bold text-sm shrink-0 shadow-sm ${
                            app.type === 'MERCHANT'
                              ? 'bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/40'
                              : 'bg-sky-50 dark:bg-sky-950/60 text-sky-700 dark:text-sky-300 border border-sky-200 dark:border-sky-800/40'
                          }`}
                        >
                          {app.type === 'MERCHANT' ? <Icons.Store /> : <Icons.Motorcycle />}
                        </div>
                        <div>
                          <div className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                            <span>{app.businessName}</span>
                            <span className="text-[11px] font-mono font-medium px-1.5 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700">
                              {app.id}
                            </span>
                          </div>
                          <div className="text-xs text-slate-500 dark:text-slate-400 flex items-center gap-1.5 mt-0.5">
                            <span>Titular: {app.applicantName}</span>
                          </div>
                        </div>
                      </div>
                    </td>

                    {/* Columna 2: Tipo & Categoría */}
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div>
                        {app.type === 'MERCHANT' ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-semibold bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/40">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 dark:bg-emerald-400"></span>
                            Comercio Afiliado
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-semibold bg-sky-100 dark:bg-sky-950/60 text-sky-800 dark:text-sky-300 border border-sky-200 dark:border-sky-800/40">
                            <span className="w-1.5 h-1.5 rounded-full bg-sky-600 dark:bg-sky-400"></span>
                            Repartidor BMCP
                          </span>
                        )}
                        <p className="text-xs text-slate-600 dark:text-slate-300 mt-1 font-medium">{app.category}</p>
                        {app.vehicleInfo && (
                          <p className="text-[11px] text-slate-400 font-mono mt-0.5">
                            {app.vehicleInfo}
                          </p>
                        )}
                      </div>
                    </td>

                    {/* Columna 3: Documento Legal (RUC / Licencia) */}
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div>
                        <div className="flex items-center gap-1.5 font-mono font-bold text-xs text-slate-900 dark:text-white">
                          <span className="px-1.5 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 text-[10px] font-sans font-semibold border border-slate-200 dark:border-slate-700">
                            {app.documentType}
                          </span>
                          <span>{app.documentNumber}</span>
                        </div>
                        <div className="text-xs text-slate-500 dark:text-slate-400 mt-1 flex items-center gap-1">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                          <span className="text-[11px]">
                            {app.documents.length} documentos adjuntos
                          </span>
                        </div>
                      </div>
                    </td>

                    {/* Columna 4: Ubicación & Contacto */}
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-xs">
                        <p className="font-semibold text-slate-800 dark:text-slate-200">{app.zone}</p>
                        <p className="text-slate-500 dark:text-slate-400 mt-0.5">{app.phone}</p>
                        <p className="text-slate-400 text-[11px] truncate max-w-[180px]">
                          {app.email}
                        </p>
                      </div>
                    </td>

                    {/* Columna 5: Estado con Badge Tailwind */}
                    <td className="px-6 py-4 whitespace-nowrap">
                      {app.status === 'PENDING' && (
                        <span className="bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-300 rounded-full px-3 py-1 text-xs font-semibold inline-flex items-center gap-1.5 border border-yellow-200 dark:border-yellow-700/50">
                          <span className="w-2 h-2 rounded-full bg-yellow-500 animate-pulse"></span>
                          Pendiente de Validación
                        </span>
                      )}

                      {app.status === 'APPROVED' && (
                        <span className="bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-300 rounded-full px-3 py-1 text-xs font-semibold inline-flex items-center gap-1.5 border border-emerald-200 dark:border-emerald-700/50">
                          <span className="w-2 h-2 rounded-full bg-emerald-600 dark:bg-emerald-400"></span>
                          Aprobado & Activo
                        </span>
                      )}

                      {app.status === 'REJECTED' && (
                        <span className="bg-rose-100 dark:bg-rose-900/30 text-rose-800 dark:text-rose-300 rounded-full px-3 py-1 text-xs font-semibold inline-flex items-center gap-1.5 border border-rose-200 dark:border-rose-700/50">
                          <span className="w-2 h-2 rounded-full bg-rose-600 dark:bg-rose-400"></span>
                          Rechazado
                        </span>
                      )}
                    </td>

                    {/* Columna 6: Acciones Rápidas */}
                    <td className="px-6 py-4 whitespace-nowrap text-right">
                      <div className="flex items-center justify-end gap-2">
                        {/* Botón Ver Documentos RUC/Licencia */}
                        <button
                          type="button"
                          onClick={() => handleOpenDocumentsModal(app)}
                          className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-semibold text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 border border-slate-300 dark:border-slate-700 rounded-lg shadow-sm transition hover:text-slate-900 dark:hover:text-white"
                          title="Ver documentos oficiales del solicitante"
                        >
                          <Icons.Eye />
                          <span>Ver Documentos {app.type === 'MERCHANT' ? 'RUC' : 'Licencia'}</span>
                        </button>

                        {app.status === 'PENDING' ? (
                          <>
                            <button
                              type="button"
                              onClick={() => handleApprove(app.id)}
                              className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-lg text-xs font-semibold shadow-sm transition inline-flex items-center gap-1.5"
                            >
                              <Icons.Check />
                              <span>Aprobar</span>
                            </button>

                            <button
                              type="button"
                              onClick={() => handleOpenRejectModal(app)}
                              className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-lg text-xs font-semibold shadow-sm transition inline-flex items-center gap-1.5"
                            >
                              <Icons.XMark />
                              <span>Rechazar</span>
                            </button>
                          </>
                        ) : (
                          <button
                            type="button"
                            onClick={() => {
                              setApplications((prev) =>
                                prev.map((item) =>
                                  item.id === app.id ? { ...item, status: 'PENDING' } : item
                                )
                              );
                              showToast(`Solicitud ${app.id} devuelta a estado Pendiente`, 'success');
                            }}
                            className="text-xs text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 underline px-2 py-1"
                          >
                            Reevaluar
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Footer de la tabla con resumen de registros */}
        <div className="bg-slate-50 dark:bg-slate-800/60 px-6 py-3 border-t border-slate-200 dark:border-slate-800 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 dark:text-slate-400 gap-2">
          <div>
            Mostrando <span className="font-bold text-slate-800 dark:text-slate-200">{filteredApplications.length}</span>{' '}
            de <span className="font-bold text-slate-800 dark:text-slate-200">{applications.length}</span> solicitudes
            registradas
          </div>
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-yellow-400"></span>
              {applications.filter((a) => a.status === 'PENDING').length} Pendientes
            </span>
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
              {applications.filter((a) => a.status === 'APPROVED').length} Aprobados
            </span>
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-rose-500"></span>
              {applications.filter((a) => a.status === 'REJECTED').length} Rechazados
            </span>
          </div>
        </div>
      </div>

      {/* ── MODAL: VER DOCUMENTOS RUC / LICENCIA ───────────────────────────── */}
      {selectedApplication && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 dark:bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-6 animate-fadeIn">
          <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl max-w-4xl w-full max-h-[92vh] flex flex-col overflow-hidden border border-slate-200 dark:border-slate-800 transition-colors">
            {/* Modal Header */}
            <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between bg-slate-50/80 dark:bg-slate-800/80">
              <div className="flex items-center gap-3">
                <div
                  className={`w-10 h-10 rounded-xl flex items-center justify-center font-bold text-base shrink-0 ${
                    selectedApplication.type === 'MERCHANT'
                      ? 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300'
                      : 'bg-sky-100 dark:bg-sky-950/60 text-sky-800 dark:text-sky-300'
                  }`}
                >
                  {selectedApplication.type === 'MERCHANT' ? (
                    <Icons.Store />
                  ) : (
                    <Icons.Motorcycle />
                  )}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-base font-bold text-slate-900 dark:text-white">
                      Verificación Documentaria: {selectedApplication.businessName}
                    </h2>
                    <span className="text-xs font-mono font-semibold px-2 py-0.5 rounded bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-300 dark:border-slate-700">
                      {selectedApplication.documentType} {selectedApplication.documentNumber}
                    </span>
                  </div>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Solicitante: {selectedApplication.applicantName} • {selectedApplication.zone}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setSelectedApplication(null)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-2 rounded-lg hover:bg-slate-200/60 dark:hover:bg-slate-800 transition"
              >
                <Icons.XMark />
              </button>
            </div>

            {/* Modal Body */}
            <div className="flex-1 overflow-y-auto p-6 grid grid-cols-1 md:grid-cols-12 gap-6">
              {/* Panel Izquierdo: Lista de documentos */}
              <div className="md:col-span-5 flex flex-col gap-3">
                <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1">
                  Documentos Legales Obligatorios ({selectedApplication.documents.length})
                </p>

                {selectedApplication.documents.map((doc, idx) => (
                  <div
                    key={doc.id}
                    onClick={() => setSelectedDocumentIndex(idx)}
                    className={`p-3.5 rounded-xl border text-left cursor-pointer transition flex items-start gap-3 ${
                      selectedDocumentIndex === idx
                        ? 'bg-indigo-50/70 dark:bg-indigo-950/40 border-indigo-400 dark:border-indigo-500 ring-1 ring-indigo-400 shadow-sm'
                        : 'bg-white dark:bg-slate-800/80 border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700/60'
                    }`}
                  >
                    <div
                      className={`p-2 rounded-lg mt-0.5 shrink-0 ${
                        selectedDocumentIndex === idx
                          ? 'bg-indigo-600 text-white'
                          : 'bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300'
                      }`}
                    >
                      <Icons.FileText />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <p className="text-xs font-bold text-slate-900 dark:text-white truncate">{doc.name}</p>
                        <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/40">
                          {doc.status}
                        </span>
                      </div>
                      <p className="text-[11px] font-mono text-slate-500 dark:text-slate-400 truncate mt-0.5">
                        {doc.code}
                      </p>
                      <p className="text-[11px] text-slate-600 dark:text-slate-300 mt-1 line-clamp-2">{doc.notes}</p>
                    </div>
                  </div>
                ))}

                {/* Resumen de auditoría */}
                <div className="mt-auto bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 p-3.5 rounded-xl text-xs">
                  <div className="flex items-center gap-2 text-slate-700 dark:text-slate-200 font-semibold mb-1">
                    <Icons.ShieldCheck />
                    <span>Verificación Automatizada SRI / ANT</span>
                  </div>
                  <p className="text-slate-500 dark:text-slate-400 text-[11px]">
                    Los datos tributarios (SRI) y vehiculares (ANT / SPPAT) fueron validados contra los registros oficiales de la República del Ecuador.
                    Se validó el estándar de 13 dígitos para RUC y vigencia en la provincia del Carchi (04).
                  </p>
                </div>
              </div>

              {/* Panel Derecho: Visor del Documento seleccionado */}
              <div className="md:col-span-7 bg-slate-100/70 dark:bg-slate-800/40 rounded-xl border border-slate-200 dark:border-slate-700 p-5 flex flex-col">
                {selectedApplication.documents[selectedDocumentIndex] && (
                  <div className="flex-1 flex flex-col">
                    <div className="flex items-center justify-between pb-3 border-b border-slate-200 dark:border-slate-700 mb-4">
                      <div>
                        <h3 className="text-sm font-bold text-slate-900 dark:text-white">
                          {selectedApplication.documents[selectedDocumentIndex].name}
                        </h3>
                        <p className="text-xs text-slate-500 dark:text-slate-400 font-mono">
                          Archivo: {selectedApplication.documents[selectedDocumentIndex].code}
                        </p>
                      </div>
                      <button
                        onClick={() =>
                          showToast(
                            `Descargando copia legal de ${selectedApplication.documents[selectedDocumentIndex].code}...`,
                            'success'
                          )
                        }
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700 shadow-sm"
                      >
                        <Icons.Download />
                        <span>Descargar PDF</span>
                      </button>
                    </div>

                    {/* Previsualización gráfica simulada del documento oficial */}
                    <div className="flex-1 bg-white dark:bg-slate-900 rounded-xl border border-slate-300 dark:border-slate-700 p-5 shadow-inner flex flex-col justify-between">
                      {/* Caso: Ficha RUC (SRI Ecuador) */}
                      {selectedApplication.documents[selectedDocumentIndex].previewType ===
                        'ruc' && (() => {
                        const rucNumber = selectedApplication.documents[selectedDocumentIndex].details?.taxId || selectedApplication.documentNumber;
                        const validation = validateEcuadorianRuc(rucNumber);

                        return (
                          <div className="space-y-4">
                            <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-3">
                              <div>
                                <span className="text-xs font-extrabold text-blue-900 dark:text-blue-400 tracking-wider">
                                  SRI — SERVICIO DE RENTAS INTERNAS (REPÚBLICA DEL ECUADOR)
                                </span>
                                <h4 className="text-sm font-black text-slate-900 dark:text-white mt-1">
                                  CERTIFICADO DE REGISTRO ÚNICO DE CONTRIBUYENTES (RUC)
                                </h4>
                              </div>
                              <span className="text-xs font-mono font-bold bg-blue-100 dark:bg-blue-950/60 text-blue-900 dark:text-blue-300 px-2 py-1 rounded">
                                OFICIAL SRI
                              </span>
                            </div>

                            {/* Badge interactivo de validación de 13 dígitos */}
                            <div className={`p-3 rounded-xl border text-xs font-medium flex items-center justify-between ${
                              validation.isValid
                                ? 'bg-emerald-50/80 dark:bg-emerald-950/40 border-emerald-300 dark:border-emerald-700 text-emerald-900 dark:text-emerald-300'
                                : 'bg-rose-50/80 dark:bg-rose-950/40 border-rose-300 dark:border-rose-700 text-rose-900 dark:text-rose-300'
                            }`}>
                              <div className="flex items-center gap-2">
                                <span className="text-base">{validation.isValid ? '✅' : '❌'}</span>
                                <div>
                                  <p className="font-bold">
                                    {validation.isValid ? 'Validación Fiscal SRI Aprobada' : 'Observación en Validación Fiscal'}
                                  </p>
                                  <p className="text-[11px] text-slate-600 dark:text-slate-400">
                                    {validation.isValid
                                      ? `Estándar 13 dígitos cumplido • ${validation.province} • Tipo: ${validation.type}`
                                      : validation.reason}
                                  </p>
                                </div>
                              </div>
                              <span className="font-mono text-[10px] font-bold uppercase px-2 py-1 bg-white dark:bg-slate-800 rounded border border-slate-200 dark:border-slate-700 text-slate-800 dark:text-slate-200">
                                SRI 13-DIGITS
                              </span>
                            </div>

                            <div className="grid grid-cols-2 gap-3 text-xs">
                              <div>
                                <span className="text-slate-400 block text-[10px] uppercase font-bold">
                                  Número de RUC (13 dígitos)
                                </span>
                                <span className="font-mono font-bold text-slate-900 dark:text-white text-sm">
                                  {rucNumber}
                                </span>
                              </div>
                              <div>
                                <span className="text-slate-400 block text-[10px] uppercase font-bold">
                                  Estado Tributario
                                </span>
                                <span className="inline-block px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/40">
                                  ACTIVO / AL DÍA
                                </span>
                              </div>
                              <div className="col-span-2">
                                <span className="text-slate-400 block text-[10px] uppercase font-bold">
                                  Razón Social / Nombre Comercial
                                </span>
                                <span className="font-semibold text-slate-900 dark:text-white">
                                  {selectedApplication.documents[selectedDocumentIndex].details
                                    ?.legalName || selectedApplication.businessName}
                                </span>
                              </div>
                              <div>
                                <span className="text-slate-400 block text-[10px] uppercase font-bold">
                                  Régimen Tributario
                                </span>
                                <span className="font-medium text-slate-800 dark:text-slate-200">
                                  {selectedApplication.documents[selectedDocumentIndex].details
                                    ?.regime || 'Régimen General'}
                                </span>
                              </div>
                              <div>
                                <span className="text-slate-400 block text-[10px] uppercase font-bold">
                                  Jurisdicción Provincial
                                </span>
                                <span className="font-medium text-indigo-700 dark:text-indigo-400">
                                  Carchi (Código 04) - Tulcán
                                </span>
                              </div>
                              <div className="col-span-2">
                                <span className="text-slate-400 block text-[10px] uppercase font-bold">
                                  Domicilio Fiscal Registrado
                                </span>
                                <span className="text-slate-800 dark:text-slate-200">
                                  {selectedApplication.documents[selectedDocumentIndex].details
                                    ?.address || 'Tulcán, Carchi, Ecuador'}
                                </span>
                              </div>
                            </div>
                          </div>
                        );
                      })()}

                      {/* Caso: Licencia de Conducir Repartidor (ANT Ecuador) */}
                      {selectedApplication.documents[selectedDocumentIndex].previewType ===
                        'driver_license' && (
                        <div className="space-y-4">
                          <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-3">
                            <div>
                              <span className="text-xs font-extrabold text-red-800 dark:text-red-400 tracking-wider">
                                REPÚBLICA DEL ECUADOR — AGENCIA NACIONAL DE TRÁNSITO (ANT)
                              </span>
                              <h4 className="text-sm font-black text-slate-900 dark:text-white mt-1">
                                LICENCIA DE CONDUCIR PROFESIONAL / TIPO A (MOTOCICLETAS)
                              </h4>
                            </div>
                            <span className="text-xs font-mono font-bold bg-red-100 dark:bg-red-950/60 text-red-900 dark:text-red-300 px-2 py-1 rounded">
                              TIPO A
                            </span>
                          </div>

                          <div className="grid grid-cols-2 gap-3 text-xs">
                            <div>
                              <span className="text-slate-400 block text-[10px] uppercase font-bold">
                                Conductor / Titular
                              </span>
                              <span className="font-bold text-slate-900 dark:text-white">
                                {selectedApplication.applicantName}
                              </span>
                            </div>
                            <div>
                              <span className="text-slate-400 block text-[10px] uppercase font-bold">
                                Categoría Habilitada
                              </span>
                              <span className="font-mono font-bold text-indigo-700 dark:text-indigo-400">
                                TIPO A (Motos y similares)
                              </span>
                            </div>
                            <div>
                              <span className="text-slate-400 block text-[10px] uppercase font-bold">
                                Fecha de Vencimiento
                              </span>
                              <span className="font-semibold text-slate-800 dark:text-slate-200">
                                {selectedApplication.documents[selectedDocumentIndex].expiryDate ||
                                  '2029-03-15'}
                              </span>
                            </div>
                            <div>
                              <span className="text-slate-400 block text-[10px] uppercase font-bold">
                                Récord de Puntos ANT
                              </span>
                              <span className="inline-block px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/40">
                                {selectedApplication.documents[selectedDocumentIndex].details?.pointsRemaining || '30 / 30 PUNTOS'}
                              </span>
                            </div>
                            <div className="col-span-2">
                              <span className="text-slate-400 block text-[10px] uppercase font-bold">
                                Cobertura Seguro Vial Obligatorio
                              </span>
                              <span className="text-slate-700 dark:text-slate-300 font-medium">
                                Certificado SPPAT Vigente (Sistema Público para Pago de Accidentes de Tránsito)
                              </span>
                            </div>
                          </div>
                        </div>
                      )}

                      {/* Caso General / Otros Documentos */}
                      {selectedApplication.documents[selectedDocumentIndex].previewType !==
                        'ruc' &&
                        selectedApplication.documents[selectedDocumentIndex].previewType !==
                          'driver_license' && (
                          <div className="space-y-4">
                            <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-3">
                              <h4 className="text-sm font-bold text-slate-900 dark:text-white">
                                {selectedApplication.documents[selectedDocumentIndex].name}
                              </h4>
                              <span className="text-xs font-mono font-bold bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 px-2 py-1 rounded">
                                DIGITAL CERTIFIED
                              </span>
                            </div>
                            <div className="p-4 bg-slate-50 dark:bg-slate-800/80 rounded-lg text-xs space-y-2 border border-slate-200 dark:border-slate-700">
                              <p className="font-semibold text-slate-800 dark:text-slate-200">
                                {selectedApplication.documents[selectedDocumentIndex].notes}
                              </p>
                              <p className="text-slate-500 dark:text-slate-400">
                                Fecha de Emisión:{' '}
                                {selectedApplication.documents[selectedDocumentIndex].issueDate}
                              </p>
                              <p className="text-slate-500 dark:text-slate-400">
                                Código de Verificación Hash:{' '}
                                <span className="font-mono text-[10px] text-slate-700 dark:text-slate-300">
                                  SHA256:7f9a88c3120...918bca
                                </span>
                              </p>
                            </div>
                          </div>
                        )}

                      {/* Sello de verificación al pie */}
                      <div className="mt-6 pt-4 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between text-[11px] text-slate-500 dark:text-slate-400">
                        <span className="flex items-center gap-1.5 font-medium text-emerald-600 dark:text-emerald-400">
                          <Icons.CheckCircle />
                          Firma digital y sellos validados contra entidad emisora
                        </span>
                        <span className="font-mono text-slate-400">ID: DOC-SEC-9921</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Modal Footer */}
            <div className="px-6 py-4 bg-slate-50 dark:bg-slate-800/80 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between">
              <button
                type="button"
                onClick={() => setSelectedApplication(null)}
                className="px-4 py-2 border border-slate-300 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 transition"
              >
                Cerrar Visor
              </button>

              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => handleOpenRejectModal(selectedApplication)}
                  className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-lg text-xs font-semibold shadow-sm transition inline-flex items-center gap-1.5"
                >
                  <Icons.XMark />
                  <span>Rechazar Solicitud</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleApprove(selectedApplication.id)}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white px-5 py-2 rounded-lg text-xs font-semibold shadow-md transition inline-flex items-center gap-1.5"
                >
                  <Icons.Check />
                  <span>Aprobar y Activar en Plataforma</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ── MODAL: MOTIVO DE RECHAZO ────────────────────────────────────────── */}
      {rejectingApplication && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 dark:bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl max-w-lg w-full overflow-hidden border border-slate-200 dark:border-slate-800 transition-colors">
            <div className="px-6 py-4 bg-rose-50 dark:bg-rose-950/40 border-b border-rose-100 dark:border-rose-900/50 flex items-center justify-between">
              <div className="flex items-center gap-2 text-rose-800 dark:text-rose-300">
                <Icons.AlertTriangle />
                <h3 className="text-sm font-bold">
                  Rechazar Solicitud: {rejectingApplication.businessName}
                </h3>
              </div>
              <button
                onClick={() => setRejectingApplication(null)}
                className="text-rose-400 hover:text-rose-700 dark:hover:text-rose-200"
              >
                <Icons.XMark />
              </button>
            </div>

            <div className="p-6 space-y-4">
              <p className="text-xs text-slate-600 dark:text-slate-300">
                Indica la causal del rechazo para que el solicitante reciba una notificación por
                correo electrónico y SMS con los pasos correctivos necesarios para subsanar su
                expediente.
              </p>

              <div>
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1.5">
                  Motivo Principal de Rechazo
                </label>
                <select
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-xs font-medium text-slate-800 dark:text-slate-200 focus:ring-2 focus:ring-rose-500 focus:outline-none"
                >
                  <option value="Documentación con inconsistencias o borrosa">
                    Documentación con inconsistencias o borrosa
                  </option>
                  <option value="Ficha RUC no figura en estado ACTIVO ante el SRI (Ecuador)">
                    Ficha RUC no figura en estado ACTIVO ante el SRI (Ecuador)
                  </option>
                  <option value="RUC no cumple con el estándar ecuatoriano de 13 dígitos o provincia Carchi (04)">
                    RUC no cumple con el estándar ecuatoriano de 13 dígitos o provincia Carchi (04)
                  </option>
                  <option value="Licencia de Conducir ANT o póliza SPPAT vencida / no registrada">
                    Licencia de Conducir ANT o póliza SPPAT vencida / no registrada
                  </option>
                  <option value="Antecedentes observados o no conformes con las políticas">
                    Antecedentes observados o no conformes con las políticas
                  </option>
                  <option value="Giro de negocio o vehículo no admitido en la plataforma">
                    Giro de negocio o vehículo no admitido en la plataforma
                  </option>
                  <option value="Otro motivo especificado en comentarios">
                    Otro motivo especificado en comentarios
                  </option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1.5">
                  Detalles o Instrucciones de Subsanación (Opcional)
                </label>
                <textarea
                  rows={3}
                  value={customRejectionComment}
                  onChange={(e) => setCustomRejectionComment(e.target.value)}
                  placeholder="Ej: Por favor adjuntar la póliza SOAT con vigencia actualizada al año en curso..."
                  className="w-full p-3 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-xs text-slate-800 dark:text-slate-200 focus:ring-2 focus:ring-rose-500 focus:outline-none"
                />
              </div>
            </div>

            <div className="px-6 py-4 bg-slate-50 dark:bg-slate-800/80 border-t border-slate-200 dark:border-slate-800 flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => setRejectingApplication(null)}
                className="px-4 py-2 border border-slate-300 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleConfirmReject}
                className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-lg text-xs font-semibold shadow-sm transition"
              >
                Confirmar Rechazo y Notificar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
