import { Component, input, inject, OnInit } from '@angular/core';
import { EmpresaService } from './empresa.service';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-admin-layout', imports: [RouterLink, RouterLinkActive],
  styleUrl: './home.page.scss',
  styles: ['@media(max-width:760px){nav{flex-wrap:wrap}nav a{gap:8px}}'],
  template: `
    <div class="admin-layout">
      <aside>
        <div class="brand"><span aria-hidden="true">✂</span><div>{{empresa.nome()}}<small>ADMINISTRATIVO</small></div></div>
        <nav aria-label="Menu administrativo">
          <a routerLink="/admin/home" routerLinkActive="selected" ariaCurrentWhenActive="page">⌂ <span>Home</span></a>
          <a routerLink="/admin/agendamentos" routerLinkActive="selected" ariaCurrentWhenActive="page">▦ <span>Agendamentos</span></a>
          <a routerLink="/admin/dashboard" routerLinkActive="selected" ariaCurrentWhenActive="page">▥ <span>Dashboard</span></a>
          <a routerLink="/admin/cadastros" routerLinkActive="selected" ariaCurrentWhenActive="page">♙ <span>Cadastros</span></a>
          <a routerLink="/admin/comissao" routerLinkActive="selected" ariaCurrentWhenActive="page">% <span>Comissões</span></a>
          <a routerLink="/admin/gerenciamento-planos" routerLinkActive="selected" ariaCurrentWhenActive="page">▤ <span>Gerenciamento de Planos</span></a>
          <a routerLink="/admin/financeiro" routerLinkActive="selected" ariaCurrentWhenActive="page">$ <span>Financeiro</span></a>
          <a routerLink="/admin/caixa" routerLinkActive="selected" ariaCurrentWhenActive="page">▣ <span>Caixa</span></a>
          <a routerLink="/admin/configuracao" routerLinkActive="selected" ariaCurrentWhenActive="page">⚙ <span>Configurações</span></a>
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
export class AdminLayout implements OnInit {
  empresa=inject(EmpresaService);
  ngOnInit(){void this.empresa.carregar();}
}
