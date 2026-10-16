import { Component, HostListener, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BarberApi, dateLabel, hhmm, money } from './api';
import { Finish } from './models';

@Component({ selector: 'app-barber-finish', imports: [FormsModule, RouterLink], templateUrl: './finish.page.html' })
export class BarberFinishPage implements OnInit, OnDestroy {
  private api = inject(BarberApi);
  private route = inject(ActivatedRoute);
  private controller = new AbortController();
  private timer?: ReturnType<typeof setInterval>;
  private url = `/api/barbeiro/agendamentos/${this.route.snapshot.paramMap.get('id')}/finalizacao`;
  appointment = signal<Finish | null>(null); busy = signal(false); error = signal(''); completed = signal(false);
  selected: string | null = null;
  money = money; dateLabel = dateLabel; hhmm = hhmm;
  names: Record<string, string> = { DINHEIRO: 'Dinheiro', PIX: 'Pix', DEBITO: 'Débito', CREDITO: 'Crédito', MENSAL: 'Mensal' };
  icons: Record<string, string> = { DINHEIRO: '＄', PIX: '◇', DEBITO: '▣', CREDITO: '▤', MENSAL: '▦' };
  ngOnInit() { void this.load(); this.timer = setInterval(() => this.recheck(), 30000); }
  ngOnDestroy() { this.controller.abort(); clearInterval(this.timer); }
  @HostListener('window:focus') recheck() {
    if (document.visibilityState === 'visible' && this.appointment() && !this.appointment()!.podeFinalizar && !this.completed() && !this.busy()) void this.load();
  }
  get initials() { return (this.appointment()?.nomeCliente || 'Cliente').trim().split(/\s+/).slice(0, 2).map(s => s[0]).join('').toUpperCase(); }
  percent(value: number) { return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(value); }
  async load(method: string | null = this.selected) {
    if (this.busy() || this.completed()) return;
    this.busy.set(true); this.error.set('');
    try {
      const data = await this.api.request<Finish>(this.url + (method ? `?formaPagamento=${encodeURIComponent(method)}` : ''), { signal: this.controller.signal });
      this.appointment.set(data); this.selected = data.formaPagamento || (method && data.formasPagamentoDisponiveis.includes(method) ? method : null);
      this.completed.set(data.status === 'CONCLUIDO');
    } catch (e) {
      if (!this.controller.signal.aborted) { this.appointment.update(a => a ? { ...a, podeFinalizar: false } : a); this.error.set((e as Error).message); }
    } finally { this.busy.set(false); }
  }
  async finish() {
    if (this.busy() || this.completed() || !this.appointment()?.podeFinalizar || !this.selected) return;
    this.busy.set(true); this.error.set('');
    try {
      this.appointment.set(await this.api.request<Finish>(this.url, { method: 'POST', body: JSON.stringify({ formaPagamento: this.selected }), signal: this.controller.signal }));
      this.completed.set(true);
    } catch (e) {
      if (!this.controller.signal.aborted) { this.appointment.update(a => a ? { ...a, podeFinalizar: false } : a); this.error.set((e as Error).message); }
    } finally { this.busy.set(false); }
  }
}
