import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { BottomNav } from '../shared/bottom-nav';

interface BarberOption {
  id: number;
  nome: string;
  telefone: string;
  imgUrl: string | null;
}

interface ServiceOption {
  id: number;
  nome: string;
  preco: number;
  duracaoMinutos: number;
  inclusoNoPlano: boolean;
}

interface BookingScreenResponse {
  usuarioId: number;
  clienteId: number;
  possuiPlano: boolean;
  nomePlano: string | null;
  barbeiros: BarberOption[];
  servicos: ServiceOption[];
}

@Component({
  selector: 'app-novo-agendamento-page',
  imports: [RouterLink, BottomNav],
  template: `
    <main class="client-shell agendar-page-shell">
      <header class="agendar-topbar">
        <a routerLink="/agendamentos" aria-label="Voltar">&lsaquo;</a>
        <h1>Novo agendamento</h1>
        <span></span>
      </header>

      <nav class="booking-steps" aria-label="Etapas do agendamento">
        @for (item of [1, 2, 3, 4]; track item) {
          <span [class.active]="step === item" [class.done]="step > item">
            @if (step > item) {
              &#10003;
            } @else {
              {{ item }}
            }
          </span>
        }
      </nav>

      @if (loading) {
        <section class="booking-state-card">
          <strong>Carregando agendamento...</strong>
        </section>
      } @else if (errorMessage) {
        <section class="booking-state-card error">
          <strong>{{ errorMessage }}</strong>
          <button type="button" (click)="loadScreen()">Tentar novamente</button>
        </section>
      } @else if (screen; as data) {
        @if (data.possuiPlano) {
          <section class="booking-plan-note">
            <span aria-hidden="true">&#9819;</span>
            <p>
              <strong>{{ data.nomePlano }}</strong>
              <small>Voce pode agendar apenas os servicos incluidos no seu plano mensal.</small>
            </p>
          </section>
        }

        @if (step === 1) {
          <section class="booking-step-view">
            <h2>1. Escolha o barbeiro</h2>
            <p>Selecione o profissional de sua preferencia.</p>

            <div class="booking-list">
              @for (barber of data.barbeiros; track barber.id) {
                <button type="button" class="booking-barber-card" [class.selected]="selectedBarberId === barber.id" (click)="selectBarber(barber.id)">
                  <img [alt]="'Foto de ' + barber.nome" [src]="avatarUrl(barber.imgUrl)" />
                  <span>
                    <strong>{{ barber.nome }}</strong>
                    <small>Profissional da barbearia</small>
                    <em><b>&#9733;</b> Disponivel para agendamento</em>
                  </span>
                  <i aria-hidden="true">&rsaquo;</i>
                </button>
              } @empty {
                <section class="booking-state-card">
                  <strong>Nenhum barbeiro disponivel.</strong>
                </section>
              }
            </div>

            <button type="button" class="booking-primary-action" [disabled]="!selectedBarberId" (click)="goToServices()">
              Continuar
            </button>
          </section>
        }

        @if (step === 2) {
          <section class="booking-step-view">
            <h2>2. Escolha os servicos</h2>
            <p>Selecione um ou mais servicos para o seu agendamento.</p>

            <div class="booking-list">
              @for (service of data.servicos; track service.id) {
                <button type="button" class="booking-service-card" [class.selected]="isServiceSelected(service.id)" (click)="toggleService(service.id)">
                  <span class="service-icon" aria-hidden="true">&#9986;</span>
                  <span class="service-copy">
                    <strong>{{ service.nome }}</strong>
                    <small>{{ service.duracaoMinutos }} min</small>
                  </span>
                  <span class="service-price">
                    @if (data.possuiPlano) {
                      <b>Incluido</b>
                    } @else {
                      <b>{{ formatPrice(service.preco) }}</b>
                    }
                    <i [class.checked]="isServiceSelected(service.id)" aria-hidden="true">&#10003;</i>
                  </span>
                </button>
              } @empty {
                <section class="booking-state-card">
                  <strong>Nenhum servico disponivel para agendamento.</strong>
                </section>
              }
            </div>

            @if (selectedServices.length) {
              <section class="booking-summary-card">
                <span aria-hidden="true">&#9776;</span>
                <p>
                  <strong>{{ selectedServices.length }} servico(s) selecionado(s)</strong>
                  <small>{{ selectedServicesLabel }}</small>
                </p>
                <b>{{ data.possuiPlano ? 'Plano mensal' : formatPrice(totalPrice) }}</b>
              </section>
            }

            <div class="booking-actions-row">
              <button type="button" class="booking-secondary-action" (click)="step = 1">Voltar</button>
              <button type="button" class="booking-primary-action" [disabled]="!selectedServices.length" (click)="goToDays()">Continuar</button>
            </div>
          </section>
        }

        @if (step === 3) {
          <section class="booking-step-view">
            <h2>3. Escolha o dia</h2>
            <p>Selecione a data para o seu agendamento.</p>

            <section class="booking-selected-card">
              <img [alt]="'Foto de ' + selectedBarber?.nome" [src]="avatarUrl(selectedBarber?.imgUrl || null)" />
              <span>
                <strong>{{ selectedBarber?.nome }}</strong>
                <small>Profissional da barbearia</small>
              </span>
            </section>

            <section class="booking-selected-card">
              <span class="service-icon" aria-hidden="true">&#9986;</span>
              <span>
                <strong>{{ selectedServicesLabel }}</strong>
                <small>{{ totalDuration }} min aprox.</small>
              </span>
              <b>{{ data.possuiPlano ? 'Plano' : formatPrice(totalPrice) }}</b>
            </section>

            @if (loadingAvailability) {
              <section class="booking-state-card">
                <strong>Buscando dias disponiveis...</strong>
              </section>
            } @else {
              <section class="booking-calendar">
                <header>
                  <strong>{{ currentMonthLabel }}</strong>
                </header>
                <div class="calendar-weekdays">
                  @for (day of weekDays; track day) {
                    <span>{{ day }}</span>
                  }
                </div>
                <div class="calendar-grid">
                  @for (day of calendarDays; track day.key) {
                    @if (day.date) {
                      <button type="button" [class.available]="day.available" [class.selected]="selectedDate === day.date" [disabled]="!day.available" (click)="selectDate(day.date)">
                        {{ day.label }}
                      </button>
                    } @else {
                      <span></span>
                    }
                  }
                </div>
              </section>
            }

            @if (selectedDate) {
              <section class="booking-date-card">
                <span aria-hidden="true">&#128197;</span>
                <strong>{{ selectedDateLabel }}</strong>
              </section>
            }

            <div class="booking-actions-row">
              <button type="button" class="booking-secondary-action" (click)="step = 2">Voltar</button>
              <button type="button" class="booking-primary-action" [disabled]="!selectedDate" (click)="goToTimes()">Continuar</button>
            </div>
          </section>
        }

        @if (step === 4) {
          <section class="booking-step-view">
            <h2>4. Escolha o horario</h2>
            <p>Selecione um horario disponivel para o seu agendamento.</p>

            <section class="booking-selected-card">
              <img [alt]="'Foto de ' + selectedBarber?.nome" [src]="avatarUrl(selectedBarber?.imgUrl || null)" />
              <span>
                <strong>{{ selectedBarber?.nome }}</strong>
                <small>Profissional da barbearia</small>
              </span>
            </section>

            <section class="booking-selected-card">
              <span class="service-icon" aria-hidden="true">&#9986;</span>
              <span>
                <strong>{{ selectedServicesLabel }}</strong>
                <small>{{ totalDuration }} min aprox.</small>
              </span>
              <b>{{ data.possuiPlano ? 'Plano' : formatPrice(totalPrice) }}</b>
            </section>

            <section class="booking-selected-card">
              <span class="service-icon calendar" aria-hidden="true">&#128197;</span>
              <span>
                <strong>{{ selectedDateLabel }}</strong>
              </span>
            </section>

            <h3 class="booking-times-title"><span aria-hidden="true">&#9716;</span> Horarios disponiveis</h3>

            @if (loadingAvailability) {
              <section class="booking-state-card">
                <strong>Buscando horarios...</strong>
              </section>
            } @else if (availableTimes.length) {
              <div class="booking-times-grid">
                @for (time of availableTimes; track time) {
                  <button type="button" [class.selected]="selectedTime === time" (click)="selectedTime = time">{{ shortTime(time) }}</button>
                }
              </div>
            } @else {
              <section class="booking-state-card">
                <strong>Nenhum horario disponivel para esse dia.</strong>
              </section>
            }

            <div class="booking-actions-row">
              <button type="button" class="booking-secondary-action" (click)="step = 3">Voltar</button>
              <button type="button" class="booking-primary-action" [disabled]="!selectedTime || saving" (click)="confirmBooking()">
                {{ saving ? 'Confirmando...' : 'Confirmar agendamento' }}
              </button>
            </div>
          </section>
        }
      }
    </main>
    <app-bottom-nav current="agendamentos" />
  `,
})
export class NovoAgendamentoPage implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly fallbackAvatar =
    'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=300&q=80';

  protected readonly weekDays = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SAB'];
  protected loading = true;
  protected loadingAvailability = false;
  protected saving = false;
  protected errorMessage = '';
  protected step = 1;
  protected screen: BookingScreenResponse | null = null;
  protected selectedBarberId: number | null = null;
  protected selectedServiceIds = new Set<number>();
  protected availableDays: string[] = [];
  protected availableTimes: string[] = [];
  protected selectedDate = '';
  protected selectedTime = '';
  protected currentMonth = new Date();

  protected get selectedBarber(): BarberOption | undefined {
    return this.screen?.barbeiros.find((barber) => barber.id === this.selectedBarberId);
  }

  protected get selectedServices(): ServiceOption[] {
    return this.screen?.servicos.filter((service) => this.selectedServiceIds.has(service.id)) ?? [];
  }

  protected get selectedServicesLabel(): string {
    return this.selectedServices.map((service) => service.nome).join(' + ');
  }

  protected get totalDuration(): number {
    return this.selectedServices.reduce((total, service) => total + service.duracaoMinutos, 0);
  }

  protected get totalPrice(): number {
    return this.selectedServices.reduce((total, service) => total + service.preco, 0);
  }

  protected get currentMonthLabel(): string {
    return this.currentMonth.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
  }

  protected get selectedDateLabel(): string {
    if (!this.selectedDate) {
      return '';
    }
    return this.parseLocalDate(this.selectedDate).toLocaleDateString('pt-BR', {
      weekday: 'long',
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    });
  }

  protected get calendarDays(): Array<{ key: string; date: string | null; label: number | null; available: boolean }> {
    const year = this.currentMonth.getFullYear();
    const month = this.currentMonth.getMonth();
    const first = new Date(year, month, 1);
    const last = new Date(year, month + 1, 0);
    const available = new Set(this.availableDays);
    const days: Array<{ key: string; date: string | null; label: number | null; available: boolean }> = [];

    for (let index = 0; index < first.getDay(); index++) {
      days.push({ key: `empty-${index}`, date: null, label: null, available: false });
    }

    for (let day = 1; day <= last.getDate(); day++) {
      const date = this.formatDate(new Date(year, month, day));
      days.push({ key: date, date, label: day, available: available.has(date) });
    }

    return days;
  }

  ngOnInit(): void {
    this.loadScreen();
  }

  protected loadScreen(): void {
    this.loading = true;
    this.errorMessage = '';

    this.http.get<BookingScreenResponse>('/api/cliente/home/agendar').subscribe({
      next: (response) => {
        this.screen = response;
        this.selectedBarberId = response.barbeiros[0]?.id ?? null;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Nao foi possivel carregar as opcoes de agendamento.';
        this.loading = false;
      },
    });
  }

  protected avatarUrl(url: string | null): string {
    return url || this.fallbackAvatar;
  }

  protected selectBarber(barberId: number): void {
    this.selectedBarberId = barberId;
    this.resetAvailability();
  }

  protected goToServices(): void {
    if (this.selectedBarberId) {
      this.step = 2;
    }
  }

  protected toggleService(serviceId: number): void {
    if (this.selectedServiceIds.has(serviceId)) {
      this.selectedServiceIds.delete(serviceId);
    } else {
      this.selectedServiceIds.add(serviceId);
    }
    this.resetAvailability();
  }

  protected isServiceSelected(serviceId: number): boolean {
    return this.selectedServiceIds.has(serviceId);
  }

  protected goToDays(): void {
    if (!this.selectedBarberId || !this.selectedServices.length || !this.screen) {
      return;
    }

    this.step = 3;
    this.loadingAvailability = true;
    this.errorMessage = '';
    this.selectedDate = '';
    this.selectedTime = '';
    this.availableTimes = [];

    this.http.get<string[]>('/api/cliente/home/agendar/disponibilidade/dias', { params: this.availabilityParams() }).subscribe({
      next: (days) => {
        this.availableDays = days;
        this.currentMonth = days.length ? this.parseLocalDate(days[0]) : new Date();
        this.loadingAvailability = false;
      },
      error: () => {
        this.errorMessage = 'Nao foi possivel carregar os dias disponiveis.';
        this.loadingAvailability = false;
      },
    });
  }

  protected selectDate(date: string): void {
    this.selectedDate = date;
    this.selectedTime = '';
    this.loadTimes();
  }

  protected goToTimes(): void {
    if (this.selectedDate) {
      this.step = 4;
      if (!this.availableTimes.length) {
        this.loadTimes();
      }
    }
  }

  protected confirmBooking(): void {
    if (!this.screen || !this.selectedBarberId || !this.selectedDate || !this.selectedTime || !this.selectedServices.length) {
      return;
    }

    this.saving = true;
    this.errorMessage = '';

    this.http
      .post('/api/cliente/home/agendar', {
        usuarioId: this.screen.usuarioId,
        clienteId: this.screen.clienteId,
        barbeiroId: this.selectedBarberId,
        servicosIds: this.selectedServices.map((service) => service.id),
        data: this.selectedDate,
        horaInicio: this.shortTime(this.selectedTime),
      })
      .subscribe({
        next: () => this.router.navigateByUrl('/agendamentos'),
        error: () => {
          this.errorMessage = 'Nao foi possivel confirmar esse horario.';
          this.saving = false;
        },
      });
  }

  protected formatPrice(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  protected shortTime(time: string): string {
    return time.slice(0, 5);
  }

  private loadTimes(): void {
    if (!this.selectedDate) {
      return;
    }

    this.loadingAvailability = true;
    this.availableTimes = [];

    this.http
      .get<string[]>('/api/cliente/home/agendar/disponibilidade/horarios', {
        params: this.availabilityParams().set('data', this.selectedDate),
      })
      .subscribe({
        next: (times) => {
          this.availableTimes = times;
          this.loadingAvailability = false;
        },
        error: () => {
          this.errorMessage = 'Nao foi possivel carregar os horarios disponiveis.';
          this.loadingAvailability = false;
        },
      });
  }

  private availabilityParams(): HttpParams {
    let params = new HttpParams()
      .set('usuarioId', String(this.screen?.usuarioId ?? ''))
      .set('clienteId', String(this.screen?.clienteId ?? ''))
      .set('barbeiroId', String(this.selectedBarberId ?? ''));

    for (const serviceId of this.selectedServices.map((service) => service.id)) {
      params = params.append('servicosIds', String(serviceId));
    }

    return params;
  }

  private resetAvailability(): void {
    this.availableDays = [];
    this.availableTimes = [];
    this.selectedDate = '';
    this.selectedTime = '';
  }

  private parseLocalDate(value: string): Date {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
  }

  private formatDate(value: Date): string {
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const day = String(value.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
