import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { FormsModule } from '@angular/forms';
import { AdminLayout } from './layout';
import { BarberApi } from '../barbeiro/api';
registerLocaleData(pt);
interface Resumo { id: number; nome: string; ativo: boolean; minutosTrabalhados: number; pontosPlano: number; valorComissoes: number }
interface Comissao {
  inicio: string; fim: string; hoje: string; barbeiroId: number | null;
  minutosTrabalhados: number; pontosPlano: number; valorComissoes: number; comissoesHoje: number; barbeiros: Resumo[];
}
@Component({ selector: 'app-admin-comissao', imports: [AdminLayout, FormsModule, CurrencyPipe, DatePipe],
  templateUrl: './comissao.page.html', styleUrl: './comissao.page.scss' })
export class AdminComissaoPage implements OnInit, OnDestroy {
  private api = inject(BarberApi);
  private controller?: AbortController;
  dados = signal<Comissao | null>(null); loading = signal(true); error = signal('');
  inicio = ''; fim = ''; barbeiroId = '';
  ngOnInit() { void this.load(true); }
  ngOnDestroy() { this.controller?.abort(); }
  horas(minutos: number) { return `${Math.floor(minutos / 60)}h ${minutos % 60}min`; }
  async load(inicial = false) {
    if (!inicial && (!this.inicio || !this.fim || this.fim < this.inicio)) {
      this.error.set('Informe um período válido, com data inicial e final.'); return;
    }
    this.controller?.abort(); const controller = this.controller = new AbortController();
    this.loading.set(true); this.error.set('');
    const query = new URLSearchParams();
    if (!inicial) { query.set('inicio', this.inicio); query.set('fim', this.fim); }
    if (this.barbeiroId) query.set('barbeiroId', this.barbeiroId);
    try {
      const dados = await this.api.request<Comissao>(`/api/admin/comissao?${query}`, { signal: controller.signal });
      if (controller.signal.aborted) return;
      this.dados.set(dados); this.inicio = dados.inicio; this.fim = dados.fim;
    } catch (e) { if (!controller.signal.aborted) this.error.set((e as Error).message); }
    finally { if (!controller.signal.aborted) this.loading.set(false); }
  }
  selecionar(id: number) { this.barbeiroId = String(id); void this.load(); }
}
