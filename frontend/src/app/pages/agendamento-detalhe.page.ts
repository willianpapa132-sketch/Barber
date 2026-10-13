import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BottomNav } from '../shared/bottom-nav';

interface AppointmentDetail {
  id: number;
  date: string;
  weekday: string;
  startsAt: string;
  endsAt: string;
  barberName: string;
  barberAvatarUrl: string | null;
  services: string[];
  servicesLabel: string;
  details: string;
  statusLabel: string;
  precoTotal: number;
}

@Component({
  selector: 'app-agendamento-detalhe-page',
  imports: [RouterLink, BottomNav],
  template: `
    <main class="client-shell appointments-page-shell appointment-detail-page">
      <header class="profile-edit-topbar">
        <a routerLink="/agendamentos" aria-label="Voltar">&lsaquo;</a>
        <h1>Detalhes</h1>
      </header>

      @if (appointment; as item) {
        <section class="appointment-detail-card">
          <img [alt]="'Foto de ' + item.barberName" [src]="avatarUrl(item.barberAvatarUrl)" />
          <div>
            <small>{{ item.statusLabel }}</small>
            <h2>{{ item.servicesLabel }}</h2>
            <strong>{{ item.startsAt }} - {{ item.endsAt }}</strong>
          </div>
        </section>

        <section class="appointment-detail-section">
          <h3>Data</h3>
          <p>{{ item.date }} · {{ item.weekday }}</p>
        </section>

        <section class="appointment-detail-section">
          <h3>Servicos</h3>
          @for (service of item.services; track service) {
            <p>{{ service }}</p>
          }
        </section>

        <section class="appointment-detail-section">
          <h3>Barbeiro</h3>
          <p>{{ item.barberName }}</p>
        </section>

        <section class="appointment-detail-section detail-total">
          <h3>Total</h3>
          <p>{{ formatPrice(item.precoTotal) }}</p>
        </section>
      } @else if (errorMessage) {
        <section class="appointments-state-card error">
          <strong>{{ errorMessage }}</strong>
        </section>
      } @else {
        <section class="appointments-state-card">
          <strong>Carregando agendamento...</strong>
        </section>
      }
    </main>

    <app-bottom-nav current="agendamentos" />
  `,
})
export class AgendamentoDetalhePage {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly fallbackAvatar =
    'https://images.unsplash.com/photo-1628157588553-5eeea00af15c?auto=format&fit=crop&w=300&q=80';

  protected appointment: AppointmentDetail | null = null;
  protected errorMessage = '';

  constructor() {
    const appointmentId = Number(this.route.snapshot.paramMap.get('id'));

    this.http.get<AppointmentDetail>(`/api/cliente/agendamentos/${appointmentId}`).subscribe({
      next: (appointment) => {
        this.appointment = appointment;
      },
      error: () => {
        this.errorMessage = 'Agendamento nao encontrado.';
      },
    });
  }

  protected avatarUrl(url: string | null): string {
    return url || this.fallbackAvatar;
  }

  protected formatPrice(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }
}
