const Potrero = require('../models/Potrero');
const Auditoria = require('../models/Auditoria');

// GET /api/potreros
exports.obtenerPotreros = async (req, res, next) => {
  try {
    const { search, estado } = req.query;
    const filter = {};

    if (search) {
      filter.$or = [
        { nombre: { $regex: search, $options: 'i' } },
        { tipoPasto: { $regex: search, $options: 'i' } }
      ];
    }
    if (estado && estado !== 'Todos') {
      filter.estado = estado;
    }

    const potreros = await Potrero.find(filter).sort({ nombre: 1 });
    res.status(200).json({
      success: true,
      count: potreros.length,
      data: potreros
    });
  } catch (error) {
    next(error);
  }
};

// GET /api/potreros/:id
exports.obtenerPotreroPorId = async (req, res, next) => {
  try {
    const potrero = await Potrero.findById(req.params.id);
    if (!potrero) {
      return res.status(404).json({
        success: false,
        message: 'Potrero no encontrado'
      });
    }
    res.status(200).json({
      success: true,
      data: potrero
    });
  } catch (error) {
    next(error);
  }
};

// POST /api/potreros
exports.crearPotrero = async (req, res, next) => {
  try {
    const nuevoPotrero = await Potrero.create(req.body);

    await Auditoria.create({
      modulo: 'Potreros',
      accion: 'Registro',
      descripcion: `Registro de lote: ${nuevoPotrero.nombre} (${nuevoPotrero.areaHa} ha)`,
      usuario: nuevoPotrero.registradoPor || 'Android Device',
      origen: 'App Android'
    });

    res.status(201).json({
      success: true,
      message: 'Potrero registrado exitosamente',
      data: nuevoPotrero
    });
  } catch (error) {
    next(error);
  }
};

// PUT /api/potreros/:id
exports.actualizarPotrero = async (req, res, next) => {
  try {
    // findByIdAndUpdate no ejecuta el middleware pre-save: recalculamos el área aquí
    const datos = { ...req.body };
    if (datos.largoMetros && datos.anchoMetros) {
      datos.areaM2 = datos.largoMetros * datos.anchoMetros;
      datos.areaHa = Number((datos.areaM2 / 10000).toFixed(2));
    }
    const potrero = await Potrero.findByIdAndUpdate(req.params.id, datos, {
      new: true,
      runValidators: true
    });

    if (!potrero) {
      return res.status(404).json({
        success: false,
        message: 'Potrero no encontrado'
      });
    }

    res.status(200).json({
      success: true,
      message: 'Potrero actualizado exitosamente',
      data: potrero
    });
  } catch (error) {
    next(error);
  }
};

// DELETE /api/potreros/:id
exports.eliminarPotrero = async (req, res, next) => {
  try {
    const potrero = await Potrero.findById(req.params.id);
    if (!potrero) {
      return res.status(404).json({
        success: false,
        message: 'Potrero no encontrado'
      });
    }

    await Potrero.findByIdAndDelete(req.params.id);

    await Auditoria.create({
      modulo: 'Potreros',
      accion: 'Eliminación',
      descripcion: `Potrero eliminado por el administrador: ${potrero.nombre}`,
      usuario: 'Administrador Web',
      origen: 'Panel Web'
    });

    res.status(200).json({
      success: true,
      message: `Potrero ${potrero.nombre} eliminado exitosamente`
    });
  } catch (error) {
    next(error);
  }
};

// POST /api/potreros/rotar (Mueve animales entre potreros)
exports.rotarGanado = async (req, res, next) => {
  try {
    const { origenId, destinoId, motivo, fecha } = req.body;
    const origen = await Potrero.findById(origenId);
    const destino = await Potrero.findById(destinoId);

    if (!origen || !destino) {
      return res.status(404).json({
        success: false,
        message: 'Potrero de origen o destino no encontrado'
      });
    }

    // Actualizar estados
    origen.estado = 'En descanso';
    destino.estado = 'Ocupado';
    destino.fechaUltimoCambio = fecha || new Date().toISOString().split('T')[0];
    
    // Mover animales en la base de datos si tienen asignado el potrero
    const Animal = require('../models/Animal');
    await Animal.updateMany({ potreroId: origenId }, { potreroId: destinoId });

    await origen.save();
    await destino.save();

    await Auditoria.create({
      modulo: 'Potreros / Rotación',
      accion: 'Rotación de Ganado',
      descripcion: `Rotación: de ${origen.nombre} a ${destino.nombre}. Motivo: ${motivo || 'Rotación de pasturas'}`,
      usuario: 'Android Operario',
      origen: 'App Android'
    });

    res.status(200).json({
      success: true,
      message: `Rotación completada: ganado trasladado a ${destino.nombre}`,
      data: { origen, destino }
    });
  } catch (error) {
    next(error);
  }
};

