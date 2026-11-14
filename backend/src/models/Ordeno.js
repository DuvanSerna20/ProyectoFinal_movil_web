const mongoose = require('mongoose');

const ordenoSchema = new mongoose.Schema(
  {
    fecha: {
      type: String,
      required: [true, 'La fecha del ordeño es obligatoria'],
      trim: true
    },
    jornada: {
      type: String,
      required: [true, 'La jornada es obligatoria']
    },
    litros: {
      type: Number,
      required: [true, 'La cantidad de litros es obligatoria'],
      min: [0.1, 'Los litros deben ser un número mayor a 0']
    },
    animalId: {
      type: String,
      default: null
    },
    animalCodigo: {
      type: String,
      default: null
    },
    animalNombre: {
      type: String,
      default: null
    },
    responsable: {
      type: String,
      required: [true, 'El responsable del ordeño es obligatorio'],
      trim: true
    },
    horaRegistro: {
      type: String,
      default: () => new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    },
    destinoLeche: {
      type: String,
      default: 'Venta / Tanque'
    }
  },
  {
    timestamps: true
  }
);

ordenoSchema.virtual('id').get(function () {
  return this._id.toHexString();
});
ordenoSchema.set('toJSON', { virtuals: true });

module.exports = mongoose.model('Ordeno', ordenoSchema);
