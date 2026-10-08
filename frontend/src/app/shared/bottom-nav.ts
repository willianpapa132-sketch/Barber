import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

type NavKey = 'inicio' | 'plano' | 'agendamentos' | 'perfil';

@Component({
  selector: 'app-bottom-nav',
  imports: [RouterLink],
  template: `
    <nav class="client-bottom-nav" aria-label="Navegacao principal">
      <a [class.active]="current() === 'inicio'" routerLink="/">
        <span aria-hidden="true">⌂</span>
        <small>Inicio</small>
      </a>
      <a [class.active]="current() === 'plano'" routerLink="/plano">
        <span aria-hidden="true">♕</span>
        <small>Plano</small>
      </a>
      <a [class.active]="current() === 'agendamentos'" routerLink="/agendamentos">
        <span aria-hidden="true">▣</span>
        <small>Meus Agendamentos</small>
      </a>
      <a [class.active]="current() === 'perfil'" routerLink="/perfil">
        <span aria-hidden="true">♙</span>
        <small>Perfil</small>
      </a>
    </nav>
  `,
})
export class BottomNav {
  readonly current = input.required<NavKey>();
}
