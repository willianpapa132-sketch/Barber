import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-barber-layout', imports: [RouterOutlet],
  template: '<div class="barber-area"><router-outlet /></div>',
})
export class BarberLayout {}

@Component({
  selector: 'app-barber-nav', imports: [RouterLink, RouterLinkActive],
  template: `<nav class="bottom-nav" aria-label="Navegação do barbeiro">
    <a routerLink="/barbeiro/home" routerLinkActive="active" ariaCurrentWhenActive="page"><span aria-hidden="true">⌂</span>Home</a>
    <a routerLink="/barbeiro/agenda" routerLinkActive="active" ariaCurrentWhenActive="page"><span aria-hidden="true">▦</span>Agenda</a>
    <a routerLink="/barbeiro/carteira" routerLinkActive="active" ariaCurrentWhenActive="page"><svg aria-hidden="true" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M20 7H5a2 2 0 0 1 0-4h13v4M4 5v14a2 2 0 0 0 2 2h14V7M20 12h-6v5h6"/><circle cx="16.5" cy="14.5" r=".6" fill="currentColor"/></svg>Carteira</a>
    <form action="/logout" method="post"><button type="submit"><span aria-hidden="true">↪</span>Sair</button></form>
  </nav>`,
})
export class BarberNav {}
