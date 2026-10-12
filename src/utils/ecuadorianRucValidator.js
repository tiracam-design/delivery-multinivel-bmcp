/**
 * Validador oficial del Registro Único de Contribuyentes (RUC) del Ecuador.
 * Normativa: Servicio de Rentas Internas (SRI).
 * Estándar: 13 dígitos numéricos.
 * Provincias: 01 a 24 (04 = Carchi).
 */

export function validateEcuadorianRuc(ruc) {
  if (!ruc || typeof ruc !== 'string') {
    return { isValid: false, reason: 'El RUC no puede estar vacío' };
  }

  const cleanRuc = ruc.trim();

  if (!/^\d{13}$/.test(cleanRuc)) {
    return {
      isValid: false,
      reason: `El RUC debe tener exactamente 13 dígitos numéricos (actual: ${cleanRuc.length} caracteres)`
    };
  }

  const prov = parseInt(cleanRuc.substring(0, 2), 10);
  if (prov < 1 || prov > 24) {
    return {
      isValid: false,
      reason: `Código de provincia inválido: ${cleanRuc.substring(0, 2)}. Debe estar entre 01 y 24.`
    };
  }

  const thirdDigit = parseInt(cleanRuc.charAt(2), 10);
  const isCarchi = prov === 4;

  // Persona Natural (3er dígito 0 a 5)
  if (thirdDigit >= 0 && thirdDigit <= 5) {
    const establishment = cleanRuc.substring(10);
    if (establishment === '000') {
      return { isValid: false, reason: 'El código de establecimiento no puede ser 000 (usualmente 001)' };
    }

    if (!validateModulo10(cleanRuc.substring(0, 10))) {
      return { isValid: false, reason: 'Dígito verificador de cédula inválido (Módulo 10 SRI)' };
    }

    return {
      isValid: true,
      type: 'PERSONA_NATURAL',
      province: isCarchi ? 'Carchi (04)' : `Provincia ${prov}`,
      isCarchi,
      establishment
    };
  }

  // Sociedad Privada o Extranjeros (3er dígito 9)
  if (thirdDigit === 9) {
    const establishment = cleanRuc.substring(10);
    if (establishment === '000') {
      return { isValid: false, reason: 'El código de establecimiento no puede ser 000' };
    }

    if (!validateModulo11Sociedades(cleanRuc)) {
      return { isValid: false, reason: 'Dígito verificador de sociedad privada inválido (Módulo 11 SRI)' };
    }

    return {
      isValid: true,
      type: 'SOCIEDAD_PRIVADA',
      province: isCarchi ? 'Carchi (04)' : `Provincia ${prov}`,
      isCarchi,
      establishment
    };
  }

  // Institución Pública (3er dígito 6)
  if (thirdDigit === 6) {
    const establishment = cleanRuc.substring(9);
    if (establishment === '0000') {
      return { isValid: false, reason: 'El código de establecimiento público no puede ser 0000' };
    }

    if (!validateModulo11Publicas(cleanRuc)) {
      return { isValid: false, reason: 'Dígito verificador de entidad pública inválido (Módulo 11 SRI)' };
    }

    return {
      isValid: true,
      type: 'INSTITUCION_PUBLICA',
      province: isCarchi ? 'Carchi (04)' : `Provincia ${prov}`,
      isCarchi,
      establishment
    };
  }

  return { isValid: false, reason: 'Tercer dígito no corresponde a un tipo de contribuyente SRI válido (0-6, 9)' };
}

function validateModulo10(cedula) {
  const digits = cedula.split('').map(d => parseInt(d, 10));
  const coef = [2, 1, 2, 1, 2, 1, 2, 1, 2];
  let sum = 0;

  for (let i = 0; i < 9; i++) {
    let p = digits[i] * coef[i];
    if (p >= 10) p -= 9;
    sum += p;
  }

  const remainder = sum % 10;
  const verifier = remainder === 0 ? 0 : 10 - remainder;
  return verifier === digits[9];
}

function validateModulo11Sociedades(ruc) {
  const digits = ruc.split('').map(d => parseInt(d, 10));
  const coef = [4, 3, 2, 7, 6, 5, 4, 3, 2];
  let sum = 0;

  for (let i = 0; i < 9; i++) {
    sum += digits[i] * coef[i];
  }

  const remainder = sum % 11;
  const verifier = remainder === 0 ? 0 : 11 - remainder;
  return verifier === digits[9];
}

function validateModulo11Publicas(ruc) {
  const digits = ruc.split('').map(d => parseInt(d, 10));
  const coef = [3, 2, 7, 6, 5, 4, 3, 2];
  let sum = 0;

  for (let i = 0; i < 8; i++) {
    sum += digits[i] * coef[i];
  }

  const remainder = sum % 11;
  const verifier = remainder === 0 ? 0 : 11 - remainder;
  return verifier === digits[8];
}
