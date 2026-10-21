import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminLayout } from './layout';
import { BarberAgendaPage } from '../barbeiro/agenda.page';
import { BarberApi } from '../barbeiro/api';
import { Grade } from '../barbeiro/models';

interface Barbeiro { id: number; nome: string; ativo: boolean }
@Component({
  selector: 'app-admin-agendamentos', imports: [AdminLayout, FormsModule, BarberAgendaPage],
  templateUrl: './agendamentos.page.html', styleUrl: './agendamentos.page.scss'
})
export class AdminAgendamentosPage implements OnInit, OnDestroy {
  private api = inject(BarberApi);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private controller?: AbortController;
  date = this.route.snapshot.queryParamMap.get('data') || this.today();
  status = this.route.snapshot.queryParamMap.get('status') || '';
  barbeiroId = '';
  barbeiros = signal<Barbeiro[]>([]);
  colunas = signal<{ barbeiro: Barbeiro; grade: Grade }[]>([]);
  range = signal<{ inicio: string; fim: string } | null>(null);
  loading = signal(true); error = signal('');
  private today() { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; }
  ngOnInit() { void this.load(); }
  ngOnDestroy() { this.controller?.abort(); }
  async load() {
    this.controller?.abort(); const controller = this.controller = new AbortController();
    this.loading.set(true); this.error.set('');
    try {
      const barbeiros = await this.api.request<Barbeiro[]>('/api/admin/agendamentos/barbeiros', { signal: controller.signal });
      const query = new URLSearchParams({ data: this.date });
      if (this.status) query.set('status', this.status);
      const colunas = await Promise.all(barbeiros.filter(b => !this.barbeiroId || String(b.id) === this.barbeiroId).map(async barbeiro => ({
        barbeiro, grade: await this.api.request<Grade>(`/api/admin/agendamentos/${barbeiro.id}/grade?${query}`, { signal: controller.signal })
      })));
      if (controller.signal.aborted) return;
      const inicios = colunas.map(c => c.grade.inicio).filter(Boolean).sort();
      const fins = colunas.map(c => c.grade.fim).filter(Boolean).sort();
      this.range.set(inicios.length ? { inicio: inicios[0], fim: fins[fins.length - 1] } : null);
      this.barbeiros.set(barbeiros); this.colunas.set(colunas);
      await this.router.navigate([], { relativeTo: this.route, queryParams: { data: this.date, status: this.status || null }, replaceUrl: true });
    } catch (e) { if (!controller.signal.aborted) this.error.set((e as Error).message); }
    finally { if (!controller.signal.aborted) this.loading.set(false); }
  }
  changeDay(offset: number) {
    const d = new Date(this.date + 'T12:00:00'); d.setDate(d.getDate() + offset);
    this.date = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    void this.load();
  }
  goToday() { this.date = this.today(); void this.load(); }
}
