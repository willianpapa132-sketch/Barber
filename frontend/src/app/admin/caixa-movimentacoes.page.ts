import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AdminLayout } from './layout';

registerLocaleData(pt);
interface Movimento {
  id: number; tipo: 'ENTRADA' | 'SANGRIA'; valor: number;
  criadoEm: string; observacao: string | null; agendamentoId: number | null;
}
@Component({
  selector: 'app-caixa-movimentacoes',
  imports: [AdminLayout, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './caixa-movimentacoes.page.html', styleUrl: './caixa.page.scss'
})
export class CaixaMovimentacoesPage implements OnInit {
  readonly id = inject(ActivatedRoute).snapshot.paramMap.get('id');
  movimentos = signal<Movimento[]>([]);
  loading = signal(true);
  error = signal('');
  ngOnInit() { void this.load(); }
  async load() {
    this.loading.set(true); this.error.set('');
    try {
      if (!this.id || !/^\d+$/.test(this.id)) throw new Error('Caixa inválido.');
      const response = await fetch(`/api/admin/caixa/${this.id}/movimentacoes`, { headers: { Accept: 'application/json' } });
      if (response.redirected || response.status === 401) {
        window.location.assign('/login'); throw new Error('Entre novamente para continuar.');
      }
      const data = await response.json().catch(() => null);
      if (!response.ok) throw new Error(data?.mensagem || 'Não foi possível carregar as movimentações.');
      if (!Array.isArray(data)) throw new Error('Resposta inválida ao consultar as movimentações.');
      this.movimentos.set(data);
    } catch (e) { this.error.set((e as Error).message); }
    finally { this.loading.set(false); }
  }
}
