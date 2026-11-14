const express = require('express');
const router = express.Router();
const alimentacionController = require('../controllers/alimentacionController');

router.get('/', alimentacionController.obtenerAlimentacion);
router.get('/:id', alimentacionController.obtenerAlimentacionPorId);
router.post('/', alimentacionController.crearAlimentacion);
router.put('/:id', alimentacionController.actualizarAlimentacion);
router.delete('/:id', alimentacionController.eliminarAlimentacion);

module.exports = router;
