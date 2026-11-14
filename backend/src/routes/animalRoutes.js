const express = require('express');
const router = express.Router();
const animalController = require('../controllers/animalController');
const upload = require('../middlewares/upload');

router.get('/', animalController.obtenerAnimales);
router.get('/:id', animalController.obtenerAnimalPorId);
router.post('/', upload.single('foto'), animalController.crearAnimal);
router.put('/:id', upload.single('foto'), animalController.actualizarAnimal);
router.delete('/:id', animalController.eliminarAnimal);

module.exports = router;
