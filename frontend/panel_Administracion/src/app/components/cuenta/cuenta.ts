import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FarmDataService } from '../../services/farm-data.service';

@Component({
  selector: 'app-cuenta',
  imports: [CommonModule, FormsModule],
  templateUrl: './cuenta.html',
  styleUrl: './cuenta.css'
})
export class Cuenta {
  readonly farmService = inject(FarmDataService);

  readonly modoEdicion = signal(false);

  nombre = this.farmService.finca().propietario;
  finca = this.farmService.finca().nombre;
  correo = this.farmService.finca().correo;
  telefono = this.farmService.finca().telefono;
  municipio = this.farmService.finca().municipio;

  guardarCambios() {
    this.farmService.finca.update(f => ({
      ...f,
      propietario: this.nombre,
      nombre: this.finca,
      correo: this.correo,
      telefono: this.telefono,
      municipio: this.municipio
    }));
    this.modoEdicion.set(false);
  }

  cancelar() {
    this.nombre = this.farmService.finca().propietario;
    this.finca = this.farmService.finca().nombre;
    this.correo = this.farmService.finca().correo;
    this.telefono = this.farmService.finca().telefono;
    this.municipio = this.farmService.finca().municipio;
    this.modoEdicion.set(false);
  }
}
