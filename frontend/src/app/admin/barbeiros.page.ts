import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminLayout } from './layout';
import { BarbeiroForm } from './barbeiro-form';

interface Barbeiro { id: number; nome: string; telefone: string; imgUrl: string | null; comissao: number | null; ativo: boolean }
interface Listagem { itens: Barbeiro[]; total: number; totalPaginas: number; pagina: number }

@Component({
  selector: 'app-admin-barbeiros', imports: [AdminLayout, FormsModule, BarbeiroForm],
  templateUrl: './barbeiros.page.html', styleUrl: './barbeiros.page.scss'
})
export class AdminBarbeirosPage implements OnInit, OnDestroy {
  resultado = signal<Listagem | null>(null);
  loading = signal(true);
  error = signal('');
  busca = '';
  status = '';
  formOpen = signal(false);
  selectedId: number | null = null;
  success = signal('');
  abrir(id: number | null = null) { this.selectedId = id; this.success.set(''); this.formOpen.set(true); }
  salvo() { this.formOpen.set(false); this.success.set('Cadastro do barbeiro salvo com sucesso.'); void this.load(this.resultado()?.pagina || 0); }
  private controller?: AbortController;
  ngOnInit() { void this.load(); }
  ngOnDestroy() { this.controller?.abort(); }
  iniciais(nome: string) { return nome.trim().split(/\s+/).slice(0, 2).map(n => n[0]).join('').toUpperCase(); }
  async load(pagina = 0) {
    this.controller?.abort();
    const controller = new AbortController(); this.controller = controller;
    this.loading.set(true); this.error.set('');
    const params = new URLSearchParams({ busca: this.busca.trim(), pagina: String(pagina) });
    if (this.status !== '') params.set('ativo', this.status);
    try {
      const response = await fetch('/api/admin/barbeiros?' + params, { signal: controller.signal, headers: { Accept: 'application/json' } });
      if (response.redirected || response.status === 401) { window.location.assign('/login'); return; }
      if (!response.ok) throw new Error(response.status === 403 ? 'Acesso permitido somente ao administrador.' : 'Não foi possível carregar os barbeiros.');
      this.resultado.set(await response.json());
    } catch (e) { if (!controller.signal.aborted) this.error.set((e as Error).message); }
    finally { if (!controller.signal.aborted) this.loading.set(false); }
  }
}
