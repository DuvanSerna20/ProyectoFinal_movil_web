require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const connectDB = require('./config/db');
const errorHandler = require('./middlewares/errorHandler');

// Rutas
const animalRoutes = require('./routes/animalRoutes');
const potreroRoutes = require('./routes/potreroRoutes');
const ordenoRoutes = require('./routes/ordenoRoutes');
const tareaRoutes = require('./routes/tareaRoutes');
const usuarioRoutes = require('./routes/usuarioRoutes');
const auditoriaRoutes = require('./routes/auditoriaRoutes');

const alimentacionRoutes = require('./routes/alimentacionRoutes');
const fincaRoutes = require('./routes/fincaRoutes');

const app = express();
const PORT = process.env.PORT || 3000;

// Conectar a MongoDB
connectDB();

// Middlewares globales
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Logger para depurar peticiones de Android y Angular
app.use((req, res, next) => {
  console.log(`[HTTP ${new Date().toLocaleTimeString()}] ${req.method} ${req.originalUrl}`);
  next();
});

// Servir archivos subidos (Multer)
app.use('/uploads', express.static(path.join(__dirname, '../uploads')));

// Ruta de estado de salud
app.get('/api/health', (req, res) => {
  res.status(200).json({
    status: 'OK',
    serverTime: new Date().toISOString(),
    service: 'Gestión Ganadera Backend API',
    version: '1.0.0'
  });
});

// Montar rutas de la API REST
app.use('/api/animales', animalRoutes);
app.use('/api/potreros', potreroRoutes);
app.use('/api/ordeno', ordenoRoutes);
app.use('/api/tareas', tareaRoutes);
app.use('/api/usuarios', usuarioRoutes);
app.use('/api/auditoria', auditoriaRoutes);

app.use('/api/alimentacion', alimentacionRoutes);
app.use('/api/fincas', fincaRoutes);

// Manejo de rutas no encontradas (404)
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: `Ruta no encontrada: ${req.method} ${req.originalUrl}`
  });
});

// Middleware de manejo de errores
app.use(errorHandler);

// Iniciar servidor
app.listen(PORT, () => {
  console.log(`=======================================================`);
  console.log(` Servidor de Gestión Ganadera activo en puerto: ${PORT}`);
  console.log(` API REST: http://localhost:${PORT}/api/health`);
  console.log(` MongoDB: ${process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/gestion_ganadera'}`);
  console.log(`=======================================================`);
});

module.exports = app;
