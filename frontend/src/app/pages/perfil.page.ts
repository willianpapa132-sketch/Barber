import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BottomNav } from '../shared/bottom-nav';

@Component({
  selector: 'app-perfil-page',
  imports: [RouterLink, BottomNav],
  template: `
    <main class="client-shell simple-client-page">
      <a class="simple-back" routerLink="/">‹ Inicio</a>
      <h1>Perfil</h1>
    </main>
    <app-bottom-nav current="perfil" />
  `,
})
export class PerfilPage {}
