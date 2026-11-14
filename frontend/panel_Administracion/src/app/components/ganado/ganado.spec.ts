import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Ganado } from './ganado';

describe('Ganado', () => {
  let component: Ganado;
  let fixture: ComponentFixture<Ganado>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Ganado],
    }).compileComponents();

    fixture = TestBed.createComponent(Ganado);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
