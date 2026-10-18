import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { fimMaximoCarteira, hojeCarteira, inicioMesCarteira, validarPeriodoCarteira } from './periodo-carteira';
import { BarberApi, dateLabel, money } from './api';
import { BarberNav } from './layout';

interface Wallet {
  inicio: string; fim: string; minutosTrabalhados: number; pontos: number; valorLiquido: number;
}

@Component({
  selector: 'app-barber-wallet', imports: [FormsModule, RouterLink, BarberNav],
  templateUrl: './carteira.page.html', styleUrl: './carteira.page.scss',
})
export class BarberWalletPage implements OnInit, OnDestroy {
  private api = inject(BarberApi);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private controller?: AbortController;
  inicio = this.route.snapshot.queryParamMap.get('inicio') ?? inicioMesCarteira();
  fim = this.route.snapshot.queryParamMap.get('fim') ?? hojeCarteira();
  loading = signal(false); error = signal(''); wallet = signal<Wallet | null>(null);
  money = money; dateLabel = dateLabel;
  get maxFim() { return fimMaximoCarteira(this.inicio); }
  get validation() { return validarPeriodoCarteira(this.inicio, this.fim); }
  get duration(): string {
    const minutes = this.wallet()?.minutosTrabalhados ?? 0;
    return `${Math.floor(minutes / 60)}h ${String(minutes % 60).padStart(2, '0')}min`;
  }
  get periodChanged(): boolean {
    const data = this.wallet(); return !!data && (data.inicio !== this.inicio || data.fim !== this.fim);
  }
  ngOnInit() { void this.load(); }
  ngOnDestroy() { this.controller?.abort(); }
  async load() {
    if (this.validation) return;
    this.controller?.abort(); const controller = this.controller = new AbortController();
    this.loading.set(true); this.error.set(''); this.wallet.set(null);
    const query = new URLSearchParams({ inicio: this.inicio, fim: this.fim });
    try {
      const data = await this.api.request<Wallet>(`/api/barbeiro/carteira?${query}`, { signal: controller.signal });
      if (controller.signal.aborted) return;
      this.wallet.set(data);
      await this.router.navigate([], { relativeTo: this.route, queryParams: { inicio: data.inicio, fim: data.fim }, replaceUrl: true });
    } catch (e) {
      if (!controller.signal.aborted) this.error.set((e as Error).message);
    } finally { if (!controller.signal.aborted) this.loading.set(false); }
  }
}
