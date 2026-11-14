const Ordeno = require('../models/Ordeno');
const Auditoria = require('../models/Auditoria');

// GET /api/ordeno
exports.obtenerRegistrosOrdeno = async (req, res, next) => {
  try {
    const { jornada, fecha } = req.query;
    const filter = {};

    if (jornada && jornada !== 'Todas') {
      filter.jornada = jornada;
    }
    if (fecha) {
      filter.fecha = fecha;
    }

    const registros = await Ordeno.find(filter).sort({ fecha: -1, createdAt: -1 });

    res.status(200).json({
      success: true,
      count: registros.length,
      data: registros
    });
  } catch (error) {
    next(error);
  }
};

// GET /api/ordeno/resumen
exports.obtenerResumenHoy = async (req, res, next) => {
  try {
    const hoy = req.query.fecha || new Date().toISOString().split('T')[0];
    const registrosHoy = await Ordeno.find({ fecha: hoy });

    const totalLitrosHoy = registrosHoy.reduce((acc, r) => acc + r.litros, 0);
    const manana = registrosHoy.find(r => r.jornada === 'Mañana')?.litros || 0;
    const tarde = registrosHoy.find(r => r.jornada === 'Tarde')?.litros || 0;

    res.status(200).json({
      success: true,
      fecha: hoy,
      totalLitrosHoy,
      litrosManana: manana,
      litrosTarde: tarde
    });
  } catch (error) {
    next(error);
  }
};

// POST /api/ordeno
exports.crearRegistroOrdeno = async (req, res, next) => {
  try {
    const nuevoRegistro = await Ordeno.create(req.body);

    await Auditoria.create({
      modulo: 'Ordeño',
      accion: 'Registro',
      descripcion: `Pesaje registrado: ${nuevoRegistro.jornada} (${nuevoRegistro.litros} L)`,
      usuario: nuevoRegistro.responsable || 'Android Device',
      origen: 'App Android'
    });

    res.status(201).json({
      success: true,
      message: 'Registro de ordeño guardado exitosamente',
      data: nuevoRegistro
    });
  } catch (error) {
    next(error);
  }
};

// PUT /api/ordeno/:id  (la app Android edita registros)
exports.actualizarRegistroOrdeno = async (req, res, next) => {
  try {
    const reg = await Ordeno.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
    if (!reg) {
      return res.status(404).json({ success: false, message: 'Registro de ordeño no encontrado' });
    }
    res.status(200).json({ success: true, message: 'Registro de ordeño actualizado exitosamente', data: reg });
  } catch (error) {
    next(error);
  }
};

// DELETE /api/ordeno/:id
exports.eliminarRegistroOrdeno = async (req, res, next) => {
  try {
    const reg = await Ordeno.findById(req.params.id);
    if (!reg) {
      return res.status(404).json({
        success: false,
        message: 'Registro de ordeño no encontrado'
      });
    }

    await Ordeno.findByIdAndDelete(req.params.id);

    await Auditoria.create({
      modulo: 'Ordeño',
      accion: 'Eliminación',
      descripcion: `Registro de ordeño eliminado: ${reg.jornada} ${reg.fecha} (${reg.litros} L)`,
      usuario: 'Administrador Web',
      origen: 'Panel Web'
    });

    res.status(200).json({
      success: true,
      message: 'Registro de ordeño eliminado exitosamente'
    });
  } catch (error) {
    next(error);
  }
};
