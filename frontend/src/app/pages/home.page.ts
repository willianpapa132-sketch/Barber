import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BottomNav } from '../shared/bottom-nav';

interface HomeResponse {
  cliente: {
    nome: string;
    imgUrl: string | null;
  };
  proximoAtendimento: AppointmentSummary | null;
}

interface AppointmentSummary {
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

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, BottomNav],
  templateUrl: './home.page.html',
})
export class HomePage implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly fallbackAvatar =
    'https://images.unsplash.com/photo-1628157588553-5eeea00af15c?auto=format&fit=crop&w=300&q=80';

  protected loading = true;
  protected errorMessage = '';
  protected clientName = 'Cliente';
  protected clientAvatarUrl: string | null = null;
  protected nextAppointment: AppointmentSummary | null = null;

  ngOnInit(): void {
    this.loading = true;
    this.errorMessage = '';

    this.http.get<HomeResponse>('/api/cliente/home').subscribe({
      next: (response) => {
        this.clientName = response.cliente.nome;
        this.clientAvatarUrl = response.cliente.imgUrl;
        this.nextAppointment = response.proximoAtendimento;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Nao foi possivel carregar sua home.';
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
