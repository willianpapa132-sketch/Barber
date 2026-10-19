import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { RouterLink } from '@angular/router';
import { AdminLayout } from './layout';

registerLocaleData(pt);

interface Resumo { data: string; agendamentosHoje: number; valorLiquidoHoje: number }

@Component({
  selector: 'app-admin-home', imports: [CurrencyPipe, RouterLink, AdminLayout],
  templateUrl: './home.page.html', styleUrl: './home.page.scss'
})
export class AdminHomePage implements OnInit, OnDestroy {
  resumo = signal<Resumo | null>(null);
  loading = signal(true);
  error = signal('');
  private controller?: AbortController;
  readonly cards = ['Agenda', 'Dashboard', 'Cadastros', 'Comissões', 'Financeiro', 'Caixa'];
  get data() { return this.resumo() ? new Date(this.resumo()!.data + 'T12:00:00').toLocaleDateString('pt-BR', { dateStyle: 'long' }) : ''; }
  ngOnInit() { void this.load(); }
  ngOnDestroy() { this.controller?.abort(); }
  async load() {
    this.controller?.abort();
    this.controller = new AbortController();
    this.loading.set(true); this.error.set('');
    try {
      const response = await fetch('/api/admin/home', { signal: this.controller.signal, headers: { Accept: 'application/json' } });
      if (response.redirected || response.status === 401) { window.location.assign('/login'); return; }
      if (!response.ok) throw new Error(response.status === 403 ? 'Acesso permitido somente ao administrador.' : 'Não foi possível carregar o resumo.');
      this.resumo.set(await response.json());
    } catch (e) { if ((e as Error).name !== 'AbortError') this.error.set((e as Error).message); }
    finally { this.loading.set(false); }
  }
}
