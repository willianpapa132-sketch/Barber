import { Component, ElementRef, OnInit, ViewChild, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { AdminLayout } from './layout';
registerLocaleData(pt);
interface Servico { id: number; nome: string; preco: number; duracaoMinutos: number; ativo: boolean }
interface Desconto { porcentagemDesconto: number; servicos: Servico[] }
interface Plano { id: number; nomePlano: string; descricao: string; valorMensal: number; ativo: boolean; diasMaximoAntecedencia: number | null; servicosIncluidos: Servico[]; descontosServicos: Desconto[]; podeExcluir: boolean }
interface Lista { itens: Plano[]; total: number; totalPaginas: number; pagina: number }
interface Regra { servicoId: number | null; percentual: number }
@Component({selector:'app-admin-planos-mensais', imports:[AdminLayout, FormsModule, CurrencyPipe], templateUrl:'./planos-mensais.page.html', styleUrl:'./planos-mensais.page.scss'})
export class AdminPlanosMensaisPage implements OnInit {
  @ViewChild('dialog', {static:true}) dialog!: ElementRef<HTMLDialogElement>;
  resultado = signal<Lista | null>(null); loading = signal(true); error = signal(''); success = signal('');
  saving = signal(false); formLoading = signal(false); formError = signal('');
  busca = ''; status = ''; selectedId: number | null = null; leitura = false;
  servicos: Servico[] = []; incluidos: number[] = []; regras: Regra[] = []; comDescontos = false;
  model = {nomePlano:'', descricao:'', valorMensal:0, ativo:true, diasMaximoAntecedencia:7 as number | null};
  private sequence = 0; private formSequence = 0;
  ngOnInit() { void this.load(); }
  async request(path: string, options: RequestInit = {}) {
    const response = await fetch('/api/admin/planos-mensais' + path, {...options, headers:{Accept:'application/json','Content-Type':'application/json'}});
    if (response.redirected || response.status === 401) { window.location.assign('/login'); throw new Error('Entre novamente.'); }
    const data = await response.json().catch(() => null);
    if (!response.ok) throw new Error(data?.mensagem || (response.status === 403 ? 'Acesso permitido somente ao administrador.' : 'Não foi possível concluir a operação.'));
    return data;
  }
  async load(pagina = 0) {
    const sequence = ++this.sequence; this.loading.set(true); this.error.set('');
    const params = new URLSearchParams({busca:this.busca.trim(), pagina:String(pagina)});
    if (this.status !== '') params.set('ativo', this.status);
    try {
      const lista: Lista = await this.request('?' + params);
      if (sequence !== this.sequence) return;
      if (pagina > 0 && !lista.itens.length) { void this.load(Math.max(0, lista.totalPaginas - 1)); return; }
      this.resultado.set(lista);
    } catch(e) { if (sequence === this.sequence) this.error.set((e as Error).message); }
    finally { if (sequence === this.sequence) this.loading.set(false); }
  }
  async abrir(plano: Plano | null = null, leitura = false) {
    this.selectedId = plano?.id ?? null; this.leitura = leitura;
    this.model = {nomePlano:'', descricao:'', valorMensal:0, ativo:true, diasMaximoAntecedencia:7};
    this.incluidos = []; this.regras = []; this.comDescontos = false; this.servicos = [];
    this.dialog.nativeElement.showModal(); await this.carregarFormulario();
  }
  async carregarFormulario() {
    const sequence = ++this.formSequence;
    this.formLoading.set(true); this.formError.set('');
    try {
      const [servicos, plano]: [Servico[], Plano | null] = await Promise.all([
        this.request('/servicos'), this.selectedId === null ? Promise.resolve(null) : this.request('/' + this.selectedId)
      ]);
      if (sequence !== this.formSequence) return;
      this.servicos = servicos;
      if (plano) {
        this.model = {nomePlano:plano.nomePlano, descricao:plano.descricao, valorMensal:plano.valorMensal, ativo:plano.ativo, diasMaximoAntecedencia:plano.diasMaximoAntecedencia};
        this.incluidos = plano.servicosIncluidos.map(s => s.id);
        this.regras = plano.descontosServicos.flatMap(d => d.servicos.map(s => ({servicoId:s.id, percentual:d.porcentagemDesconto})));
        this.comDescontos = this.regras.length > 0;
      }
    } catch(e) { if (sequence === this.formSequence) this.formError.set((e as Error).message); }
    finally { if (sequence === this.formSequence) this.formLoading.set(false); }
  }
  fechar() { if (!this.saving()) { ++this.formSequence; this.dialog.nativeElement.close(); } }
  cancelar(event: Event) { if (this.saving()) event.preventDefault(); else ++this.formSequence; }
  incluir(id: number, event: Event) {
    if ((event.target as HTMLInputElement).checked) { this.incluidos.push(id); this.regras = this.regras.filter(r => r.servicoId !== id); }
    else this.incluidos = this.incluidos.filter(i => i !== id);
  }
  extras(regra: Regra) { return this.servicos.filter(s => !this.incluidos.includes(s.id) && (s.id === regra.servicoId || !this.regras.some(r => r.servicoId === s.id))); }
  adicionarDesconto() { this.regras.push({servicoId:null, percentual:10}); }
  descontoValido() { return !this.comDescontos || (this.regras.length > 0 && this.regras.every(r => r.servicoId !== null && !this.incluidos.includes(r.servicoId) && Number.isInteger(r.percentual) && r.percentual >= 0 && r.percentual <= 100) && new Set(this.regras.map(r => r.servicoId)).size === this.regras.length); }
  async salvar() {
    if (!this.incluidos.length || !this.descontoValido()) { this.formError.set('Selecione os serviços incluídos e confira as regras de desconto.'); return; }
    this.saving.set(true); this.formError.set(''); this.success.set('');
    const payload = {...this.model, servicosId:this.incluidos, descontosServicos:this.comDescontos ? this.regras.map(r => ({porcentagemDesconto:r.percentual, servicosIds:[r.servicoId]})) : []};
    try {
      await this.request(this.selectedId === null ? '' : '/' + this.selectedId, {method:this.selectedId === null ? 'POST':'PUT', body:JSON.stringify(payload)});
      this.dialog.nativeElement.close(); this.success.set('Plano mensal salvo com sucesso.'); await this.load(this.resultado()?.pagina || 0);
    } catch(e) { this.formError.set((e as Error).message); }
    finally { this.saving.set(false); }
  }
  async excluir(plano: Plano) {
    if (!plano.podeExcluir || !window.confirm('Excluir o plano "' + plano.nomePlano + '"? Esta ação não pode ser desfeita.')) return;
    this.saving.set(true); this.error.set(''); this.success.set('');
    try { await this.request('/' + plano.id, {method:'DELETE'}); this.success.set('Plano excluído com sucesso.'); await this.load(this.resultado()?.pagina || 0); }
    catch(e) { this.error.set((e as Error).message); } finally { this.saving.set(false); }
  }
}
