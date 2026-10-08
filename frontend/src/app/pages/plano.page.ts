import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BottomNav } from '../shared/bottom-nav';

@Component({
  selector: 'app-plano-page',
  imports: [RouterLink, BottomNav],
  template: `
    <main class="client-shell simple-client-page">
      <a class="simple-back" routerLink="/">‹ Inicio</a>
      <h1>Plano</h1>
    </main>
    <app-bottom-nav current="plano" />
  `,
})
export class PlanoPage {}
