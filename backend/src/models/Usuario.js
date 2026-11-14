const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const usuarioSchema = new mongoose.Schema(
  {
    nombre: {
      type: String,
      required: [true, 'El nombre completo es obligatorio'],
      trim: true
    },
    finca: {
      type: String,
      default: 'Finca La Esperanza',
      trim: true
    },
    correo: {
      type: String,
      required: [true, 'El correo electrónico es obligatorio'],
      unique: true,
      lowercase: true,
      trim: true,
      match: [/\S+@\S+\.\S+/, 'Por favor ingrese un correo válido']
    },
    contrasena: {
      type: String,
      required: [true, 'La contraseña es obligatoria'],
      minlength: [6, 'La contraseña debe tener mínimo 6 caracteres']
    },
    rol: {
      type: String,
      enum: ['Administrador', 'Operario de Campo', 'Vaquero'],
      default: 'Operario de Campo'
    },
    dispositivo: {
      type: String,
      default: 'Android Device'
    },
    estado: {
      type: String,
      enum: ['Activo', 'Inactivo'],
      default: 'Activo'
    },
    fechaRegistro: {
      type: String,
      default: () => new Date().toLocaleDateString('es-CO')
    },
    ultimoAcceso: {
      type: String,
      default: () => new Date().toLocaleString('es-CO')
    }
  },
  {
    timestamps: true
  }
);

// Hash de contraseña pre-save si fue modificada
usuarioSchema.pre('save', async function (next) {
  if (!this.isModified('contrasena')) return next();
  try {
    const salt = await bcrypt.genSalt(10);
    this.contrasena = await bcrypt.hash(this.contrasena, salt);
    next();
  } catch (err) {
    next(err);
  }
});

// Método para comparar contraseña
usuarioSchema.methods.compararContrasena = async function (candidata) {
  return bcrypt.compare(candidata, this.contrasena);
};

usuarioSchema.virtual('id').get(function () {
  return this._id.toHexString();
});
usuarioSchema.set('toJSON', {
  virtuals: true,
  transform: function (doc, ret) {
    delete ret.contrasena;
    return ret;
  }
});

module.exports = mongoose.model('Usuario', usuarioSchema);
