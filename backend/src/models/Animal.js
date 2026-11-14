const mongoose = require('mongoose');

const animalSchema = new mongoose.Schema(
  {
    codigo: {
      type: String,
      required: [true, 'El código del animal es obligatorio'],
      unique: true,
      trim: true,
      uppercase: true
    },
    nombre: {
      type: String,
      required: [true, 'El nombre del animal es obligatorio'],
      trim: true
    },
    raza: {
      type: String,
      required: [true, 'La raza es obligatoria'],
      trim: true
    },
    sexo: {
      type: String,
      enum: {
        values: ['Hembra', 'Macho'],
        message: 'El sexo debe ser Hembra o Macho'
      },
      required: [true, 'El sexo es obligatorio']
    },
    pesoKg: {
      type: Number,
      min: [0, 'El peso no puede ser negativo'],
      default: null
    },
    estado: {
      type: String,
      default: 'En producción'
    },
    especie: {
      type: String,
      default: 'Bovino'
    },
    categoria: {
      type: String,
      default: null
    },
    proposito: {
      type: String,
      default: null
    },
    estadoReproductivo: {
      type: String,
      default: null
    },
    produccionLitrosDia: {
      type: Number,
      default: null
    },
    fincaId: {
      type: String,
      default: null
    },
    potreroId: {
      type: String,
      default: null
    },
    fotoUrl: {
      type: String,
      default: ''
    },
    registradoPor: {
      type: String,
      default: 'Juan Pablo Berrocal (Android)'
    },
    fechaRegistro: {
      type: String,
      default: () => new Date().toLocaleDateString('es-CO')
    }
  },
  {
    timestamps: true
  }
);

animalSchema.virtual('id').get(function () {
  return this._id.toHexString();
});
animalSchema.set('toJSON', { virtuals: true });

module.exports = mongoose.model('Animal', animalSchema);
