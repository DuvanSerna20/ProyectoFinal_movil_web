const Finca = require('../models/Finca');
const Auditoria = require('../models/Auditoria');

// GET /api/fincas
exports.obtenerFincas = async (req, res, next) => {
  try {
    const fincas = await Finca.find().sort({ createdAt: -1 });
    res.status(200).json({
      success: true,
      count: fincas.length,
      data: fincas
    });
  } catch (error) {
    next(error);
  }
};

// POST /api/fincas
exports.crearFinca = async (req, res, next) => {
  try {
    const nuevaFinca = await Finca.create(req.body);

    await Auditoria.create({
      modulo: 'Fincas',
      accion: 'Registro',
      descripcion: `Finca registrada: ${nuevaFinca.nombre} (${nuevaFinca.departamento})`,
      usuario: nuevaFinca.registradoPor || 'Android Device',
      origen: 'App Android'
    });

    res.status(201).json({
      success: true,
      message: 'Finca registrada exitosamente',
      data: nuevaFinca
    });
  } catch (error) {
    next(error);
  }
};
