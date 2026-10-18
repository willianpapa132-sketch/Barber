import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BarberApi, dateLabel, hhmm, money } from './api';
import { BarberNav } from './layout';
import { fimMaximoCarteira, hojeCarteira, inicioMesCarteira, validarPeriodoCarteira } from './periodo-carteira';

interface StatementItem {
  agendamentoId: number; data: string; horaInicio: string; horaFim: string; duracaoMinutos: number;
  nomeCliente: string; servicos: string[]; nomePlano: string | null; pontos: number | null;
  valorBase: number | null; percentualAplicado: number | null; valorComissao: number | null;
}
interface Statement {
  inicio: string; fim: string; totalPontos?: number; valorLiquido?: number;
  totalAtendimentos: number; pagina: number; totalPaginas: number; atendimentos: StatementItem[];
}

@Component({
  selector: 'app-barber-statement', imports: [FormsModule, RouterLink, BarberNav],
  templateUrl: './valores-formulado.page.html', styleUrls: ['./carteira.page.scss', './valores-formulado.page.scss'],
})
export class BarberStatementPage implements OnInit, OnDestroy {
  private api = inject(BarberApi); private route = inject(ActivatedRoute); private router = inject(Router);
  private controller?: AbortController;
  readonly tipo: 'pontos' | 'comissoes' = this.route.snapshot.data['tipo'];
  readonly isPoints = this.tipo === 'pontos';
  inicio = this.route.snapshot.queryParamMap.get('inicio') ?? inicioMesCarteira();
  fim = this.route.snapshot.queryParamMap.get('fim') ?? hojeCarteira();
  loading = signal(false); error = signal(''); statement = signal<Statement | null>(null);
  money = money; dateLabel = dateLabel; hhmm = hhmm;
  get maxFim() { return fimMaximoCarteira(this.inicio); }
  get validation() { return validarPeriodoCarteira(this.inicio, this.fim); }
  get backPeriod() {
    const data = this.statement(); return { inicio: data?.inicio ?? this.inicio, fim: data?.fim ?? this.fim };
  }
  get periodChanged() {
    const data = this.statement(); return !!data && (data.inicio !== this.inicio || data.fim !== this.fim);
  }
  ngOnInit() {
    const page = Number(this.route.snapshot.queryParamMap.get('pagina') ?? 0);
    if (!Number.isSafeInteger(page) || page < 0) { this.error.set('Página inválida. Use Filtrar para reiniciar a consulta.'); return; }
    void this.load(page);
  }
  ngOnDestroy() { this.controller?.abort(); }
  month(date: string) { return new Date(`${date}T12:00:00`).toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '').toUpperCase(); }
  duration(minutes: number) { return minutes < 60 ? `${minutes} min` : `${Math.floor(minutes / 60)}h ${String(minutes % 60).padStart(2, '0')}min`; }
  async load(pagina = 0) {
    if (this.validation) return;
    this.controller?.abort(); const controller = this.controller = new AbortController();
    this.loading.set(true); this.error.set(''); this.statement.set(null);
    const query = new URLSearchParams({ inicio: this.inicio, fim: this.fim, pagina: String(pagina) });
    try {
      const data = await this.api.request<Statement>(`/api/barbeiro/carteira/${this.tipo}?${query}`, { signal: controller.signal });
      if (controller.signal.aborted) return;
      this.statement.set(data);
      await this.router.navigate([], { relativeTo: this.route, queryParams: { inicio: data.inicio, fim: data.fim, pagina: data.pagina || null }, replaceUrl: true });
    } catch (e) { if (!controller.signal.aborted) this.error.set((e as Error).message); }
    finally { if (!controller.signal.aborted) this.loading.set(false); }
  }
}
