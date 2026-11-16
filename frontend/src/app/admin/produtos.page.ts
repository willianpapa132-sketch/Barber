import { Component, OnInit, signal, ViewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import pt from '@angular/common/locales/pt';
import { registerLocaleData } from '@angular/common';
registerLocaleData(pt);
import { CurrencyPipe, DatePipe } from '@angular/common';
import { AdminLayout } from './layout';
interface Produto { id: number; nome: string; preco: number; periodicidadeMeses: number; quantidadeEstoque: number; percentualComissao: number; criadoEm: string }
interface Lista { itens: Produto[]; total: number; totalPaginas: number; pagina: number }
@Component({selector:'app-admin-produtos', imports:[AdminLayout, FormsModule, CurrencyPipe, DatePipe], templateUrl:'./produtos.page.html', styleUrl:'./produtos.page.scss'})
export class AdminProdutosPage implements OnInit {
  @ViewChild('dialog', {static:true}) dialog!: ElementRef<HTMLDialogElement>;
  resultado = signal<Lista | null>(null); loading = signal(true); saving = signal(false); error = signal(''); formError = signal(''); success = signal('');
  busca = ''; selected: Produto | null = null; leitura = false;
  model = {nome:'', preco:0, periodicidadeMeses:1, quantidadeEstoque:0, percentualComissao:0};
  private sequence = 0;
  ngOnInit() { void this.load(); }
  async request(path: string, options: RequestInit = {}) {
    const response = await fetch('/api/admin/produtos' + path, {...options, headers:{Accept:'application/json','Content-Type':'application/json'}});
    if (response.redirected || response.status === 401) { window.location.assign('/login'); throw new Error('Entre novamente.'); }
    const data = await response.json().catch(() => null);
    if (!response.ok) throw new Error(data?.mensagem || 'Não foi possível concluir a operação.');
    return data;
  }
  async load(pagina = 0) {
    const sequence = ++this.sequence; this.loading.set(true); this.error.set('');
    try {
      const data: Lista = await this.request('?' + new URLSearchParams({busca:this.busca.trim(), pagina:String(pagina)}));
      if (sequence !== this.sequence) return;
      if (pagina > 0 && !data.itens.length) { void this.load(Math.max(0, data.totalPaginas - 1)); return; }
      this.resultado.set(data);
    } catch(e) { if (sequence === this.sequence) this.error.set((e as Error).message); }
    finally { if (sequence === this.sequence) this.loading.set(false); }
  }
  abrir(produto: Produto | null = null, leitura = false) {
    this.selected = produto; this.leitura = leitura; this.formError.set('');
    this.model = produto ? {nome:produto.nome, preco:produto.preco, periodicidadeMeses:produto.periodicidadeMeses, quantidadeEstoque:produto.quantidadeEstoque,percentualComissao:produto.percentualComissao} : {nome:'',preco:0,periodicidadeMeses:1,quantidadeEstoque:0, percentualComissao:0};
    this.dialog.nativeElement.showModal();
  }
  fechar() { if (!this.saving()) this.dialog.nativeElement.close(); }
  async salvar() {
    this.saving.set(true); this.formError.set(''); this.success.set('');
    try { await this.request(this.selected ? '/' + this.selected.id : '', {method:this.selected ? 'PUT':'POST', body:JSON.stringify(this.model)});
      this.dialog.nativeElement.close(); this.success.set('Produto salvo com sucesso.'); await this.load(this.resultado()?.pagina || 0);
    } catch(e) { this.formError.set((e as Error).message); } finally { this.saving.set(false); }
  }
  async excluir(s: Produto) {
    if (!window.confirm('Excluir o produto "' + s.nome + '"? Esta ação não pode ser desfeita.')) return;
    this.saving.set(true); this.success.set('');
    try { await this.request('/' + s.id, {method:'DELETE'}); this.success.set('Produto excluído com sucesso.'); await this.load(this.resultado()?.pagina || 0); }
    catch(e) { this.error.set((e as Error).message); } finally { this.saving.set(false); }
  }
}
