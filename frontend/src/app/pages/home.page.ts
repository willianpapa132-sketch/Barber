import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppointmentSummary, latestAppointments, nextAppointment as fallbackNextAppointment } from '../client-data';
import { BottomNav } from '../shared/bottom-nav';

interface HomeResponse {
  cliente: {
    nome: string;
  };
  proximoAtendimento: AppointmentSummary | null;
}

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, BottomNav],
  templateUrl: './home.page.html',
})
export class HomePage implements OnInit {
  private readonly http = inject(HttpClient);

  protected clientName = 'Willian';
  protected nextAppointment = fallbackNextAppointment;
  protected readonly latestAppointments = latestAppointments;

  ngOnInit(): void {
    this.http.get<HomeResponse>('/api/cliente/home').subscribe({
      next: (response) => {
        this.clientName = response.cliente.nome;
        this.nextAppointment = response.proximoAtendimento;
      },
    });
  }

  protected servicesLabel(services: string[]): string {
    return services.join(' + ');
  }

  protected formatPrice(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }
}
