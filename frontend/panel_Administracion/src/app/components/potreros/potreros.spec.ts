import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Potreros } from './potreros';

describe('Potreros', () => {
  let component: Potreros;
  let fixture: ComponentFixture<Potreros>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Potreros],
    }).compileComponents();

    fixture = TestBed.createComponent(Potreros);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
