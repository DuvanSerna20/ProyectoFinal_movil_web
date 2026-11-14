const Usuario = require('../models/Usuario');
const Auditoria = require('../models/Auditoria');

// GET /api/usuarios
exports.obtenerUsuarios = async (req, res, next) => {
  try {
    const { rol, search } = req.query;
    const filter = {};

    if (rol && rol !== 'Todos') {
      filter.rol = rol;
    }
    if (search) {
      filter.$or = [
        { nombre: { $regex: search, $options: 'i' } },
        { correo: { $regex: search, $options: 'i' } },
        { finca: { $regex: search, $options: 'i' } }
      ];
    }

    const usuarios = await Usuario.find(filter).sort({ createdAt: -1 });

    res.status(200).json({
      success: true,
      count: usuarios.length,
      data: usuarios
    });
  } catch (error) {
    next(error);
  }
};

// POST /api/usuarios (Crear cuenta desde Android Onboarding)
exports.crearUsuario = async (req, res, next) => {
  try {
    const { nombre, finca, correo, contrasena, rol, dispositivo } = req.body;

    const existe = await Usuario.findOne({ correo });
    if (existe) {
      return res.status(409).json({
        success: false,
        message: 'El correo electrónico ya está registrado.'
      });
    }

    const nuevoUsuario = await Usuario.create({
      nombre,
      finca: finca || 'Finca La Esperanza',
      correo,
      contrasena,
      rol: rol || 'Operario de Campo',
      dispositivo: dispositivo || 'Android Mobile App'
    });

    await Auditoria.create({
      modulo: 'Usuarios',
      accion: 'Registro',
      descripcion: `Nueva cuenta móvil creada: ${nuevoUsuario.nombre} (${nuevoUsuario.rol})`,
      usuario: nuevoUsuario.nombre,
      origen: 'App Android'
    });

    res.status(201).json({
      success: true,
      message: 'Cuenta de usuario creada exitosamente',
      data: nuevoUsuario
    });
  } catch (error) {
    next(error);
  }
};

// POST /api/usuarios/login (Iniciar sesión desde Android)
exports.loginUsuario = async (req, res, next) => {
  try {
    const { correo, contrasena } = req.body;

    if (!correo || !contrasena) {
      return res.status(400).json({
        success: false,
        message: 'Por favor ingrese correo y contraseña'
      });
    }

    const usuario = await Usuario.findOne({ correo });
    if (!usuario) {
      return res.status(401).json({
        success: false,
        message: 'Credenciales inválidas. Usuario no encontrado.'
      });
    }

    const esValida = await usuario.compararContrasena(contrasena);
    if (!esValida) {
      return res.status(401).json({
        success: false,
        message: 'Credenciales inválidas. Contraseña incorrecta.'
      });
    }

    usuario.ultimoAcceso = new Date().toLocaleString('es-CO');
    await usuario.save();

    res.status(200).json({
      success: true,
      message: 'Inicio de sesión exitoso',
      data: usuario
    });
  } catch (error) {
    next(error);
  }
};

// DELETE /api/usuarios/:id (Borrado administrativo)
exports.eliminarUsuario = async (req, res, next) => {
  try {
    const usuario = await Usuario.findById(req.params.id);
    if (!usuario) {
      return res.status(404).json({
        success: false,
        message: 'Usuario no encontrado'
      });
    }

    // Regla de seguridad: no permitir eliminar la cuenta de Super Administrador
    if (usuario.rol === 'Administrador' && usuario.correo === 'juan@correo.com') {
      return res.status(403).json({
        success: false,
        message: 'No está permitido eliminar la cuenta del Administrador Principal.'
      });
    }

    await Usuario.findByIdAndDelete(req.params.id);

    await Auditoria.create({
      modulo: 'Usuarios',
      accion: 'Eliminación',
      descripcion: `Cuenta de usuario eliminada por el administrador: ${usuario.nombre} (${usuario.correo})`,
      usuario: 'Administrador Web',
      origen: 'Panel Web'
    });

    res.status(200).json({
      success: true,
      message: `Usuario ${usuario.nombre} eliminado satisfactoriamente`
    });
  } catch (error) {
    next(error);
  }
};
