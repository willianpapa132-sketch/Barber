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
    <form action="/logout" method="post"><button type="submit"><span aria-hidden="true">↪</span>Sair</button></form>
  </nav>`,
})
export class BarberNav {}
