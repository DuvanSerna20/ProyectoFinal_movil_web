const express = require('express');
const router = express.Router();
const tareaController = require('../controllers/tareaController');

router.get('/', tareaController.obtenerTareas);
router.post('/', tareaController.crearTarea);
router.put('/:id', tareaController.actualizarTarea);
router.patch('/:id/toggle', tareaController.toggleTarea);
router.delete('/:id', tareaController.eliminarTarea);

module.exports = router;
