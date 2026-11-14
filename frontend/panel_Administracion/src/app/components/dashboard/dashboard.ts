import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FarmDataService } from '../../services/farm-data.service';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard {
  readonly farmService = inject(FarmDataService);

  imprimirPDF() {
    this.farmService.imprimirReporte();
  }
}
