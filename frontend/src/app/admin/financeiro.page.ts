import { Component, OnInit, computed, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { FormsModule } from '@angular/forms';
import { AdminLayout } from './layout';

registerLocaleData(pt);
interface Lancamento {
  id: number; dataHora: string; tipo: string; descricao: string; cliente: string;
  barbeiro: string | null; formaPagamento: string | null; valor: number;
}
interface Financeiro {
  inicio: string; fim: string; valorBruto: number; despesas: number; valorLiquido: number;
  entradas: Lancamento[]; saidas: Lancamento[];
}
@Component({
  selector: 'app-admin-financeiro', imports: [AdminLayout, FormsModule, CurrencyPipe, DatePipe],
  templateUrl: './financeiro.page.html', styleUrl: './financeiro.page.scss'
})
export class AdminFinanceiroPage implements OnInit {
  dados = signal<Financeiro | null>(null);
  loading = signal(true); error = signal(''); aba = signal<'entradas' | 'saidas'>('entradas');
  busca = signal(''); pagina = signal(0); readonly tamanho = 10;
  inicio = ''; fim = '';
  itens = computed(() => {
    const termo = this.busca().trim().toLocaleLowerCase('pt-BR');
    return (this.dados()?.[this.aba()] ?? []).filter(i =>
      [i.descricao, i.cliente, i.barbeiro, i.tipo, this.pagamento(i.formaPagamento)]
        .some(v => v?.toLocaleLowerCase('pt-BR').includes(termo)));
  });
  visiveis = computed(() => this.itens().slice(this.pagina() * this.tamanho, (this.pagina() + 1) * this.tamanho));
  paginas = computed(() => Math.max(1, Math.ceil(this.itens().length / this.tamanho)));
  ngOnInit() { void this.carregar(true); }
  async carregar(inicial = false) {
    if (!inicial && (!this.inicio || !this.fim || this.fim < this.inicio)) {
      this.error.set('Informe um período com início e fim válidos.'); return;
    }
    this.loading.set(true); this.error.set(''); this.dados.set(null);
    try {
      const query = inicial ? '' : '?' + new URLSearchParams({ inicio: this.inicio, fim: this.fim });
      const response = await fetch('/api/admin/financeiro' + query, { headers: { Accept: 'application/json' } });
      if (response.redirected || response.status === 401) { window.location.assign('/login'); return; }
      const data = await response.json().catch(() => null);
      if (!response.ok) throw new Error(data?.detail || data?.message || 'Não foi possível consultar o financeiro. Verifique o período (máximo de 3 meses).');
      if (!data || !Array.isArray(data.entradas) || !Array.isArray(data.saidas)) throw new Error('Resposta inválida ao consultar o financeiro.');
      this.dados.set(data); this.inicio = data.inicio; this.fim = data.fim; this.pagina.set(0);
    } catch (e) { this.error.set((e as Error).message); }
    finally { this.loading.set(false); }
  }
  trocar(aba: 'entradas' | 'saidas') { this.aba.set(aba); this.pagina.set(0); }
  pesquisar(valor: string) { this.busca.set(valor); this.pagina.set(0); }
  pagamento(valor: string | null) {
    return ({ PIX: 'PIX', DINHEIRO: 'Dinheiro', DEBITO: 'Cartão débito', CREDITO: 'Cartão crédito', MENSAL: 'Plano mensal' } as Record<string, string>)[valor ?? ''] ?? '—';
  }
  exportar() {
    const linhas = [['Data/Hora', 'Tipo', 'Descrição', 'Cliente', 'Barbeiro', 'Forma de pagamento', 'Valor'],
      ...this.itens().map(i => [i.dataHora.replace('T', ' '), i.tipo, i.descricao, i.cliente, i.barbeiro ?? '', this.pagamento(i.formaPagamento), i.valor.toFixed(2).replace('.', ',')])];
    const csv = '\uFEFF' + linhas.map(l => l.map(v => '"' + String(v).replace(/^[=+@\-\t\r]/, "'$&").replaceAll('"', '""') + '"').join(';')).join('\r\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }));
    const link = document.createElement('a'); link.href = url;
    link.download = `financeiro-${this.aba()}-${this.dados()?.inicio}-${this.dados()?.fim}.csv`; link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
}
