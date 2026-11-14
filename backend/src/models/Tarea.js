const mongoose = require('mongoose');

const tareaSchema = new mongoose.Schema(
  {
    titulo: {
      type: String,
      required: [true, 'El título de la tarea es obligatorio'],
      trim: true
    },
    categoria: {
      type: String,
      default: 'Vacunación'
    },
    fechaLimite: {
      type: String,
      required: [true, 'La fecha límite es obligatoria'],
      trim: true
    },
    completada: {
      type: Boolean,
      default: false
    },
    prioridad: {
      type: String,
      default: 'Media'
    },
    responsable: {
      type: String,
      default: 'Operario de Campo',
      trim: true
    },
    descripcion: {
      type: String,
      default: null
    },
    esTareaEspecial: {
      type: Boolean,
      default: false
    },
    tipoTareaEspecial: {
      type: String,
      default: null
    },
    animalId: {
      type: String,
      default: null
    },
    animalCodigoNombre: {
      type: String,
      default: null
    }
  },
  {
    timestamps: true
  }
);

tareaSchema.virtual('id').get(function () {
  return this._id.toHexString();
});
tareaSchema.set('toJSON', { virtuals: true });

module.exports = mongoose.model('Tarea', tareaSchema);
