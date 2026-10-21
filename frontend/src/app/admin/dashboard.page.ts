import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe, registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminLayout } from './layout';
import { BarberApi } from '../barbeiro/api';
registerLocaleData(pt);
interface Destaque { tipo: string; barbeiroId: number | null; nome: string | null; imagem: string | null; valor: number; atendimentos: number }
interface Dashboard {
  periodo: string; inicio: string; fim: string; agrupamento: string;
  agendamentos: number; concluidos: number; cancelados: number;
  valorBruto: number; despesas: number; valorLiquido: number; entradasAgendamentos: number; entradasPlanos: number;
  grafico: { data: string; quantidade: number }[];
  caixa: { id: number; saldoAtual: number } | null;
  destaques: Destaque[];
  planos: { ativos: number; pagosPeriodo: number; pendentes: number; vencendoHoje: number };
}
@Component({selector: 'app-admin-dashboard', imports: [AdminLayout, FormsModule, RouterLink, CurrencyPipe, DatePipe, DecimalPipe],
  templateUrl: './dashboard.page.html', styleUrl: './dashboard.page.scss'})
export class AdminDashboardPage implements OnInit, OnDestroy {
  private api = inject(BarberApi); private controller?: AbortController;
  dados = signal<Dashboard | null>(null); loading = signal(true); error = signal('');
  periodo = 'semana'; referencia = this.today(); inicio = ''; fim = '';
  periodos = [{id:'hoje',nome:'Hoje'},{id:'semana',nome:'Semana'},{id:'mes',nome:'Mês'},{id:'ano',nome:'Ano'},{id:'personalizado',nome:'Personalizado'}];
  maximo = computed(() => Math.max(1, ...this.dados()?.grafico.map(p => p.quantidade) || []));
  private today() { const d=new Date();return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; }
  ngOnInit(){void this.load();} ngOnDestroy(){this.controller?.abort();}
  percentual(valor:number,total:number){return total ? Math.round(valor*100/total) : 0;}
  horas(valor:number){return `${Math.floor(valor/60)}h ${valor%60}min`;}
  nome(tipo:string){return ({comissao:'Maior comissão',atendimentos:'Mais atendimentos',pontos:'Mais pontos do plano',horas:'Mais horas trabalhadas'} as Record<string,string>)[tipo];}
  rotulo(data:string){
    const d=new Date(data+'T12:00:00');
    if(this.dados()?.agrupamento==='mes')return d.toLocaleDateString('pt-BR',{month:'short'});
    if(this.dados()?.agrupamento==='semana'){
      const fim=Math.min(d.getDate()+6,new Date(d.getFullYear(),d.getMonth()+1,0).getDate());
      return `${String(d.getDate()).padStart(2,'0')}–${String(fim).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')}`;
    }
    return d.toLocaleDateString('pt-BR',{day:'2-digit',month:'2-digit'});
  }
  async trocar(periodo:string){this.periodo=periodo;if(periodo!=='personalizado')await this.load();}
  async load(){
    if(this.periodo==='personalizado'&&(!this.inicio||!this.fim||this.fim<this.inicio)){this.error.set('Informe um período válido de até um ano.');return;}
    this.controller?.abort();const controller=this.controller=new AbortController();this.loading.set(true);this.error.set('');
    const query=new URLSearchParams({periodo:this.periodo,referencia:this.referencia});
    if(this.periodo==='personalizado'){query.set('inicio',this.inicio);query.set('fim',this.fim);}
    try{const dados=await this.api.request<Dashboard>(`/api/admin/deshboard?${query}`,{signal:controller.signal});
      if(!controller.signal.aborted){this.dados.set(dados);this.inicio=dados.inicio;this.fim=dados.fim;}
    }catch(e){if(!controller.signal.aborted)this.error.set((e as Error).message);}
    finally{if(!controller.signal.aborted)this.loading.set(false);}
  }
}
