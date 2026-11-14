import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FarmDataService, RegistroAlimentacion } from '../../services/farm-data.service';

@Component({
  selector: 'app-alimentacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './alimentacion.html',
  styleUrl: './alimentacion.css'
})
export class AlimentacionComponent {
  readonly farmService = inject(FarmDataService);

  readonly filtroTipo = signal<string>('Todos');
  readonly filtroEstado = signal<string>('Todos');
  readonly terminoBusqueda = signal<string>('');
  readonly registroAEliminar = signal<RegistroAlimentacion | null>(null);

  readonly registrosFiltrados = computed(() => {
    const tipo = this.filtroTipo();
    const estado = this.filtroEstado();
    const q = this.terminoBusqueda().toLowerCase().trim();

    return this.farmService.alimentacion().filter(item => {
      const coincideTipo = tipo === 'Todos' || item.tipoRegistro.includes(tipo);
      const coincideEstado = estado === 'Todos' ||
        (estado === 'Activos' && item.activo) ||
        (estado === 'Finalizados' && !item.activo) ||
        (estado === 'ConRetiro' && item.tieneRetiroLeche && item.activo);

      const coincideBusqueda = !q ||
        item.nombreProducto.toLowerCase().includes(q) ||
        item.objetivoLabel.toLowerCase().includes(q) ||
        item.categoriaDetalle.toLowerCase().includes(q);

      return coincideTipo && coincideEstado && coincideBusqueda;
    });
  });

  pedirConfirmacionEliminar(item: RegistroAlimentacion) {
    this.registroAEliminar.set(item);
  }

  cancelarEliminar() {
    this.registroAEliminar.set(null);
  }

  confirmarEliminacion() {
    const item = this.registroAEliminar();
    if (item) {
      this.farmService.eliminarAlimentacion(item.id);
      this.registroAEliminar.set(null);
    }
  }

  exportarPDF() {
    window.print();
  }
}
