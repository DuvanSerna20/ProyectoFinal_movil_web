import { Routes } from '@angular/router';
import { Dashboard } from './components/dashboard/dashboard';
import { Ganado } from './components/ganado/ganado';
import { Potreros } from './components/potreros/potreros';
import { Rotacion } from './components/rotacion/rotacion';
import { Ordeno } from './components/ordeno/ordeno';
import { Tareas } from './components/tareas/tareas';
import { AlimentacionComponent } from './components/alimentacion/alimentacion';
import { Usuarios } from './components/usuarios/usuarios';
import { Reportes } from './components/reportes/reportes';
import { Cuenta } from './components/cuenta/cuenta';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: Dashboard, title: 'Panel - Auditoría y Resumen' },
  { path: 'ganado', component: Ganado, title: 'Panel - Registro de Ganado' },
  { path: 'potreros', component: Potreros, title: 'Panel - Registro de Potreros' },
  { path: 'rotacion', component: Rotacion, title: 'Panel - Planes de Rotación' },
  { path: 'ordeno', component: Ordeno, title: 'Panel - Historial de Ordeño' },
  { path: 'tareas', component: Tareas, title: 'Panel - Auditoría de Tareas' },
  { path: 'alimentacion', component: AlimentacionComponent, title: 'Panel - Alimentación y Sanidad' },
  { path: 'usuarios', component: Usuarios, title: 'Panel - Gestión de Usuarios' },
  { path: 'reportes', component: Reportes, title: 'Panel - Reportes Oficiales en PDF' },
  { path: 'cuenta', component: Cuenta, title: 'Panel - Cuenta de Administrador' },
  { path: '**', redirectTo: 'dashboard' }
];
