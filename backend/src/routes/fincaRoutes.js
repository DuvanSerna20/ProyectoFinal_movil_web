const express = require('express');
const router = express.Router();
const fincaController = require('../controllers/fincaController');

router.get('/', fincaController.obtenerFincas);
router.post('/', fincaController.crearFinca);

module.exports = router;
