import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FarmDataService, Animal } from '../../services/farm-data.service';

@Component({
  selector: 'app-ganado',
  imports: [CommonModule, FormsModule],
  templateUrl: './ganado.html',
  styleUrl: './ganado.css'
})
export class Ganado {
  readonly farmService = inject(FarmDataService);
  private readonly router = inject(Router);

  // Filtros y búsquedas
  readonly searchTerm = signal('');
  readonly filtroSexo = signal<'Todos' | 'Hembra' | 'Macho'>('Todos');
  readonly filtroRaza = signal<string>('Todas');
  readonly vistaModo = signal<'grid' | 'tabla'>('grid');

  // Modal Ver Detalle / Editar
  readonly modalDetalleAbierto = signal(false);
  readonly animalDetalle = signal<Animal | null>(null);

  // Modal de Confirmación de Eliminación
  readonly modalEliminarAbierto = signal(false);
  readonly animalAEliminar = signal<Animal | null>(null);

  readonly razasDisponibles = ['Holstein', 'Normando', 'Jersey', 'Brahman', 'Girolando', 'Gyr', 'Pardo Suizo'];

  // Lista filtrada reactiva
  readonly animalesFiltrados = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const sexo = this.filtroSexo();
    const raza = this.filtroRaza();

    return this.farmService.animales().filter(animal => {
      const coincideTerm = !term ||
        animal.nombre.toLowerCase().includes(term) ||
        animal.codigo.toLowerCase().includes(term) ||
        animal.raza.toLowerCase().includes(term);

      const coincideSexo = sexo === 'Todos' || animal.sexo === sexo;
      const coincideRaza = raza === 'Todas' || animal.raza === raza;

      return coincideTerm && coincideSexo && coincideRaza;
    });
  });

  abrirDetalle(animal: Animal) {
    this.animalDetalle.set(animal);
    this.modalDetalleAbierto.set(true);
  }

  cerrarDetalle() {
    this.modalDetalleAbierto.set(false);
  }

  solicitarEliminar(animal: Animal) {
    this.animalAEliminar.set(animal);
    this.modalEliminarAbierto.set(true);
  }

  confirmarEliminar() {
    const animal = this.animalAEliminar();
    if (animal) {
      this.farmService.eliminarAnimal(animal.id);
    }
    this.cancelarEliminar();
  }

  cancelarEliminar() {
    this.animalAEliminar.set(null);
    this.modalEliminarAbierto.set(false);
  }

  exportarPDF() {
    this.router.navigate(['/reportes'], { queryParams: { tipo: 'ganado' } });
  }
}
