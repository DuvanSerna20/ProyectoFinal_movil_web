const mongoose = require('mongoose');

const potreroSchema = new mongoose.Schema(
  {
    fincaId: {
      type: String,
      default: null
    },
    nombre: {
      type: String,
      required: [true, 'El nombre del potrero es obligatorio'],
      trim: true
    },
    largoMetros: {
      type: Number,
      default: null
    },
    anchoMetros: {
      type: Number,
      default: null
    },
    area: {
      type: Number,
      default: null
    },
    unidadArea: {
      type: String,
      default: 'ha'
    },
    areaM2: {
      type: Number,
      default: function () {
        if (this.largoMetros && this.anchoMetros) {
          return this.largoMetros * this.anchoMetros;
        }
        if (this.area) {
          return this.unidadArea === 'm2' ? this.area : this.area * 10000;
        }
        return 0;
      }
    },
    areaHa: {
      type: Number,
      default: function () {
        if (this.area && this.unidadArea === 'ha') return this.area;
        if (this.largoMetros && this.anchoMetros) {
          return Number(((this.largoMetros * this.anchoMetros) / 10000).toFixed(2));
        }
        return 0;
      }
    },
    estado: {
      type: String,
      default: 'En descanso'
    },
    condicionPasto: {
      type: String,
      default: null
    },
    diasOcupacion: {
      type: Number,
      default: 3
    },
    diasDescanso: {
      type: Number,
      default: 30
    },
    tipoPasto: {
      type: String,
      default: 'Brachiaria',
      trim: true
    },
    fechaUltimoCambio: {
      type: String,
      default: null
    },
    cantidadAnimales: {
      type: Number,
      default: 0
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

potreroSchema.virtual('id').get(function () {
  return this._id.toHexString();
});
potreroSchema.set('toJSON', { virtuals: true });

module.exports = mongoose.model('Potrero', potreroSchema);
