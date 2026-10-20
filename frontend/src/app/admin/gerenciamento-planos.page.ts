import { Component, ElementRef, OnInit, ViewChild, computed, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { FormsModule } from '@angular/forms';
import { AdminLayout } from './layout';
registerLocaleData(pt);
interface Contrato {
  id: number; cliente: string; telefone: string; planoId: number; plano: string; valorMensal: number;
  contratadoEm: string; proximoVencimento: string; prazoPagamento: string; diasParaVencer: number;
  status: string; ativo: boolean; pagamentoAtrasado: boolean; liberadoAte: string | null;
}
interface Recebimento { id: number; vencimento: string; dataPagamento: string; valor: number; formaPagamento: string; registradoPor: string; referencia: string }
interface Dados { diasTolerancia: number; hoje: string; contratos: Contrato[] }
@Component({
  selector: 'app-gerenciamento-planos', imports: [AdminLayout, FormsModule, CurrencyPipe, DatePipe],
  templateUrl: './gerenciamento-planos.page.html', styleUrl: './gerenciamento-planos.page.scss'
})
export class GerenciamentoPlanosPage implements OnInit {
  @ViewChild('dialog', { static: true }) dialog!: ElementRef<HTMLDialogElement>;
  dados = signal<Dados | null>(null); loading = signal(true); saving = signal(false);
  erro = signal(''); sucesso = signal(''); erroDialog = signal(''); historicoErro = signal(''); historicoLoading = signal(false);
  busca = signal(''); status = signal(''); plano = signal(''); pagina = signal(0);
  selecionado: Contrato | null = null; acao: 'receber' | 'ver' | 'cancelar' | 'desbloquear' = 'ver';
  historico = signal<Recebimento[]>([]); tolerancia: number | null = 0;
  dataPagamento = ''; formaPagamento = 'PIX'; referencia = '';
  readonly rotulos: Record<string, string> = { EM_DIA: 'Em dia', VENCE_HOJE: 'Vence hoje', EM_TOLERANCIA: 'Em tolerância', BLOQUEADO: 'Bloqueado', LIBERADO: 'Liberado pelo admin', CANCELADO: 'Cancelado' };
  planos = computed(() => [...new Map((this.dados()?.contratos ?? []).map(c => [c.planoId, c.plano])).entries()]);
  filtrados = computed(() => {
    const termo = this.busca().trim().toLocaleLowerCase('pt-BR');
    return (this.dados()?.contratos ?? []).filter(c => (!this.status() || c.status === this.status()) &&
      (!this.plano() || c.planoId.toString() === this.plano()) && [c.cliente, c.telefone, c.plano].some(v => v?.toLocaleLowerCase('pt-BR').includes(termo)));
  });
  visiveis = computed(() => this.filtrados().slice(this.pagina() * 10, this.pagina() * 10 + 10));
  paginas = computed(() => Math.max(1, Math.ceil(this.filtrados().length / 10)));
  ativos = computed(() => this.dados()?.contratos.filter(c => c.ativo).length ?? 0);
  bloqueados = computed(() => this.dados()?.contratos.filter(c => c.pagamentoAtrasado).length ?? 0);
  ngOnInit() { void this.carregar(); }
  async request(path = '', options: RequestInit = {}) {
    const r = await fetch('/api/admin/gerenciamento-planos' + path, { ...options, headers: { Accept: 'application/json', 'Content-Type': 'application/json' } });
    if (r.redirected || r.status === 401) { window.location.assign('/login'); throw new Error('Entre novamente para continuar.'); }
    const data = await r.text(); let body: any = null;
    try { body = data ? JSON.parse(data) : null; } catch { if (r.ok) throw new Error('Resposta inválida do servidor.'); }
    if (!r.ok) throw new Error(body?.mensagem || body?.detail || 'Não foi possível concluir a operação.');
    return body;
  }
  async carregar() {
    this.loading.set(true); this.erro.set('');
    try { const dados: Dados = await this.request(); this.dados.set(dados); this.tolerancia = dados.diasTolerancia; this.pagina.set(0); }
    catch (e) { this.erro.set((e as Error).message); }
    finally { this.loading.set(false); }
  }
  filtrar() { this.pagina.set(0); }
  prazo(c: Contrato) { return c.diasParaVencer === 0 ? 'Hoje' : c.diasParaVencer > 0 ? `Em ${c.diasParaVencer} dia(s)` : `Há ${-c.diasParaVencer} dia(s)`; }
  async salvarTolerancia() {
    if (this.saving() || this.tolerancia === null || !Number.isInteger(this.tolerancia) || this.tolerancia < 0 || this.tolerancia > 30) return;
    this.saving.set(true); this.erro.set(''); this.sucesso.set('');
    try { await this.request('/tolerancia', { method: 'PUT', body: JSON.stringify({ dias: this.tolerancia }) }); await this.carregar(); this.sucesso.set('Tolerância atualizada.'); }
    catch (e) { this.erro.set((e as Error).message); } finally { this.saving.set(false); }
  }
  abrir(c: Contrato, acao: typeof this.acao) {
    this.selecionado = c; this.acao = acao; this.erroDialog.set(''); this.historico.set([]); this.historicoErro.set('');
    this.dataPagamento = this.dados()!.hoje; this.formaPagamento = 'PIX'; this.referencia = '';
    this.dialog.nativeElement.showModal();
    if (acao === 'ver') void this.carregarHistorico(c.id);
  }
  async carregarHistorico(id: number) {
    this.historicoLoading.set(true);
    try { const itens = await this.request(`/${id}/recebimentos`); if (this.selecionado?.id === id) this.historico.set(itens); }
    catch (e) { this.historicoErro.set((e as Error).message); }
    finally { this.historicoLoading.set(false); }
  }
  fechar() { if (!this.saving()) this.dialog.nativeElement.close(); }
  async confirmar() {
    if (!this.selecionado || this.saving()) return;
    this.saving.set(true); this.erroDialog.set(''); this.sucesso.set('');
    try {
      const path = this.acao === 'receber' ? 'recebimentos' : this.acao === 'cancelar' ? 'cancelamento' : 'desbloqueio';
      await this.request(`/${this.selecionado.id}/${path}`, { method: 'POST', ...(this.acao === 'receber' ? { body: JSON.stringify({ vencimento: this.selecionado.proximoVencimento, dataPagamento: this.dataPagamento, formaPagamento: this.formaPagamento, referencia: this.referencia.trim() }) } : {}) });
      this.dialog.nativeElement.close(); await this.carregar();
      this.sucesso.set(this.acao === 'receber' ? 'Recebimento registrado. Confira a próxima cobrança.' : this.acao === 'cancelar' ? 'Plano cancelado.' : 'Agendamento liberado até a próxima cobrança. A dívida continua pendente.');
    } catch (e) { this.erroDialog.set((e as Error).message); } finally { this.saving.set(false); }
  }
}
