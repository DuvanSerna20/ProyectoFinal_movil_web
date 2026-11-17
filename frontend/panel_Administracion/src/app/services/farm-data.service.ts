import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, of } from 'rxjs';

export interface UsuarioMovil {
  id: string;
  nombre: string;
  finca: string;
  correo: string;
  rol: 'Administrador' | 'Operario de Campo' | 'Vaquero';
  fechaRegistro: string;
  ultimoAcceso: string;
  dispositivo: string;
  estado: 'Activo' | 'Inactivo';
}

export interface ActividadAuditoria {
  id: string;
  modulo: string;
  accion: string;
  descripcion: string;
  usuario: string;
  fecha: string;
  hora: string;
  origen: string;
}

export interface Animal {
  id: string;
  codigo: string;
  nombre: string;
  raza: string;
  sexo: 'Hembra' | 'Macho';
  pesoKg?: number;
  estado: string;
  especie?: string;
  categoria?: string;
  proposito?: string;
  estadoReproductivo?: string;
  produccionLitrosDia?: number;
  fincaId?: string;
  potreroId?: string;
  fotoUrl?: string;
  registradoPor: string;
  fechaRegistro: string;
}

export interface Potrero {
  id: string;
  fincaId?: string;
  nombre: string;
  largoMetros?: number;
  anchoMetros?: number;
  area?: number;
  unidadArea?: string;
  areaM2: number;
  areaHa: number;
  estado: 'Ocupado' | 'En descanso';
  condicionPasto?: string;
  diasOcupacion: number;
  diasDescanso: number;
  tipoPasto: string;
  fechaUltimoCambio?: string;
  cantidadAnimales?: number;
  registradoPor: string;
  fechaRegistro: string;
}

export interface RegistroOrdeno {
  id: string;
  fecha: string;
  jornada: string;
  litros: number;
  animalId?: string;
  animalCodigo?: string;
  animalNombre?: string;
  responsable: string;
  horaRegistro: string;
  destinoLeche?: string;
}

export interface TareaCuidado {
  id: string;
  titulo: string;
  categoria: string;
  fechaLimite: string;
  completada: boolean;
  prioridad: 'Alta' | 'Media' | 'Baja';
  responsable: string;
  descripcion?: string;
  esTareaEspecial?: boolean;
  tipoTareaEspecial?: string;
  animalId?: string;
  animalCodigoNombre?: string;
}

export interface RegistroAlimentacion {
  id: string;
  animalId?: string;
  grupoProposito?: string;
  objetivoLabel: string;
  tipoRegistro: 'Alimento / Suplemento' | 'Medicamento / Tratamiento' | string;
  categoriaDetalle: string;
  nombreProducto: string;
  cantidad: number;
  unidad: string;
  frecuencia: string;
  tieneRetiroLeche: boolean;
  diasRetiro: number;
  activo: boolean;
  fechaInicio: string;
  fechaFin?: string;
  registradoPor?: string;
}

export interface FincaInfo {
  id?: string;
  nombre: string;
  departamento: string;
  municipio?: string;
  pisoTermico?: string;
  propietario: string;
  correo?: string;
  telefono?: string;
  fechaCreacion?: string;
  superficieTotalHa?: number;
}

export interface CalculoRotacion {
  potreroId?: string;
  potreroNombre?: string;
  diasOcupacion: number;
  diasDescanso: number;
  fechaIngreso: string;
  potrerosNecesarios: number;
  tiempoTotalRegulacion: number;
  proximaFechaCambio: string;
  alcanzanPotreros: boolean;
  potrerosFaltantes: number;
}

@Injectable({
  providedIn: 'root'
})
export class FarmDataService {
  private readonly http = inject(HttpClient);
  readonly apiUrl = 'http://localhost:3000/api';

  // Estados de conexión
  readonly conectadoBackend = signal<boolean>(false);
  readonly cargando = signal<boolean>(false);

  // Fecha del sistema en tiempo real
  readonly fechaHoyStr = signal<string>(new Date().toISOString().split('T')[0]);
  readonly fechaLegible = computed(() => {
    return new Date().toLocaleDateString('es-CO', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  });

  // Información de la finca
  readonly finca = signal<FincaInfo>({
    nombre: 'Finca La Esperanza',
    departamento: 'Córdoba',
    municipio: 'Montería',
    pisoTermico: 'Cálido',
    propietario: 'Juan Pablo Berrocal',
    correo: 'juan@correo.com',
    telefono: '+57 312 456 7890',
    fechaCreacion: new Date().toLocaleDateString('es-CO'),
    superficieTotalHa: 0
  });

  // Colecciones reales desde MongoDB
  readonly usuarios = signal<UsuarioMovil[]>([]);
  readonly animales = signal<Animal[]>([]);
  readonly potreros = signal<Potrero[]>([]);
  readonly registrosOrdeno = signal<RegistroOrdeno[]>([]);
  readonly tareas = signal<TareaCuidado[]>([]);
  readonly auditoria = signal<ActividadAuditoria[]>([]);
  readonly alimentacion = signal<RegistroAlimentacion[]>([]);
  readonly fincas = signal<FincaInfo[]>([]);

  constructor() {
    this.cargarDatosDesdeServidor();
  }

  cargarDatosDesdeServidor() {
    this.cargando.set(true);

    this.http.get<{ status: string }>(`${this.apiUrl}/health`).pipe(
      catchError(err => {
        console.warn('[API] Servidor Node.js no disponible:', err.message);
        this.conectadoBackend.set(false);
        this.cargando.set(false);
        return of(null);
      })
    ).subscribe(res => {
      if (res && res.status === 'OK') {
        this.conectadoBackend.set(true);
        this.refrescarColecciones();
      } else {
        this.conectadoBackend.set(false);
        this.cargando.set(false);
      }
    });
  }

  refrescarColecciones() {
    // 1. Cargar Animales
    this.http.get<{ success: boolean; data: Animal[] }>(`${this.apiUrl}/animales`).pipe(
      catchError(() => of({ success: false, data: [] }))
    ).subscribe(resp => {
      this.animales.set(resp.data || []);
    });

    // 2. Cargar Potreros
    this.http.get<{ success: boolean; data: Potrero[] }>(`${this.apiUrl}/potreros`).pipe(
      catchError(() => of({ success: false, data: [] }))
    ).subscribe(resp => {
      this.potreros.set(resp.data || []);
    });

    // 3. Cargar Ordeño
    this.http.get<{ success: boolean; data: RegistroOrdeno[] }>(`${this.apiUrl}/ordeno`).pipe(
      catchError(() => of({ success: false, data: [] }))
    ).subscribe(resp => {
      this.registrosOrdeno.set(resp.data || []);
    });

    // 4. Cargar Tareas
    this.http.get<{ success: boolean; data: TareaCuidado[] }>(`${this.apiUrl}/tareas`).pipe(
      catchError(() => of({ success: false, data: [] }))
    ).subscribe(resp => {
      this.tareas.set(resp.data || []);
    });

    // 5. Cargar Usuarios
    this.http.get<{ success: boolean; data: UsuarioMovil[] }>(`${this.apiUrl}/usuarios`).pipe(
      catchError(() => of({ success: false, data: [] }))
    ).subscribe(resp => {
      this.usuarios.set(resp.data || []);
    });

    // 6. Cargar Auditoría
    this.http.get<{ success: boolean; data: ActividadAuditoria[] }>(`${this.apiUrl}/auditoria`).pipe(
      catchError(() => of({ success: false, data: [] }))
    ).subscribe(resp => {
      this.auditoria.set(resp.data || []);
    });

    // 7. Cargar Alimentación / Medicamentos
    this.http.get<{ success: boolean; data: RegistroAlimentacion[] }>(`${this.apiUrl}/alimentacion`).pipe(
      catchError(() => of({ success: false, data: [] }))
    ).subscribe(resp => {
      this.alimentacion.set(resp.data || []);
    });

    // 8. Cargar Fincas
    this.http.get<{ success: boolean; data: FincaInfo[] }>(`${this.apiUrl}/fincas`).pipe(
      catchError(() => of({ success: false, data: [] }))
    ).subscribe(resp => {
      if (resp.data && resp.data.length > 0) {
        this.fincas.set(resp.data);
        this.finca.set(resp.data[0]);
      }
      this.cargando.set(false);
    });
  }

  // Operaciones de Eliminación en MongoDB (Panel Web Administrador)
  eliminarUsuario(id: string) {
    if (this.conectadoBackend()) {
      this.http.delete(`${this.apiUrl}/usuarios/${id}`).pipe(
        catchError(err => of(null))
      ).subscribe(() => {
        this.usuarios.update(list => list.filter(u => u.id !== id));
        this.refrescarColecciones();
      });
    } else {
      this.usuarios.update(list => list.filter(u => u.id !== id));
    }
  }

  eliminarAnimal(id: string) {
    if (this.conectadoBackend()) {
      this.http.delete(`${this.apiUrl}/animales/${id}`).pipe(
        catchError(err => of(null))
      ).subscribe(() => {
        this.animales.update(list => list.filter(a => a.id !== id));
        this.refrescarColecciones();
      });
    } else {
      this.animales.update(list => list.filter(a => a.id !== id));
    }
  }

  eliminarPotrero(id: string) {
    if (this.conectadoBackend()) {
      this.http.delete(`${this.apiUrl}/potreros/${id}`).pipe(
        catchError(err => of(null))
      ).subscribe(() => {
        this.potreros.update(list => list.filter(p => p.id !== id));
        this.refrescarColecciones();
      });
    } else {
      this.potreros.update(list => list.filter(p => p.id !== id));
    }
  }

  eliminarRegistroOrdeno(id: string) {
    this.eliminarOrdeno(id);
  }

  eliminarOrdeno(id: string) {
    if (this.conectadoBackend()) {
      this.http.delete(`${this.apiUrl}/ordeno/${id}`).pipe(
        catchError(err => of(null))
      ).subscribe(() => {
        this.registrosOrdeno.update(list => list.filter(o => o.id !== id));
        this.refrescarColecciones();
      });
    } else {
      this.registrosOrdeno.update(list => list.filter(o => o.id !== id));
    }
  }

  eliminarTarea(id: string) {
    if (this.conectadoBackend()) {
      this.http.delete(`${this.apiUrl}/tareas/${id}`).pipe(
        catchError(err => of(null))
      ).subscribe(() => {
        this.tareas.update(list => list.filter(t => t.id !== id));
        this.refrescarColecciones();
      });
    } else {
      this.tareas.update(list => list.filter(t => t.id !== id));
    }
  }

  eliminarAlimentacion(id: string) {
    if (this.conectadoBackend()) {
      this.http.delete(`${this.apiUrl}/alimentacion/${id}`).pipe(
        catchError(err => of(null))
      ).subscribe(() => {
        this.alimentacion.update(list => list.filter(a => a.id !== id));
        this.refrescarColecciones();
      });
    } else {
      this.alimentacion.update(list => list.filter(a => a.id !== id));
    }
  }

  alternarEstadoTarea(id: string) {
    if (this.conectadoBackend()) {
      this.http.patch(`${this.apiUrl}/tareas/${id}/toggle`, {}).pipe(
        catchError(err => of(null))
      ).subscribe(() => {
        this.refrescarColecciones();
      });
    } else {
      this.tareas.update(list =>
        list.map(t => t.id === id ? { ...t, completada: !t.completada } : t)
      );
    }
  }

  // ==========================================
  // Métricas Computadas
  // ==========================================
  readonly totalCabezas = computed(() => this.animales().length);
  readonly totalHembras = computed(() => this.animales().filter(a => a.sexo === 'Hembra').length);
  readonly totalMachos = computed(() => this.animales().filter(a => a.sexo === 'Macho').length);

  readonly totalPotreros = computed(() => this.potreros().length);
  readonly potrerosOcupados = computed(() => this.potreros().filter(p => p.estado === 'Ocupado').length);
  readonly potrerosDescanso = computed(() => this.potreros().filter(p => p.estado === 'En descanso').length);
  readonly areaTotalHa = computed(() =>
    Number(this.potreros().reduce((acc, p) => acc + (p.areaHa || 0), 0).toFixed(2))
  );

  /**
   * Leche que cuenta como comercial (va al tanque / venta), igual que en la app Android
   * (DestinoLeche.esComercial). Sin destino = tanque por defecto; solo se excluyen
   * "Retiro médico (Descarte)" y "Consumo ternero" (y sus nombres de enum).
   */
  private static esLecheComercial(destino?: string): boolean {
    const d = (destino ?? '').trim().toLowerCase();
    if (!d) return true;
    return !(d.includes('descarte') || d.includes('retiro') || d.includes('ternero') || d.includes('consumo'));
  }

  readonly litrosHoy = computed(() => {
    const hoy = this.fechaHoyStr();
    return this.registrosOrdeno()
      .filter(r => r.fecha === hoy && FarmDataService.esLecheComercial(r.destinoLeche))
      .reduce((sum, r) => sum + r.litros, 0);
  });

  readonly litrosMananaHoy = computed(() => {
    const hoy = this.fechaHoyStr();
    return this.registrosOrdeno()
      .filter(r => r.fecha === hoy && r.jornada === 'Mañana' && FarmDataService.esLecheComercial(r.destinoLeche))
      .reduce((sum, r) => sum + r.litros, 0);
  });

  readonly litrosTardeHoy = computed(() => {
    const hoy = this.fechaHoyStr();
    return this.registrosOrdeno()
      .filter(r => r.fecha === hoy && r.jornada === 'Tarde' && FarmDataService.esLecheComercial(r.destinoLeche))
      .reduce((sum, r) => sum + r.litros, 0);
  });

  readonly tareasPendientes = computed(() =>
    this.tareas().filter(t => !t.completada).length
  );

  readonly tareasCompletadas = computed(() =>
    this.tareas().filter(t => t.completada).length
  );

  readonly totalUsuarios = computed(() => this.usuarios().length);
  readonly usuariosActivos = computed(() => this.usuarios().filter(u => u.estado === 'Activo').length);

  readonly totalAlimentacion = computed(() => this.alimentacion().length);
  readonly tratamientosActivos = computed(() => this.alimentacion().filter(a => a.activo).length);
  readonly tratamientosRetiro = computed(() => this.alimentacion().filter(a => a.tieneRetiroLeche && a.activo).length);

  // Fórmulas de Rotación Ganadera
  calcularRotacion(diasOcupacion: number, diasDescanso: number, fechaIngresoStr: string, potreroId?: string): CalculoRotacion {
    const ocup = Math.max(1, diasOcupacion || 1);
    const desc = Math.max(1, diasDescanso || 1);

    const potrerosNecesarios = Math.ceil(desc / ocup) + 1;
    const tiempoTotalRegulacion = ocup * potrerosNecesarios;

    const fechaIngreso = new Date(fechaIngresoStr || new Date());
    const fechaCambio = new Date(fechaIngreso);
    fechaCambio.setDate(fechaCambio.getDate() + ocup);

    const proximaFechaCambio = fechaCambio.toLocaleDateString('es-CO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });

    const potrerosRegistrados = this.potreros().length;
    const alcanzanPotreros = potrerosRegistrados >= potrerosNecesarios;
    const potrerosFaltantes = alcanzanPotreros ? 0 : potrerosNecesarios - potrerosRegistrados;

    let potreroNombre = '';
    if (potreroId) {
      const p = this.potreros().find(item => item.id === potreroId);
      if (p) potreroNombre = p.nombre;
    }

    return {
      potreroId,
      potreroNombre,
      diasOcupacion: ocup,
      diasDescanso: desc,
      fechaIngreso: fechaIngresoStr,
      potrerosNecesarios,
      tiempoTotalRegulacion,
      proximaFechaCambio,
      alcanzanPotreros,
      potrerosFaltantes
    };
  }

  imprimirReporte() {
    window.print();
  }
}
