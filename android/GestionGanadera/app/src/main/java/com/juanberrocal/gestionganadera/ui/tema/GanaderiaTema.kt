package com.juanberrocal.gestionganadera.ui.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.juanberrocal.gestionganadera.R

/**
 * Tema Compose que reutiliza los colores de res/values/colors.xml, para que las pantallas
 * en Compose y las de XML se vean iguales. Todos los "surfaceContainer*" se fijan en blanco
 * o crema: así menús, hojas inferiores y diálogos no salen con el tono lila por defecto de M3.
 */
@Composable
fun GanaderiaTema(content: @Composable () -> Unit) {
    val blanco = colorResource(R.color.blanco)
    val fondo = colorResource(R.color.fondo)
    val superficieVariante = colorResource(R.color.superficie_variante)
    val verdeContenedor = colorResource(R.color.verde_contenedor)
    val verdeSobreContenedor = colorResource(R.color.verde_sobre_contenedor)

    val esquema = lightColorScheme(
        primary = colorResource(R.color.verde_primario),
        onPrimary = blanco,
        primaryContainer = verdeContenedor,
        onPrimaryContainer = verdeSobreContenedor,
        secondary = colorResource(R.color.tierra),
        onSecondary = blanco,
        // Los SegmentedButton y FilterChip seleccionados usan secondaryContainer: verde suave
        secondaryContainer = verdeContenedor,
        onSecondaryContainer = verdeSobreContenedor,
        tertiary = colorResource(R.color.oliva),
        background = fondo,
        onBackground = colorResource(R.color.texto),
        surface = colorResource(R.color.superficie),
        onSurface = colorResource(R.color.texto),
        surfaceVariant = superficieVariante,
        onSurfaceVariant = colorResource(R.color.texto_secundario),
        surfaceTint = blanco,
        outline = colorResource(R.color.contorno),
        outlineVariant = superficieVariante,
        error = colorResource(R.color.error),
        surfaceBright = blanco,
        surfaceDim = fondo,
        surfaceContainerLowest = blanco,
        surfaceContainerLow = fondo,      // ModalBottomSheet
        surfaceContainer = blanco,        // DropdownMenu / ExposedDropdownMenu
        surfaceContainerHigh = blanco,    // AlertDialog
        surfaceContainerHighest = superficieVariante
    )

    MaterialTheme(colorScheme = esquema, content = content)
}

/** Colores semánticos para las etiquetas de estado del ganado. */
object ColoresEstado {
    val ordenoFondo = Color(0xFFDCEFD8)
    val ordenoTexto = Color(0xFF1E5A2B)
    val secaFondo = Color(0xFFFCEBC7)
    val secaTexto = Color(0xFF7A4E00)
    val toroFondo = Color(0xFFDCE8F7)
    val toroTexto = Color(0xFF1F4E8C)
    val criaFondo = Color(0xFFF0DEC8)
    val criaTexto = Color(0xFF3A2410)
    val otroFondo = Color(0xFFEAE3D2)
    val otroTexto = Color(0xFF5A5E50)
    val cargadaFondo = Color(0xFFEDE3F5)
    val cargadaTexto = Color(0xFF5B3A7A)
}

/** Colores para alertas sanitarias, badges de tareas especiales y alimentación. */
object ColoresSanidad {
    val retiroFondo = Color(0xFFFBE3E1)
    val retiroTexto = Color(0xFF9A1C14)
    val ambarFondo = Color(0xFFFCEBC7)
    val ambarTexto = Color(0xFF7A4E00)
    val vacunaFondo = Color(0xFFDCE8F7)
    val vacunaTexto = Color(0xFF1F4E8C)
    val veterinariaFondo = Color(0xFFE0F0EE)
    val veterinariaTexto = Color(0xFF125A52)
    val purgaFondo = Color(0xFFF0DEC8)
    val purgaTexto = Color(0xFF6B3E14)
    val reproductivoFondo = Color(0xFFEDE3F5)
    val reproductivoTexto = Color(0xFF5B3A7A)
    val alimentoFondo = Color(0xFFDCEFD8)
    val alimentoTexto = Color(0xFF1E5A2B)
    val neutroFondo = Color(0xFFEAE3D2)
    val neutroTexto = Color(0xFF5A5E50)
}
