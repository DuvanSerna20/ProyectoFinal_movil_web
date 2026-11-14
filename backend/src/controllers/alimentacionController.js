const Alimentacion = require('../models/Alimentacion');
const Auditoria = require('../models/Auditoria');

// GET /api/alimentacion
exports.obtenerAlimentacion = async (req, res, next) => {
  try {
    const { tipo, activo, animalId } = req.query;
    const filter = {};

    if (tipo) filter.tipoRegistro = tipo;
    if (activo !== undefined) filter.activo = activo === 'true';
    if (animalId) filter.animalId = animalId;

    const registros = await Alimentacion.find(filter).sort({ createdAt: -1 });

    res.status(200).json({
      success: true,
      count: registros.length,
      data: registros
    });
  } catch (error) {
    next(error);
  }
};

// GET /api/alimentacion/:id
exports.obtenerAlimentacionPorId = async (req, res, next) => {
  try {
    const registro = await Alimentacion.findById(req.params.id);
    if (!registro) {
      return res.status(404).json({
        success: false,
        message: 'Registro de alimentación/medicamento no encontrado'
      });
    }
    res.status(200).json({
      success: true,
      data: registro
    });
  } catch (error) {
    next(error);
  }
};

// POST /api/alimentacion
exports.crearAlimentacion = async (req, res, next) => {
  try {
    const nuevoRegistro = await Alimentacion.create(req.body);

    await Auditoria.create({
      modulo: 'Alimentación / Medicamentos',
      accion: 'Registro',
      descripcion: `Insumo registrado: ${nuevoRegistro.nombreProducto} (${nuevoRegistro.tipoRegistro} - ${nuevoRegistro.objetivoLabel})`,
      usuario: nuevoRegistro.registradoPor || 'Android Device',
      origen: 'App Android'
    });

    res.status(201).json({
      success: true,
      message: 'Registro creado exitosamente',
      data: nuevoRegistro
    });
  } catch (error) {
    next(error);
  }
};

// PUT /api/alimentacion/:id
exports.actualizarAlimentacion = async (req, res, next) => {
  try {
    const registro = await Alimentacion.findByIdAndUpdate(req.params.id, req.body, {
      new: true,
      runValidators: true
    });

    if (!registro) {
      return res.status(404).json({
        success: false,
        message: 'Registro no encontrado'
      });
    }

    res.status(200).json({
      success: true,
      message: 'Registro actualizado exitosamente',
      data: registro
    });
  } catch (error) {
    next(error);
  }
};

// DELETE /api/alimentacion/:id
exports.eliminarAlimentacion = async (req, res, next) => {
  try {
    const registro = await Alimentacion.findById(req.params.id);
    if (!registro) {
      return res.status(404).json({
        success: false,
        message: 'Registro no encontrado'
      });
    }

    await registro.deleteOne();

    await Auditoria.create({
      modulo: 'Alimentación / Medicamentos',
      accion: 'Eliminación',
      descripcion: `Eliminado: ${registro.nombreProducto} (${registro.objetivoLabel})`,
      usuario: 'Panel Web / Android',
      origen: 'API'
    });

    res.status(200).json({
      success: true,
      message: 'Registro eliminado satisfactoriamente'
    });
  } catch (error) {
    next(error);
  }
};
