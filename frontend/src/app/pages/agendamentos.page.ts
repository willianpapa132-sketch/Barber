import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BottomNav } from '../shared/bottom-nav';

type AppointmentTab = 'proximos' | 'antigos';

interface AppointmentListItem {
  id: number;
  dayName: string;
  dayNumber: string;
  startsAt: string;
  endsAt: string;
  barberName: string;
  barberAvatarUrl: string | null;
  services: string[];
  precoTotal: number;
}

interface AppointmentsPageResponse {
  proximos: AppointmentListItem[];
  antigos: AppointmentListItem[];
}

@Component({
  selector: 'app-agendamentos-page',
  imports: [RouterLink, BottomNav],
  template: `
    <main class="client-shell appointments-page-shell">
      <header class="appointments-header">
        <h1>Agendamentos</h1>
        <span class="appointments-bell" aria-hidden="true">&#128276;</span>
      </header>

      <div class="appointments-tabs" role="tablist" aria-label="Filtro de agendamentos">
        <button type="button" [class.active]="activeTab === 'proximos'" (click)="activeTab = 'proximos'">Proximos</button>
        <button type="button" [class.active]="activeTab === 'antigos'" (click)="activeTab = 'antigos'">Antigos</button>
      </div>

      @if (loading) {
        <section class="appointments-state-card">
          <strong>Carregando agendamentos...</strong>
        </section>
      } @else if (errorMessage) {
        <section class="appointments-state-card error">
          <strong>{{ errorMessage }}</strong>
          <button type="button" (click)="loadAppointments()">Tentar novamente</button>
        </section>
      } @else if (visibleAppointments.length) {
        <section class="appointments-list" [attr.aria-label]="activeTab === 'proximos' ? 'Proximos agendamentos' : 'Agendamentos antigos'">
          @for (appointment of visibleAppointments; track appointment.id; let first = $first) {
            <a class="appointment-list-card" [class.featured]="activeTab === 'proximos' && first" [routerLink]="['/agendamentos', appointment.id]">
              <img [alt]="'Foto de ' + appointment.barberName" [src]="avatarUrl(appointment.barberAvatarUrl)" />
              <span class="appointment-card-content">
                <span class="appointment-card-head">
                  <strong>{{ appointment.barberName }}</strong>
                  <small><i aria-hidden="true">&#128197;</i>{{ activeTab === 'proximos' ? 'Agendado' : 'Antigo' }}</small>
                </span>
                <span class="appointment-card-row">
                  <i aria-hidden="true">&#128197;</i>
                  <span><b>Dia {{ appointment.dayNumber }}</b><em>{{ appointment.dayName }}</em></span>
                </span>
                <span class="appointment-card-row">
                  <i aria-hidden="true">&#9716;</i>
                  <span><b>{{ appointment.startsAt }} - {{ appointment.endsAt }}</b></span>
                </span>
                <span class="appointment-card-row">
                  <i aria-hidden="true">&#9986;</i>
                  <span><b>{{ servicesLabel(appointment.services) }}</b></span>
                </span>
              </span>
              <i class="appointment-card-arrow" aria-hidden="true">&rsaquo;</i>
            </a>
          }
        </section>
      } @else {
        <section class="appointments-empty-state">
          <div class="empty-calendar" aria-hidden="true">
            <span></span>
            <i></i>
          </div>
          <h2>Voce ainda nao possui agendamentos {{ activeTab === 'proximos' ? 'proximos' : 'antigos' }}</h2>
          <p>
            @if (activeTab === 'proximos') {
              Quando seus proximos horarios forem marcados, eles aparecerao aqui.
            } @else {
              Quando seus atendimentos forem finalizados, eles aparecerao aqui.
            }
          </p>
        </section>
      }
    </main>
    <app-bottom-nav current="agendamentos" />
  `,
})
export class AgendamentosPage implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly fallbackAvatar =
    'https://images.unsplash.com/photo-1628157588553-5eeea00af15c?auto=format&fit=crop&w=300&q=80';

  protected loading = true;
  protected errorMessage = '';
  protected activeTab: AppointmentTab = 'proximos';
  protected proximos: AppointmentListItem[] = [];
  protected antigos: AppointmentListItem[] = [];

  protected get visibleAppointments(): AppointmentListItem[] {
    return this.activeTab === 'proximos' ? this.proximos : this.antigos;
  }

  ngOnInit(): void {
    this.loadAppointments();
  }

  protected loadAppointments(): void {
    this.loading = true;
    this.errorMessage = '';

    this.http.get<AppointmentsPageResponse>('/api/cliente/agendamentos').subscribe({
      next: (response) => {
        this.proximos = response.proximos;
        this.antigos = response.antigos;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Nao foi possivel carregar seus agendamentos.';
        this.loading = false;
      },
    });
  }

  protected avatarUrl(url: string | null): string {
    return url || this.fallbackAvatar;
  }

  protected servicesLabel(services: string[]): string {
    return services.join(' + ');
  }
}
