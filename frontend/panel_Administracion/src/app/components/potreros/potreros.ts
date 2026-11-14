import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FarmDataService, Potrero } from '../../services/farm-data.service';

@Component({
  selector: 'app-potreros',
  imports: [CommonModule, FormsModule],
  templateUrl: './potreros.html',
  styleUrl: './potreros.css'
})
export class Potreros {
  readonly farmService = inject(FarmDataService);
  private readonly router = inject(Router);

  readonly searchTerm = signal('');
  readonly filtroEstado = signal<'Todos' | 'Ocupado' | 'En descanso'>('Todos');

  // Modal Confirmar Eliminación
  readonly modalEliminarAbierto = signal(false);
  readonly potreroAEliminar = signal<Potrero | null>(null);

  readonly potrerosFiltrados = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const estado = this.filtroEstado();

    return this.farmService.potreros().filter(p => {
      const coincideTerm = !term ||
        p.nombre.toLowerCase().includes(term) ||
        p.tipoPasto.toLowerCase().includes(term);

      const coincideEstado = estado === 'Todos' || p.estado === estado;

      return coincideTerm && coincideEstado;
    });
  });

  solicitarEliminar(potrero: Potrero) {
    this.potreroAEliminar.set(potrero);
    this.modalEliminarAbierto.set(true);
  }

  confirmarEliminar() {
    const p = this.potreroAEliminar();
    if (p) {
      this.farmService.eliminarPotrero(p.id);
    }
    this.cancelarEliminar();
  }

  cancelarEliminar() {
    this.potreroAEliminar.set(null);
    this.modalEliminarAbierto.set(false);
  }

  irARotacionConPotrero(potrero: Potrero) {
    this.router.navigate(['/rotacion'], {
      queryParams: {
        potreroId: potrero.id,
        ocupacion: potrero.diasOcupacion,
        descanso: potrero.diasDescanso
      }
    });
  }

  exportarPDF() {
    this.router.navigate(['/reportes'], { queryParams: { tipo: 'potreros' } });
  }
}
