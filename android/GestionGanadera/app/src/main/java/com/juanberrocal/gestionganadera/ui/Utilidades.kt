package com.juanberrocal.gestionganadera.ui

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.juanberrocal.gestionganadera.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun Fragment.avisar(@StringRes mensaje: Int) {
    view?.let { Snackbar.make(it, mensaje, Snackbar.LENGTH_SHORT).show() }
}

fun Fragment.avisar(mensaje: String) {
    view?.let { Snackbar.make(it, mensaje, Snackbar.LENGTH_LONG).show() }
}

fun Fragment.confirmarEliminacion(mensaje: String, alConfirmar: () -> Unit) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.eliminar_titulo)
        .setMessage(mensaje)
        .setPositiveButton(R.string.eliminar_si) { _, _ -> alConfirmar() }
        .setNegativeButton(R.string.cancelar, null)
        .show()
}

fun Fragment.elegirFecha(inicial: LocalDate, alElegir: (LocalDate) -> Unit) {
    val seleccion = inicial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val selector = MaterialDatePicker.Builder.datePicker()
        .setTitleText(R.string.elegir_fecha)
        .setSelection(seleccion)
        .build()
    selector.addOnPositiveButtonClickListener { milis ->
        alElegir(Instant.ofEpochMilli(milis).atZone(ZoneOffset.UTC).toLocalDate())
    }
    selector.show(childFragmentManager, "selector_fecha")
}
