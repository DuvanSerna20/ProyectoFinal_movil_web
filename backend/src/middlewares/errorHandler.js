const errorHandler = (err, req, res, next) => {
  console.error('[Error Handler]', err);

  // Error de validación de Mongoose
  if (err.name === 'ValidationError') {
    const messages = Object.values(err.errors).map(val => val.message);
    return res.status(400).json({
      success: false,
      message: 'Error de validación',
      errors: messages
    });
  }

  // Error de clave duplicada en MongoDB (ej: código de animal o correo repetido)
  if (err.code === 11000) {
    const campo = Object.keys(err.keyValue)[0];
    const valor = err.keyValue[campo];
    return res.status(409).json({
      success: false,
      message: `El valor '${valor}' para el campo '${campo}' ya está registrado en el sistema.`,
      campo
    });
  }

  // CastError (ID inválido en MongoDB)
  if (err.name === 'CastError') {
    return res.status(400).json({
      success: false,
      message: `Identificador inválido: ${err.value}`
    });
  }

  // Error de Multer
  if (err.name === 'MulterError') {
    return res.status(400).json({
      success: false,
      message: `Error al subir archivo: ${err.message}`
    });
  }

  res.status(err.status || 500).json({
    success: false,
    message: err.message || 'Error interno del servidor'
  });
};

module.exports = errorHandler;
