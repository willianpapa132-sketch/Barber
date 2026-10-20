import { Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-admin-layout', imports: [RouterLink, RouterLinkActive],
  styleUrl: './home.page.scss',
  template: `
    <div class="admin-layout">
      <aside>
        <div class="brand"><span aria-hidden="true">✂</span><div>BARBEARIA<small>ADMINISTRATIVO</small></div></div>
        <nav aria-label="Menu administrativo">
          <a routerLink="/admin/home" routerLinkActive="selected" ariaCurrentWhenActive="page">⌂ <span>Home</span></a>
          <span class="nav-pending">Ver Agendamentos <small>Em breve</small></span>
          <span class="nav-pending">Dashboard <small>Em breve</small></span>
          <a routerLink="/admin/cadastros" routerLinkActive="selected" ariaCurrentWhenActive="page">♙ <span>Cadastros</span></a>
          <span class="nav-pending">Comissões <small>Em breve</small></span>
          <span class="nav-pending">Financeiro <small>Em breve</small></span>
          <a routerLink="/admin/caixa" routerLinkActive="selected" ariaCurrentWhenActive="page">▣ <span>Caixa</span></a>
          <span class="nav-pending">Configurações <small>Em breve</small></span>
        </nav>
        <form action="/logout" method="post"><button type="submit">Sair da conta ↗</button></form>
      </aside>
      <div class="workspace">
        <header><span>Gestão da barbearia</span><div><span class="avatar">AD</span><span>Administrador<small>Barbearia</small></span></div></header>
        <main><ng-content /></main>
      </div>
    </div>
  `
})
export class AdminLayout {}
