import { Component, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AppointmentDetail, appointmentDetails } from '../client-data';
import { BottomNav } from '../shared/bottom-nav';

@Component({
  selector: 'app-agendamento-detalhe-page',
  imports: [RouterLink, BottomNav],
  template: `
    <main class="client-shell simple-client-page appointment-detail-page">
      <a class="simple-back" routerLink="/">Inicio</a>

      @if (appointment; as item) {
        <section class="detail-hero">
          <img [alt]="'Foto de ' + item.barberName" [src]="item.barberAvatarUrl" />
          <span>
            <small>{{ item.date }}</small>
            <h1>{{ servicesLabel(item.services) }}</h1>
            <strong>{{ item.startsAt }} - {{ item.endsAt }}</strong>
          </span>
        </section>

        <section class="detail-section">
          <h2>Servicos</h2>
          @for (service of item.services; track service) {
            <p>{{ service }}</p>
          }
        </section>

        <section class="detail-section">
          <h2>Barbeiro</h2>
          <p>{{ item.barberName }}</p>
        </section>

        <section class="detail-section detail-total">
          <h2>Total</h2>
          <p>{{ formatPrice(item.precoTotal) }}</p>
        </section>
      } @else {
        <h1>Agendamento nao encontrado</h1>
      }
    </main>

    <app-bottom-nav current="agendamentos" />
  `,
})
export class AgendamentoDetalhePage {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly appointmentId = Number(this.route.snapshot.paramMap.get('id'));
  protected appointment: AppointmentDetail | null =
    appointmentDetails.find((item) => item.id === this.appointmentId) ?? null;

  constructor() {
    this.http.get<AppointmentDetail>(`/api/cliente/home/agendamentos/${this.appointmentId}`).subscribe({
      next: (appointment) => {
        this.appointment = appointment;
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
