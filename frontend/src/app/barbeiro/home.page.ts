import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BarberApi, hhmm } from './api';
import { BarberHome } from './models';
import { BarberNav } from './layout';

@Component({ selector: 'app-barber-home', imports: [RouterLink, BarberNav], templateUrl: './home.page.html' })
export class BarberHomePage implements OnInit, OnDestroy {
  private api = inject(BarberApi);
  private controller?: AbortController;
  home = signal<BarberHome | null>(null);
  error = signal('');
  loading = signal(true);
  hhmm = hhmm;
  get formattedDate() {
    return this.home() ? new Date(this.home()!.data + 'T12:00:00').toLocaleDateString('pt-BR', { day: 'numeric', month: 'long', year: 'numeric' }) : '';
  }
  ngOnInit() { void this.load(); }
  ngOnDestroy() { this.controller?.abort(); }
  async load() {
    this.controller?.abort(); this.controller = new AbortController();
    this.loading.set(true); this.error.set('');
    try { this.home.set(await this.api.request<BarberHome>('/api/barbeiro/home', { signal: this.controller.signal })); }
    catch (e) { if ((e as Error).name !== 'AbortError') this.error.set((e as Error).message); }
    finally { this.loading.set(false); }
  }
}
