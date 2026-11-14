const express = require('express');
const router = express.Router();
const potreroController = require('../controllers/potreroController');

router.get('/', potreroController.obtenerPotreros);
router.get('/:id', potreroController.obtenerPotreroPorId);
router.post('/', potreroController.crearPotrero);
router.post('/rotar', potreroController.rotarGanado);
router.put('/:id', potreroController.actualizarPotrero);
router.delete('/:id', potreroController.eliminarPotrero);

module.exports = router;
