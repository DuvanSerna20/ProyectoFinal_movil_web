const express = require('express');
const router = express.Router();
const usuarioController = require('../controllers/usuarioController');

router.get('/', usuarioController.obtenerUsuarios);
router.post('/', usuarioController.crearUsuario);
router.post('/login', usuarioController.loginUsuario);
router.delete('/:id', usuarioController.eliminarUsuario);

module.exports = router;
