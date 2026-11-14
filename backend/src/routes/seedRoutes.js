const express = require('express');
const router = express.Router();
const seedController = require('../controllers/seedController');

router.post('/', seedController.poblarBaseDeDatos);
router.get('/', seedController.poblarBaseDeDatos);

module.exports = router;
