const Tarea = require('../models/Tarea');
const Auditoria = require('../models/Auditoria');

// GET /api/tareas
exports.obtenerTareas = async (req, res, next) => {
  try {
    const { estado, categoria } = req.query;
    const filter = {};

    if (estado === 'Pendientes') filter.completada = false;
    if (estado === 'Completadas') filter.completada = true;
    if (categoria && categoria !== 'Todas') filter.categoria = categoria;

    const tareas = await Tarea.find(filter).sort({ completada: 1, fechaLimite: 1 });

    res.status(200).json({
      success: true,
      count: tareas.length,
      data: tareas
    });
  } catch (error) {
    next(error);
  }
};

// POST /api/tareas
exports.crearTarea = async (req, res, next) => {
  try {
    const nuevaTarea = await Tarea.create(req.body);

    await Auditoria.create({
      modulo: 'Tareas',
      accion: 'Registro',
      descripcion: `Nueva tarea programada: ${nuevaTarea.titulo} (${nuevaTarea.categoria})`,
      usuario: nuevaTarea.responsable || 'Android Device',
      origen: 'App Android'
    });

    res.status(201).json({
      success: true,
      message: 'Tarea creada exitosamente',
      data: nuevaTarea
    });
  } catch (error) {
    next(error);
  }
};

// PUT /api/tareas/:id  (la app Android edita descripción, fecha y estado)
exports.actualizarTarea = async (req, res, next) => {
  try {
    const tarea = await Tarea.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
    if (!tarea) {
      return res.status(404).json({ success: false, message: 'Tarea no encontrada' });
    }
    res.status(200).json({ success: true, message: 'Tarea actualizada exitosamente', data: tarea });
  } catch (error) {
    next(error);
  }
};

// PATCH /api/tareas/:id/toggle
exports.toggleTarea = async (req, res, next) => {
  try {
    const tarea = await Tarea.findById(req.params.id);
    if (!tarea) {
      return res.status(404).json({
        success: false,
        message: 'Tarea no encontrada'
      });
    }

    tarea.completada = !tarea.completada;
    await tarea.save();

    await Auditoria.create({
      modulo: 'Tareas',
      accion: tarea.completada ? 'Completado' : 'Actualización',
      descripcion: `Tarea marcada como ${tarea.completada ? 'Completada' : 'Pendiente'}: ${tarea.titulo}`,
      usuario: 'Panel Web / Android',
      origen: 'App Android'
    });

    res.status(200).json({
      success: true,
      message: `Tarea ${tarea.completada ? 'completada' : 'reabierta'} exitosamente`,
      data: tarea
    });
  } catch (error) {
    next(error);
  }
};

// DELETE /api/tareas/:id
exports.eliminarTarea = async (req, res, next) => {
  try {
    const tarea = await Tarea.findById(req.params.id);
    if (!tarea) {
      return res.status(404).json({
        success: false,
        message: 'Tarea no encontrada'
      });
    }

    await Tarea.findByIdAndDelete(req.params.id);

    await Auditoria.create({
      modulo: 'Tareas',
      accion: 'Eliminación',
      descripcion: `Tarea eliminada por el administrador: ${tarea.titulo}`,
      usuario: 'Administrador Web',
      origen: 'Panel Web'
    });

    res.status(200).json({
      success: true,
      message: 'Tarea eliminada exitosamente'
    });
  } catch (error) {
    next(error);
  }
};
