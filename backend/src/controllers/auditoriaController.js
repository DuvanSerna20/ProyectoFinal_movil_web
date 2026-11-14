const Auditoria = require('../models/Auditoria');

// GET /api/auditoria
exports.obtenerAuditoria = async (req, res, next) => {
  try {
    const { modulo, origen, limit = 50 } = req.query;
    const filter = {};

    if (modulo && modulo !== 'Todos') {
      filter.modulo = modulo;
    }
    if (origen) {
      filter.origen = origen;
    }

    const logs = await Auditoria.find(filter)
      .sort({ createdAt: -1 })
      .limit(Number(limit));

    res.status(200).json({
      success: true,
      count: logs.length,
      data: logs
    });
  } catch (error) {
    next(error);
  }
};

// POST /api/auditoria
exports.crearLogAuditoria = async (req, res, next) => {
  try {
    const log = await Auditoria.create(req.body);
    res.status(201).json({
      success: true,
      data: log
    });
  } catch (error) {
    next(error);
  }
};
