import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BottomNav } from '../shared/bottom-nav';

interface ClientePerfil {
  id: number;
  nome: string;
  telefone: string;
  imgUrl: string | null;
}

@Component({
  selector: 'app-perfil-page',
  imports: [RouterLink, BottomNav],
  template: `
    <main class="client-shell profile-page-shell">
      <header class="profile-header">
        <h1>Perfil</h1>
        <span class="profile-bell" aria-hidden="true">&#128276;</span>
      </header>

      <section class="profile-summary">
        <div class="profile-photo-wrap">
          <img class="profile-photo" [alt]="'Foto de ' + profile.nome" [src]="avatarUrl" />
          <a class="profile-edit-fab" routerLink="/perfil/editar" aria-label="Editar perfil">&#9998;</a>
        </div>
        <div>
          <strong>{{ profile.nome }}</strong>
          <p><span aria-hidden="true">&#9742;</span>{{ profile.telefone }}</p>
        </div>
      </section>

      @if (errorMessage) {
        <section class="profile-state-card">
          <strong>{{ errorMessage }}</strong>
          <button type="button" (click)="loadProfile()">Tentar novamente</button>
        </section>
      }

      <nav class="profile-menu" aria-label="Opcoes do perfil">
        <a routerLink="/perfil/editar">
          <span class="profile-menu-icon" aria-hidden="true">&#9817;</span>
          <strong>Editar perfil</strong>
          <i aria-hidden="true">&rsaquo;</i>
        </a>
        <a routerLink="/agendamentos">
          <span class="profile-menu-icon calendar" aria-hidden="true">&#128197;</span>
          <strong>Meus agendamentos</strong>
          <i aria-hidden="true">&rsaquo;</i>
        </a>
        <a routerLink="/plano">
          <span class="profile-menu-icon" aria-hidden="true">&#9819;</span>
          <strong>Meu plano mensal</strong>
          <i aria-hidden="true">&rsaquo;</i>
        </a>
        <button type="button" (click)="logout()">
          <span class="profile-menu-icon" aria-hidden="true">&#8618;</span>
          <strong>Sair da conta</strong>
          <i aria-hidden="true">&rsaquo;</i>
        </button>
      </nav>
    </main>
    <app-bottom-nav current="perfil" />
  `,
})
export class PerfilPage implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly fallbackAvatar =
    'https://images.unsplash.com/photo-1628157588553-5eeea00af15c?auto=format&fit=crop&w=300&q=80';

  protected errorMessage = '';
  protected profile: ClientePerfil = {
    id: 0,
    nome: 'Cliente',
    telefone: '',
    imgUrl: null,
  };

  protected get avatarUrl(): string {
    return this.profile.imgUrl || this.fallbackAvatar;
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  protected loadProfile(): void {
    this.errorMessage = '';

    this.http.get<ClientePerfil>('/api/cliente/perfil').subscribe({
      next: (profile) => {
        this.profile = profile;
      },
      error: () => {
        this.errorMessage = 'Nao foi possivel carregar seu perfil.';
      },
    });
  }

  protected logout(): void {
    const form = document.createElement('form');
    form.method = 'post';
    form.action = '/logout';
    document.body.appendChild(form);
    form.submit();
  }
}
