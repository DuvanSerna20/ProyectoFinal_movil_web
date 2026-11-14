import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FarmDataService } from '../../services/farm-data.service';

@Component({
  selector: 'app-reportes',
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.html',
  styleUrl: './reportes.css'
})
export class Reportes implements OnInit {
  readonly farmService = inject(FarmDataService);
  private readonly route = inject(ActivatedRoute);

  readonly tipoReporte = signal<'general' | 'ganado' | 'potreros' | 'ordeno' | 'usuarios'>('general');
  readonly fechaEmision = signal(new Date().toLocaleDateString('es-CO'));
  readonly folio = signal(`REP-GAN-${new Date().getFullYear()}-${Math.floor(1000 + Math.random() * 9000)}`);

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['tipo']) {
        const t = params['tipo'];
        if (['general', 'ganado', 'potreros', 'ordeno', 'usuarios'].includes(t)) {
          this.tipoReporte.set(t as any);
        }
      }
    });
  }

  cambiarTipo(tipo: 'general' | 'ganado' | 'potreros' | 'ordeno' | 'usuarios') {
    this.tipoReporte.set(tipo);
  }

  descargarOImprimirPDF() {
    window.print();
  }
}
