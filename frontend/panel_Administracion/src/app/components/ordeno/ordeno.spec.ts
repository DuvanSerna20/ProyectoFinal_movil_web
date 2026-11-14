import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Ordeno } from './ordeno';

describe('Ordeno', () => {
  let component: Ordeno;
  let fixture: ComponentFixture<Ordeno>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Ordeno],
    }).compileComponents();

    fixture = TestBed.createComponent(Ordeno);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
