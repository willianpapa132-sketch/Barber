import { Component, ElementRef, OnInit, ViewChild, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { FormsModule } from '@angular/forms';
import { AdminLayout } from './layout';
import { RouterLink } from '@angular/router';

registerLocaleData(pt);
interface Caixa {
  id: number; saldoInicial: number; totalEntradas: number; totalSangrias: number;
  saldoAtual: number; saldoFinal: number | null; status: 'ABERTO' | 'FECHADO';
  abertoEm: string; fechadoEm: string | null;
}
@Component({
  selector: 'app-admin-caixa', imports: [AdminLayout, FormsModule, CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './caixa.page.html', styleUrl: './caixa.page.scss'
})
export class AdminCaixaPage implements OnInit {
  @ViewChild('dialog', { static: true }) dialog!: ElementRef<HTMLDialogElement>;
  caixa = signal<Caixa | null>(null);
  caixas = signal<Caixa[]>([]);
  selecionado = signal<Caixa | null>(null);
  loading = signal(true); saving = signal(false); error = signal(''); formError = signal(''); success = signal('');
  acao: 'abrir' | 'sangria' | 'fechar' = 'abrir';
  valor: number | null = 0; observacao = '';
  ngOnInit() { void this.load(); }
  private async request<T>(path = '', options: RequestInit = {}): Promise<T> {
    const response = await fetch('/api/admin/caixa' + path, { ...options, headers: { Accept: 'application/json', 'Content-Type': 'application/json' } });
    if (response.redirected || response.status === 401) { window.location.assign('/login'); throw new Error('Entre novamente para continuar.'); }
    const data = await response.json().catch(() => null);
    if (!response.ok) throw new Error(data?.mensagem || data?.detail || data?.message || 'Não foi possível concluir a operação. Tente novamente.');
    if (!data) throw new Error('Resposta inválida ao consultar o caixa.');
    return data;
  }
  async load() {
    this.loading.set(true); this.error.set('');
    try {
      const caixas = await this.request<Caixa[]>('/historico');
      this.caixas.set(caixas);
      this.caixa.set(caixas.find(c => c.status === 'ABERTO') ?? null);
      const id = this.selecionado()?.id;
      this.selecionado.set(caixas.find(c => c.id === id) ?? null);
    }
    catch (e) { this.error.set((e as Error).message); }
    finally { this.loading.set(false); }
  }
  abrir(acao: 'abrir' | 'sangria' | 'fechar') {
    this.acao = acao; this.valor = acao === 'abrir' ? 0 : null; this.observacao = ''; this.formError.set('');
    this.dialog.nativeElement.showModal();
  }
  fecharDialog() { if (!this.saving()) this.dialog.nativeElement.close(); }
  valorValido() {
    return this.valor !== null && Number.isFinite(this.valor) && this.valor >= (this.acao === 'abrir' ? 0 : 0.01)
      && this.valor <= (this.acao === 'sangria' ? this.caixa()?.saldoAtual ?? 0 : 9999999999.99)
      && Math.abs(this.valor * 100 - Math.round(this.valor * 100)) < 0.0001;
  }
  async confirmar() {
    if (this.saving() || (this.acao !== 'fechar' && !this.valorValido())) return;
    this.saving.set(true); this.formError.set(''); this.success.set('');
    try {
      const path = this.acao === 'abrir' ? '' : this.acao === 'sangria' ? '/sangrias' : '/fechamento';
      const body = this.acao === 'abrir' ? { saldoInicial: this.valor } : { valor: this.valor, observacao: this.observacao.trim() };
      const caixa = await this.request<Caixa>(path, { method: 'POST', ...(this.acao === 'fechar' ? {} : { body: JSON.stringify(body) }) });
      this.caixas.update(caixas => [caixa, ...caixas.filter(c => c.id !== caixa.id)].sort((a, b) => b.id - a.id));
      this.caixa.set(caixa.status === 'ABERTO' ? caixa : null);
      if (this.selecionado()) this.selecionado.set(caixa);
      this.error.set(''); this.dialog.nativeElement.close();
      this.success.set(this.acao === 'abrir' ? 'Caixa aberto com sucesso.' : this.acao === 'sangria' ? 'Sangria registrada com sucesso.' : 'Caixa fechado com sucesso.');
    } catch (e) { this.formError.set((e as Error).message); }
    finally { this.saving.set(false); }
  }
}
