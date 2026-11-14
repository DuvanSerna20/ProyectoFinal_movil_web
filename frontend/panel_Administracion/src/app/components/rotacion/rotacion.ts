import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FarmDataService, CalculoRotacion } from '../../services/farm-data.service';

@Component({
  selector: 'app-rotacion',
  imports: [CommonModule, FormsModule],
  templateUrl: './rotacion.html',
  styleUrl: './rotacion.css'
})
export class Rotacion implements OnInit {
  readonly farmService = inject(FarmDataService);
  private readonly route = inject(ActivatedRoute);

  // Inputs del cálculo
  potreroSeleccionadoId = '';
  diasOcupacion = 3;
  diasDescanso = 30;
  fechaIngreso = new Date().toISOString().split('T')[0];

  // Resultado
  readonly resultado = signal<CalculoRotacion | null>(null);

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['potreroId']) {
        this.potreroSeleccionadoId = params['potreroId'];
      }
      if (params['ocupacion']) {
        this.diasOcupacion = Number(params['ocupacion']) || 3;
      }
      if (params['descanso']) {
        this.diasDescanso = Number(params['descanso']) || 30;
      }
      this.ejecutarCalculo();
    });
  }

  alCambiarPotrero() {
    if (this.potreroSeleccionadoId) {
      const p = this.farmService.potreros().find(item => item.id === this.potreroSeleccionadoId);
      if (p) {
        this.diasOcupacion = p.diasOcupacion;
        this.diasDescanso = p.diasDescanso;
      }
    }
    this.ejecutarCalculo();
  }

  ejecutarCalculo() {
    const res = this.farmService.calcularRotacion(
      this.diasOcupacion,
      this.diasDescanso,
      this.fechaIngreso,
      this.potreroSeleccionadoId
    );
    this.resultado.set(res);
  }
}
