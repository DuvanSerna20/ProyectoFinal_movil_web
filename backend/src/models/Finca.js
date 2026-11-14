const mongoose = require('mongoose');

const fincaSchema = new mongoose.Schema(
  {
    nombre: {
      type: String,
      required: [true, 'El nombre de la finca es obligatorio'],
      trim: true
    },
    departamento: {
      type: String,
      required: [true, 'El departamento es obligatorio'],
      trim: true
    },
    municipio: {
      type: String,
      default: '',
      trim: true
    },
    pisoTermico: {
      type: String,
      default: null,
      trim: true
    },
    propietario: {
      type: String,
      default: 'Juan Pablo Berrocal'
    },
    registradoPor: {
      type: String,
      default: 'Android App'
    }
  },
  {
    timestamps: true
  }
);

fincaSchema.virtual('id').get(function () {
  return this._id.toHexString();
});
fincaSchema.set('toJSON', { virtuals: true });

module.exports = mongoose.model('Finca', fincaSchema);
