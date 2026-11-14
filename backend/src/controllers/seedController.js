const Animal = require('../models/Animal');
const Potrero = require('../models/Potrero');
const Ordeno = require('../models/Ordeno');
const Tarea = require('../models/Tarea');
const Usuario = require('../models/Usuario');
const Auditoria = require('../models/Auditoria');

exports.poblarBaseDeDatos = async (req, res, next) => {
  try {
    const fuerza = req.query.fuerza === 'true';

    const conteoAnimales = await Animal.countDocuments();
    if (conteoAnimales > 0 && !fuerza) {
      return res.status(200).json({
        success: true,
        message: 'La base de datos ya contiene información. Para repoblar use ?fuerza=true',
        conteo: {
          animales: conteoAnimales,
          potreros: await Potrero.countDocuments(),
          ordeno: await Ordeno.countDocuments(),
          tareas: await Tarea.countDocuments(),
          usuarios: await Usuario.countDocuments()
        }
      });
    }

    if (fuerza) {
      await Animal.deleteMany({});
      await Potrero.deleteMany({});
      await Ordeno.deleteMany({});
      await Tarea.deleteMany({});
      await Usuario.deleteMany({});
      await Auditoria.deleteMany({});
    }

    // 1. Usuarios Móviles
    await Usuario.create([
      {
        nombre: 'Juan Pablo Berrocal',
        finca: 'Finca La Esperanza',
        correo: 'juan@correo.com',
        contrasena: 'admin123',
        rol: 'Administrador',
        fechaRegistro: '05/11/2026',
        ultimoAcceso: 'Hoy 01:45 PM',
        dispositivo: 'Android (Samsung Galaxy S22)',
        estado: 'Activo'
      },
      {
        nombre: 'Carlos Mario Benítez',
        finca: 'Finca La Esperanza',
        correo: 'carlos.benitez@correo.com',
        contrasena: 'operario123',
        rol: 'Operario de Campo',
        fechaRegistro: '04/11/2026',
        ultimoAcceso: 'Hoy 10:15 AM',
        dispositivo: 'Android (Motorola G60)',
        estado: 'Activo'
      },
      {
        nombre: 'José Antonio Hoyos',
        finca: 'Finca La Esperanza',
        correo: 'jose.hoyos@correo.com',
        contrasena: 'vaquero123',
        rol: 'Vaquero',
        fechaRegistro: '02/11/2026',
        ultimoAcceso: 'Ayer 05:30 PM',
        dispositivo: 'Android (Xiaomi Redmi Note 11)',
        estado: 'Activo'
      },
      {
        nombre: 'Pedro Nel Gómez',
        finca: 'Finca La Esperanza',
        correo: 'pedro.gomez@correo.com',
        contrasena: 'campo123',
        rol: 'Operario de Campo',
        fechaRegistro: '28/10/2026',
        ultimoAcceso: '04/11/2026 04:20 PM',
        dispositivo: 'Android (Samsung A32)',
        estado: 'Inactivo'
      }
    ]);

    // 2. Potreros (Exactos al Mockup Pantalla 7 y 8)
    await Potrero.create([
      { nombre: 'Potrero Norte', largoMetros: 200, anchoMetros: 150, areaM2: 30000, areaHa: 3.0, estado: 'Ocupado', diasOcupacion: 3, diasDescanso: 30, tipoPasto: 'Brachiaria', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '05/11/2026' },
      { nombre: 'Potrero El Roble', largoMetros: 200, anchoMetros: 120, areaM2: 24000, areaHa: 2.4, estado: 'En descanso', diasOcupacion: 3, diasDescanso: 30, tipoPasto: 'Estrella', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '05/11/2026' },
      { nombre: 'Potrero La Ceiba', largoMetros: 200, anchoMetros: 140, areaM2: 28000, areaHa: 2.8, estado: 'En descanso', diasOcupacion: 3, diasDescanso: 30, tipoPasto: 'Brachiaria', registradoPor: 'Carlos Mario Benítez', fechaRegistro: '04/11/2026' },
      { nombre: 'Potrero El Manantial', largoMetros: 250, anchoMetros: 140, areaM2: 35000, areaHa: 3.5, estado: 'Ocupado', diasOcupacion: 4, diasDescanso: 28, tipoPasto: 'Guinea Mombasa', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '03/11/2026' },
      { nombre: 'Potrero Las Brisas', largoMetros: 200, anchoMetros: 110, areaM2: 22000, areaHa: 2.2, estado: 'En descanso', diasOcupacion: 3, diasDescanso: 30, tipoPasto: 'Brachiaria', registradoPor: 'José Antonio Hoyos', fechaRegistro: '02/11/2026' },
      { nombre: 'Potrero San Isidro', largoMetros: 200, anchoMetros: 155, areaM2: 31000, areaHa: 3.1, estado: 'En descanso', diasOcupacion: 3, diasDescanso: 30, tipoPasto: 'Pangola', registradoPor: 'Carlos Mario Benítez', fechaRegistro: '01/11/2026' },
      { nombre: 'Potrero La Esperanza', largoMetros: 200, anchoMetros: 200, areaM2: 40000, areaHa: 4.0, estado: 'En descanso', diasOcupacion: 4, diasDescanso: 32, tipoPasto: 'Guinea Mombasa', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '30/10/2026' },
      { nombre: 'Potrero El Cedro', largoMetros: 190, anchoMetros: 150, areaM2: 28500, areaHa: 2.85, estado: 'En descanso', diasOcupacion: 3, diasDescanso: 30, tipoPasto: 'Estrella', registradoPor: 'José Antonio Hoyos', fechaRegistro: '29/10/2026' },
      { nombre: 'Potrero Los Pinos', largoMetros: 180, anchoMetros: 140, areaM2: 25200, areaHa: 2.52, estado: 'En descanso', diasOcupacion: 3, diasDescanso: 30, tipoPasto: 'Brachiaria', registradoPor: 'Carlos Mario Benítez', fechaRegistro: '28/10/2026' },
      { nombre: 'Potrero El Trébol', largoMetros: 210, anchoMetros: 160, areaM2: 33600, areaHa: 3.36, estado: 'En descanso', diasOcupacion: 3, diasDescanso: 30, tipoPasto: 'Brachiaria', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '27/10/2026' },
      { nombre: 'Potrero La Vega', largoMetros: 200, anchoMetros: 135, areaM2: 27000, areaHa: 2.7, estado: 'En descanso', diasOcupacion: 3, diasDescanso: 30, tipoPasto: 'Estrella', registradoPor: 'Carlos Mario Benítez', fechaRegistro: '26/10/2026' }
    ]);

    // 3. Animales (24 Cabezas de Ganado)
    await Animal.create([
      { codigo: 'A-001', nombre: 'Lucero', raza: 'Holstein', sexo: 'Hembra', pesoKg: 480, estado: 'En producción', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '05/11/2026' },
      { codigo: 'A-002', nombre: 'Canelo', raza: 'Normando', sexo: 'Macho', pesoKg: 620, estado: 'Reproductor', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '05/11/2026' },
      { codigo: 'A-003', nombre: 'Margarita', raza: 'Jersey', sexo: 'Hembra', pesoKg: 410, estado: 'En producción', registradoPor: 'Carlos Mario Benítez', fechaRegistro: '04/11/2026' },
      { codigo: 'A-004', nombre: 'Paloma', raza: 'Holstein', sexo: 'Hembra', pesoKg: 495, estado: 'En producción', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '04/11/2026' },
      { codigo: 'A-005', nombre: 'Estrella', raza: 'Jersey', sexo: 'Hembra', pesoKg: 420, estado: 'Gestante', registradoPor: 'José Antonio Hoyos', fechaRegistro: '03/11/2026' },
      { codigo: 'A-006', nombre: 'Relámpago', raza: 'Brahman', sexo: 'Macho', pesoKg: 680, estado: 'Engorde', registradoPor: 'Carlos Mario Benítez', fechaRegistro: '03/11/2026' },
      { codigo: 'A-007', nombre: 'Campanera', raza: 'Girolando', sexo: 'Hembra', pesoKg: 460, estado: 'En producción', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '02/11/2026' },
      { codigo: 'A-008', nombre: 'Nube', raza: 'Holstein', sexo: 'Hembra', pesoKg: 475, estado: 'Seca', registradoPor: 'José Antonio Hoyos', fechaRegistro: '02/11/2026' },
      { codigo: 'A-009', nombre: 'Princesa', raza: 'Jersey', sexo: 'Hembra', pesoKg: 390, estado: 'En producción', registradoPor: 'Carlos Mario Benítez', fechaRegistro: '01/11/2026' },
      { codigo: 'A-010', nombre: 'Torbellino', raza: 'Normando', sexo: 'Macho', pesoKg: 590, estado: 'Reproductor', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '01/11/2026' },
      { codigo: 'A-011', nombre: 'Mariposa', raza: 'Girolando', sexo: 'Hembra', pesoKg: 450, estado: 'En producción', registradoPor: 'José Antonio Hoyos', fechaRegistro: '30/10/2026' },
      { codigo: 'A-012', nombre: 'Gitana', raza: 'Holstein', sexo: 'Hembra', pesoKg: 485, estado: 'En producción', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '30/10/2026' },
      { codigo: 'A-013', nombre: 'Centella', raza: 'Brahman', sexo: 'Hembra', pesoKg: 510, estado: 'Seca', registradoPor: 'Carlos Mario Benítez', fechaRegistro: '29/10/2026' },
      { codigo: 'A-014', nombre: 'Manantial', raza: 'Jersey', sexo: 'Hembra', pesoKg: 415, estado: 'En producción', registradoPor: 'José Antonio Hoyos', fechaRegistro: '29/10/2026' },
      { codigo: 'A-015', nombre: 'Sultán', raza: 'Brahman', sexo: 'Macho', pesoKg: 710, estado: 'Reproductor', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '28/10/2026' },
      { codigo: 'A-016', nombre: 'Aurora', raza: 'Holstein', sexo: 'Hembra', pesoKg: 490, estado: 'En producción', registradoPor: 'Carlos Mario Benítez', fechaRegistro: '28/10/2026' },
      { codigo: 'A-017', nombre: 'Brisa', raza: 'Girolando', sexo: 'Hembra', pesoKg: 440, estado: 'Gestante', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '27/10/2026' },
      { codigo: 'A-018', nombre: 'Dorado', raza: 'Normando', sexo: 'Macho', pesoKg: 580, estado: 'Engorde', registradoPor: 'José Antonio Hoyos', fechaRegistro: '27/10/2026' },
      { codigo: 'A-019', nombre: 'Luna', raza: 'Jersey', sexo: 'Hembra', pesoKg: 405, estado: 'En producción', registradoPor: 'Carlos Mario Benítez', fechaRegistro: '26/10/2026' },
      { codigo: 'A-020', nombre: 'Pecosa', raza: 'Holstein', sexo: 'Hembra', pesoKg: 470, estado: 'En producción', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '26/10/2026' },
      { codigo: 'A-021', nombre: 'Cariñosa', raza: 'Girolando', sexo: 'Hembra', pesoKg: 455, estado: 'En producción', registradoPor: 'José Antonio Hoyos', fechaRegistro: '25/10/2026' },
      { codigo: 'A-022', nombre: 'Camilo', raza: 'Brahman', sexo: 'Macho', pesoKg: 640, estado: 'Engorde', registradoPor: 'Carlos Mario Benítez', fechaRegistro: '25/10/2026' },
      { codigo: 'A-023', nombre: 'Dulzura', raza: 'Jersey', sexo: 'Hembra', pesoKg: 410, estado: 'En producción', registradoPor: 'Juan Pablo Berrocal', fechaRegistro: '24/10/2026' },
      { codigo: 'A-024', nombre: 'Esperanza', raza: 'Holstein', sexo: 'Hembra', pesoKg: 505, estado: 'En producción', registradoPor: 'Carlos Mario Benítez', fechaRegistro: '24/10/2026' }
    ]);

    // 4. Registros de Ordeño (Mockup Pantalla 9 - 86L hoy)
    await Ordeno.create([
      { fecha: '2026-11-05', jornada: 'Mañana', litros: 48, responsable: 'Juan Pablo Berrocal', horaRegistro: '06:15 AM' },
      { fecha: '2026-11-05', jornada: 'Tarde', litros: 38, responsable: 'Juan Pablo Berrocal', horaRegistro: '04:30 PM' },
      { fecha: '2026-11-04', jornada: 'Mañana', litros: 45, responsable: 'Pedro Nel Gómez', horaRegistro: '06:10 AM' },
      { fecha: '2026-11-04', jornada: 'Tarde', litros: 40, responsable: 'Pedro Nel Gómez', horaRegistro: '04:25 PM' },
      { fecha: '2026-11-03', jornada: 'Mañana', litros: 50, responsable: 'Juan Pablo Berrocal', horaRegistro: '06:05 AM' },
      { fecha: '2026-11-03', jornada: 'Tarde', litros: 37, responsable: 'Juan Pablo Berrocal', horaRegistro: '04:40 PM' },
      { fecha: '2026-11-02', jornada: 'Mañana', litros: 47, responsable: 'Carlos Mario Benítez', horaRegistro: '06:20 AM' },
      { fecha: '2026-11-02', jornada: 'Tarde', litros: 39, responsable: 'Carlos Mario Benítez', horaRegistro: '04:35 PM' },
      { fecha: '2026-11-01', jornada: 'Mañana', litros: 49, responsable: 'Juan Pablo Berrocal', horaRegistro: '06:00 AM' },
      { fecha: '2026-11-01', jornada: 'Tarde', litros: 41, responsable: 'Juan Pablo Berrocal', horaRegistro: '04:15 PM' }
    ]);

    // 5. Tareas de Cuidado (Mockup Pantalla 10)
    await Tarea.create([
      { titulo: 'Vacuna aftosa lote hembras', categoria: 'Vacunación', fechaLimite: '2026-11-01', completada: true, prioridad: 'Alta', responsable: 'Juan Pablo Berrocal' },
      { titulo: 'Vacuna clostridial terneros', categoria: 'Vacunación', fechaLimite: '2026-11-10', completada: false, prioridad: 'Alta', responsable: 'Carlos Mario Benítez' },
      { titulo: 'Desparasitación con Ivermectina', categoria: 'Desparasitación', fechaLimite: '2026-11-15', completada: false, prioridad: 'Media', responsable: 'José Antonio Hoyos' },
      { titulo: 'Revisión y tensión de cercas Potrero Norte', categoria: 'Mantenimiento', fechaLimite: '2026-11-20', completada: false, prioridad: 'Media', responsable: 'Pedro Nel Gómez' },
      { titulo: 'Baño para control de garrapatas y moscas', categoria: 'Desparasitación', fechaLimite: '2026-11-25', completada: false, prioridad: 'Baja', responsable: 'José Antonio Hoyos' },
      { titulo: 'Limpieza de bebederos y saladeros', categoria: 'Mantenimiento', fechaLimite: '2026-11-03', completada: true, prioridad: 'Media', responsable: 'Carlos Mario Benítez' }
    ]);

    // 6. Auditoría
    await Auditoria.create([
      { modulo: 'Ordeño', accion: 'Registro', descripcion: 'Pesaje registrado: Jornada Tarde (38 L)', usuario: 'Juan Pablo Berrocal', origen: 'App Android' },
      { modulo: 'Ordeño', accion: 'Registro', descripcion: 'Pesaje registrado: Jornada Mañana (48 L)', usuario: 'Juan Pablo Berrocal', origen: 'App Android' },
      { modulo: 'Ganado', accion: 'Registro', descripcion: 'Alta de animal: Lucero (A-001)', usuario: 'Juan Pablo Berrocal', origen: 'App Android' },
      { modulo: 'Potreros', accion: 'Registro', descripcion: 'Alta de potrero: Potrero Norte (3.0 ha)', usuario: 'Juan Pablo Berrocal', origen: 'App Android' }
    ]);

    res.status(201).json({
      success: true,
      message: 'Base de datos inicializada exitosamente con datos de la Finca La Esperanza',
      resumen: {
        animales: 24,
        potreros: 11,
        ordenos: 10,
        tareas: 6,
        usuarios: 4
      }
    });
  } catch (error) {
    next(error);
  }
};
