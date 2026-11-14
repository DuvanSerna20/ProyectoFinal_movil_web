const express = require('express');
const router = express.Router();
const ordenoController = require('../controllers/ordenoController');

router.get('/', ordenoController.obtenerRegistrosOrdeno);
router.get('/resumen', ordenoController.obtenerResumenHoy);
router.post('/', ordenoController.crearRegistroOrdeno);
router.put('/:id', ordenoController.actualizarRegistroOrdeno);
router.delete('/:id', ordenoController.eliminarRegistroOrdeno);

module.exports = router;
