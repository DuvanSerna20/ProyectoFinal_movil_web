const mongoose = require('mongoose');

const alimentacionSchema = new mongoose.Schema(
  {
    animalId: {
      type: String,
      default: null
    },
    grupoProposito: {
      type: String,
      default: null
    },
    objetivoLabel: {
      type: String,
      required: [true, 'El objetivo o destinatario es obligatorio'],
      trim: true
    },
    tipoRegistro: {
      type: String,
      required: [true, 'El tipo de registro es obligatorio'],
      trim: true
    },
    categoriaDetalle: {
      type: String,
      required: [true, 'La categoría del insumo es obligatoria'],
      trim: true
    },
    nombreProducto: {
      type: String,
      required: [true, 'El nombre del producto es obligatorio'],
      trim: true
    },
    cantidad: {
      type: Number,
      required: [true, 'La cantidad es obligatoria'],
      min: [0, 'La cantidad no puede ser negativa']
    },
    unidad: {
      type: String,
      required: [true, 'La unidad es obligatoria'],
      trim: true
    },
    frecuencia: {
      type: String,
      required: [true, 'La frecuencia es obligatoria'],
      trim: true
    },
    tieneRetiroLeche: {
      type: Boolean,
      default: false
    },
    diasRetiro: {
      type: Number,
      default: 0
    },
    activo: {
      type: Boolean,
      default: true
    },
    fechaInicio: {
      type: String,
      default: () => new Date().toISOString().split('T')[0]
    },
    fechaFin: {
      type: String,
      default: null
    },
    registradoPor: {
      type: String,
      default: 'Operario Android'
    }
  },
  {
    timestamps: true
  }
);

alimentacionSchema.virtual('id').get(function () {
  return this._id.toHexString();
});
alimentacionSchema.set('toJSON', { virtuals: true });

module.exports = mongoose.model('Alimentacion', alimentacionSchema);
