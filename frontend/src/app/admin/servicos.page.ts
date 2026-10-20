import { Component, OnInit, signal, ViewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import pt from '@angular/common/locales/pt';
import { registerLocaleData } from '@angular/common';
registerLocaleData(pt);
import { CurrencyPipe, DatePipe } from '@angular/common';
import { AdminLayout } from './layout';
interface Servico { id: number; nome: string; preco: number; duracaoMinutos: number; ativo: boolean; criadoEm: string; podeExcluir: boolean }
interface Lista { itens: Servico[]; total: number; totalPaginas: number; pagina: number }
@Component({selector:'app-admin-servicos', imports:[AdminLayout, FormsModule, CurrencyPipe, DatePipe], templateUrl:'./servicos.page.html', styleUrl:'./servicos.page.scss'})
export class AdminServicosPage implements OnInit {
  @ViewChild('dialog', {static:true}) dialog!: ElementRef<HTMLDialogElement>;
  resultado = signal<Lista | null>(null); loading = signal(true); saving = signal(false); error = signal(''); formError = signal(''); success = signal('');
  busca = ''; selected: Servico | null = null; leitura = false;
  model = {nome:'', valor:0, duracaoMinutos:30, ativo:true};
  private sequence = 0;
  ngOnInit() { void this.load(); }
  async request(path: string, options: RequestInit = {}) {
    const response = await fetch('/api/admin/servicos' + path, {...options, headers:{Accept:'application/json','Content-Type':'application/json'}});
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
  abrir(servico: Servico | null = null, leitura = false) {
    this.selected = servico; this.leitura = leitura; this.formError.set('');
    this.model = servico ? {nome:servico.nome, valor:servico.preco, duracaoMinutos:servico.duracaoMinutos, ativo:servico.ativo} : {nome:'',valor:0,duracaoMinutos:30,ativo:true};
    this.dialog.nativeElement.showModal();
  }
  fechar() { if (!this.saving()) this.dialog.nativeElement.close(); }
  async salvar() {
    this.saving.set(true); this.formError.set(''); this.success.set('');
    try { await this.request(this.selected ? '/' + this.selected.id : '', {method:this.selected ? 'PUT':'POST', body:JSON.stringify(this.model)});
      this.dialog.nativeElement.close(); this.success.set('Serviço salvo com sucesso.'); await this.load(this.resultado()?.pagina || 0);
    } catch(e) { this.formError.set((e as Error).message); } finally { this.saving.set(false); }
  }
  async excluir(s: Servico) {
    if (!s.podeExcluir || !window.confirm('Excluir o serviço "' + s.nome + '"? Esta ação não pode ser desfeita.')) return;
    this.saving.set(true); this.success.set('');
    try { await this.request('/' + s.id, {method:'DELETE'}); this.success.set('Serviço excluído com sucesso.'); await this.load(this.resultado()?.pagina || 0); }
    catch(e) { this.error.set((e as Error).message); } finally { this.saving.set(false); }
  }
}
