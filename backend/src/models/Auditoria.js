const mongoose = require('mongoose');

const auditoriaSchema = new mongoose.Schema(
  {
    modulo: {
      type: String,
      required: true
    },
    accion: {
      type: String,
      required: true
    },
    descripcion: {
      type: String,
      required: true
    },
    usuario: {
      type: String,
      default: 'Operario de Campo'
    },
    fecha: {
      type: String,
      default: () => new Date().toLocaleDateString('es-CO')
    },
    hora: {
      type: String,
      default: () => new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    },
    origen: {
      type: String,
      default: 'App Android'
    }
  },
  {
    timestamps: true
  }
);

auditoriaSchema.virtual('id').get(function () {
  return this._id.toHexString();
});
auditoriaSchema.set('toJSON', { virtuals: true });

module.exports = mongoose.model('Auditoria', auditoriaSchema);
