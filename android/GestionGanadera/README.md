# Gestión Ganadera — modelo funcional (Android)

Autor: Juan Pablo Berrocal · Kotlin · Material Design 3 · Room (SQLite) · Android 8.0+ (API 26)

## Cómo abrirlo
1. Descomprimir y abrir la carpeta `GestionGanadera` con **Android Studio** (Koala o superior, JDK 17 incluido).
2. Esperar la sincronización de Gradle (descarga Gradle 8.7 y las dependencias; requiere internet la primera vez).
3. Ejecutar (▶) en un emulador o celular con Android 8.0 o superior.
4. Pruebas unitarias de los cálculos: clic derecho en `CalculosTest` → *Run*.

## Estructura
```
app/src/main/java/com/juanberrocal/gestionganadera/
├── data/      Entidades (Animal, Potrero, Ordeno, Tarea), DAOs y AppDatabase (Room)
├── logica/    Calculos.kt (fórmulas de la propuesta) y Formato.kt (números/fechas en español)
└── ui/        MainActivity, GanaderiaViewModel y un paquete por pantalla
               (inicio, ganado, potreros, rotacion, ordeno, tareas)
app/src/main/res/  layouts XML, tema verde/tierra, íconos vectoriales, textos en español
app/src/test/      CalculosTest: casos de aceptación de la hoja HISTORIA DE USUARIO
```

## Trazabilidad con la tabla de requerimientos
| Módulo | REQ | Dónde está |
|---|---|---|
| Inicio | 1 resumen (cabezas, potreros, litros del día, pendientes) · 2 barra inferior | `InicioFragment`, `MainActivity` |
| Ganado | 1 registrar (código único) · 2 editar · 3 eliminar con confirmación · 4 listar · 5 total | `GanadoFragment`, `AnimalDao.contarCodigo` |
| Potreros | 1 registrar · 2 área m²/ha · 3 editar y recalcular · 4 eliminar · 5 listar + total · 6 cambiar estado | `PotrerosFragment`, `Calculos` |
| Rotación | 1 cada cuánto y próxima fecha · 2 potreros necesarios · 3 tiempo total | `RotacionFragment`, `Calculos` |
| Ordeño | 1 registrar (litros > 0) · 2 editar · 3 eliminar · 4 orden descendente · 5 total del día | `OrdenoFragment`, `OrdenoDao` |
| Tareas | 1 registrar (pendiente) · 2 orden por fecha · 3 completar · 4 editar · 5 eliminar | `TareasFragment`, `TareaDao` |

No funcionales: API 26 (RNF-1), Kotlin (2), Room (3), sin internet (4), persistencia (5), Material con botones grandes y barra inferior (6), colores verdes y tierra (7), mensajes en español (8), validación de entradas (9).

## Decisiones que conviene revisar
- **Potreros necesarios**: se usa `ceil(descanso / ocupación) + 1`. Con los datos del ejemplo (30 y 3) da 11, igual que la propuesta; si la división no es exacta se redondea hacia arriba porque no existen fracciones de potrero.
- **Rotación**: la fecha de ingreso del ganado se usa solo para el cálculo y no se guarda, porque la tabla Potrero de la propuesta no tiene ese campo.
- **Código de animal**: no se repite sin importar mayúsculas/minúsculas.
- **Estado inicial** de un potrero nuevo: *En descanso*.
- La pantalla de Rotación se abre desde Inicio o desde el botón *Rotación* de cada potrero (la barra inferior mantiene las 5 opciones del documento).
