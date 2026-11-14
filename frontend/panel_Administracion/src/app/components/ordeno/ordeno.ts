import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FarmDataService, RegistroOrdeno } from '../../services/farm-data.service';

@Component({
  selector: 'app-ordeno',
  imports: [CommonModule, FormsModule],
  templateUrl: './ordeno.html',
  styleUrl: './ordeno.css'
})
export class Ordeno {
  readonly farmService = inject(FarmDataService);
  private readonly router = inject(Router);

  readonly filtroJornada = signal<'Todas' | 'Mañana' | 'Tarde'>('Todas');
  readonly filtroFecha = signal<string>('');

  // Modal Eliminar
  readonly modalEliminarAbierto = signal(false);
  readonly registroAEliminar = signal<RegistroOrdeno | null>(null);

  readonly registrosFiltrados = computed(() => {
    const jornada = this.filtroJornada();
    const fecha = this.filtroFecha();

    return this.farmService.registrosOrdeno().filter(r => {
      const coincideJornada = jornada === 'Todas' || r.jornada === jornada;
      const coincideFecha = !fecha || r.fecha === fecha;
      return coincideJornada && coincideFecha;
    });
  });

  // Métricas de ordeño
  readonly totalLitrosHistorico = computed(() =>
    this.farmService.registrosOrdeno().reduce((sum, r) => sum + r.litros, 0)
  );

  readonly promedioLitrosPorJornada = computed(() => {
    const total = this.farmService.registrosOrdeno().length;
    if (!total) return 0;
    return Number((this.totalLitrosHistorico() / total).toFixed(1));
  });

  solicitarEliminar(reg: RegistroOrdeno) {
    this.registroAEliminar.set(reg);
    this.modalEliminarAbierto.set(true);
  }

  confirmarEliminar() {
    const reg = this.registroAEliminar();
    if (reg) {
      this.farmService.eliminarRegistroOrdeno(reg.id);
    }
    this.cancelarEliminar();
  }

  cancelarEliminar() {
    this.registroAEliminar.set(null);
    this.modalEliminarAbierto.set(false);
  }

  exportarPDF() {
    this.router.navigate(['/reportes'], { queryParams: { tipo: 'ordeno' } });
  }
}
