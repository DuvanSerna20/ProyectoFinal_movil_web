import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FarmDataService, UsuarioMovil } from '../../services/farm-data.service';

@Component({
  selector: 'app-usuarios',
  imports: [CommonModule, FormsModule],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.css'
})
export class Usuarios {
  readonly farmService = inject(FarmDataService);
  private readonly router = inject(Router);

  readonly searchTerm = signal('');
  readonly filtroRol = signal<string>('Todos');

  // Modal Confirmar Eliminación de Usuario
  readonly modalEliminarAbierto = signal(false);
  readonly usuarioAEliminar = signal<UsuarioMovil | null>(null);

  readonly roles = ['Todos', 'Administrador', 'Operario de Campo', 'Vaquero'];

  readonly usuariosFiltrados = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const rol = this.filtroRol();

    return this.farmService.usuarios().filter(u => {
      const coincideTerm = !term ||
        u.nombre.toLowerCase().includes(term) ||
        u.correo.toLowerCase().includes(term) ||
        u.dispositivo.toLowerCase().includes(term);

      const coincideRol = rol === 'Todos' || u.rol === rol;

      return coincideTerm && coincideRol;
    });
  });

  solicitarEliminar(u: UsuarioMovil) {
    this.usuarioAEliminar.set(u);
    this.modalEliminarAbierto.set(true);
  }

  confirmarEliminar() {
    const u = this.usuarioAEliminar();
    if (u) {
      this.farmService.eliminarUsuario(u.id);
    }
    this.cancelarEliminar();
  }

  cancelarEliminar() {
    this.usuarioAEliminar.set(null);
    this.modalEliminarAbierto.set(false);
  }

  exportarPDF() {
    this.router.navigate(['/reportes'], { queryParams: { tipo: 'usuarios' } });
  }
}
