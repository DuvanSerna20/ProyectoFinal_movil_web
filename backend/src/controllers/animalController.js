const Animal = require('../models/Animal');
const Auditoria = require('../models/Auditoria');

// GET /api/animales
exports.obtenerAnimales = async (req, res, next) => {
  try {
    const { search, sexo, raza, estado } = req.query;
    const filter = {};

    if (search) {
      filter.$or = [
        { nombre: { $regex: search, $options: 'i' } },
        { codigo: { $regex: search, $options: 'i' } },
        { raza: { $regex: search, $options: 'i' } }
      ];
    }
    if (sexo && sexo !== 'Todos') {
      filter.sexo = sexo;
    }
    if (raza && raza !== 'Todas') {
      filter.raza = raza;
    }
    if (estado) {
      filter.estado = estado;
    }

    const animales = await Animal.find(filter).sort({ createdAt: -1 });
    res.status(200).json({
      success: true,
      count: animales.length,
      data: animales
    });
  } catch (error) {
    next(error);
  }
};

// GET /api/animales/:id
exports.obtenerAnimalPorId = async (req, res, next) => {
  try {
    const animal = await Animal.findById(req.params.id);
    if (!animal) {
      return res.status(404).json({
        success: false,
        message: 'Animal no encontrado'
      });
    }
    res.status(200).json({
      success: true,
      data: animal
    });
  } catch (error) {
    next(error);
  }
};

// POST /api/animales
exports.crearAnimal = async (req, res, next) => {
  try {
    const data = { ...req.body };
    if (req.file) {
      data.fotoUrl = `/uploads/${req.file.filename}`;
    }

    const nuevoAnimal = await Animal.create(data);

    // Registro automático en Auditoría
    await Auditoria.create({
      modulo: 'Ganado',
      accion: 'Registro',
      descripcion: `Alta de animal: ${nuevoAnimal.nombre} (${nuevoAnimal.codigo}, ${nuevoAnimal.raza})`,
      usuario: nuevoAnimal.registradoPor || 'Android Device',
      origen: 'App Android'
    });

    res.status(201).json({
      success: true,
      message: 'Animal registrado exitosamente',
      data: nuevoAnimal
    });
  } catch (error) {
    next(error);
  }
};

// PUT /api/animales/:id
exports.actualizarAnimal = async (req, res, next) => {
  try {
    const data = { ...req.body };
    if (req.file) {
      data.fotoUrl = `/uploads/${req.file.filename}`;
    }

    const animal = await Animal.findByIdAndUpdate(req.params.id, data, {
      new: true,
      runValidators: true
    });

    if (!animal) {
      return res.status(404).json({
        success: false,
        message: 'Animal no encontrado'
      });
    }

    await Auditoria.create({
      modulo: 'Ganado',
      accion: 'Actualización',
      descripcion: `Actualización de datos: ${animal.nombre} (${animal.codigo})`,
      usuario: 'Panel Web / Android',
      origen: 'App Android'
    });

    res.status(200).json({
      success: true,
      message: 'Animal actualizado exitosamente',
      data: animal
    });
  } catch (error) {
    next(error);
  }
};

// DELETE /api/animales/:id
exports.eliminarAnimal = async (req, res, next) => {
  try {
    const animal = await Animal.findById(req.params.id);
    if (!animal) {
      return res.status(404).json({
        success: false,
        message: 'Animal no encontrado'
      });
    }

    await Animal.findByIdAndDelete(req.params.id);

    // Log de eliminación administrativa
    await Auditoria.create({
      modulo: 'Ganado',
      accion: 'Eliminación',
      descripcion: `Animal retirado/eliminado por el administrador: ${animal.nombre} (${animal.codigo})`,
      usuario: 'Administrador Web',
      origen: 'Panel Web'
    });

    res.status(200).json({
      success: true,
      message: `Animal ${animal.nombre} eliminado satisfactoriamente`
    });
  } catch (error) {
    next(error);
  }
};
