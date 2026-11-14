import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FarmDataService, TareaCuidado } from '../../services/farm-data.service';

@Component({
  selector: 'app-tareas',
  imports: [CommonModule, FormsModule],
  templateUrl: './tareas.html',
  styleUrl: './tareas.css'
})
export class Tareas {
  readonly farmService = inject(FarmDataService);
  private readonly router = inject(Router);

  readonly filtroEstado = signal<'Todas' | 'Pendientes' | 'Completadas'>('Todas');
  readonly filtroCategoria = signal<string>('Todas');

  readonly modalEliminarAbierto = signal(false);
  readonly tareaAEliminar = signal<TareaCuidado | null>(null);

  readonly categorias = ['Todas', 'Vacunación', 'Desparasitación', 'Mantenimiento', 'Alimentación'];

  readonly tareasFiltradas = computed(() => {
    const estado = this.filtroEstado();
    const cat = this.filtroCategoria();

    return this.farmService.tareas().filter(t => {
      const coincideEstado =
        estado === 'Todas' ||
        (estado === 'Pendientes' && !t.completada) ||
        (estado === 'Completadas' && t.completada);

      const coincideCat = cat === 'Todas' || t.categoria === cat;

      return coincideEstado && coincideCat;
    });
  });

  solicitarEliminar(tarea: TareaCuidado) {
    this.tareaAEliminar.set(tarea);
    this.modalEliminarAbierto.set(true);
  }

  confirmarEliminar() {
    const t = this.tareaAEliminar();
    if (t) {
      this.farmService.eliminarTarea(t.id);
    }
    this.cancelarEliminar();
  }

  cancelarEliminar() {
    this.tareaAEliminar.set(null);
    this.modalEliminarAbierto.set(false);
  }

  exportarPDF() {
    this.router.navigate(['/reportes'], { queryParams: { tipo: 'tareas' } });
  }
}
