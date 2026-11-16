import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { EmpresaService } from './empresa.service';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AdminLayout } from './layout';
import { BarberApi, money, dateLabel, hhmm } from '../barbeiro/api';
interface Item { tipo: string; servicoId: number | null; produtoId: number | null; nome: string; quantidade: number; precoUnitario: number; subtotal: number; desconto: number; total: number; percentualComissao: number; comissao: number; incluidoNoPlano: boolean; pontos: number }
interface Comanda { id: number | null; agendamentoId: number; cliente: string; telefone: string; barbeiro: string; data: string; horaInicio: string; fechada: boolean; legado: boolean; fechadoEm: string; fechadoPor: string; observacao: string; tipoDesconto: string; valorDescontoInformado: number; subtotal: number; desconto: number; total: number; totalComissao: number; pontos: number; itens: Item[]; pagamentos: Pagamento[] }
interface Opcao { id: number; nome: string; preco: number; estoque: number | null }
interface Pagamento { forma: string; valor: number }
interface Escolha { tipo: 'SERVICO' | 'PRODUTO'; id: number; quantidade: number }
@Component({selector:'app-admin-comanda',imports:[AdminLayout,FormsModule,RouterLink,DatePipe],templateUrl:'./comanda.page.html',styleUrl:'./comanda.page.scss'})
export class AdminComandaPage implements OnInit,OnDestroy {
  empresa=inject(EmpresaService);
  private api=inject(BarberApi); private route=inject(ActivatedRoute);
  private id=Number(this.route.snapshot.paramMap.get('id'));
  private request?: AbortController; private lifetime=new AbortController();
  comanda=signal<Comanda|null>(null); catalogo=signal<{servicos:Opcao[];produtos:Opcao[]}>({servicos:[],produtos:[]});
  loading=signal(true); calculating=signal(false); saving=signal(false); error=signal(''); dirty=signal(false);
  itens:Escolha[]=[]; pagamentos:Pagamento[]=[]; tipoDesconto='NENHUM'; desconto=0; observacao=''; buscaServico=''; buscaProduto='';
  money=money; dateLabel=dateLabel; hhmm=hhmm;
  formas=[{id:'DINHEIRO',nome:'Dinheiro'},{id:'PIX',nome:'Pix'},{id:'DEBITO',nome:'Cartão de débito'},{id:'CREDITO',nome:'Cartão de crédito'}];
  ngOnInit(){void this.load();} ngOnDestroy(){this.request?.abort();this.lifetime.abort();}
  async load(){
    this.loading.set(true);this.error.set('');
    try {
      const [c,cat]=await Promise.all([this.api.request<Comanda>(`/api/admin/comandas/agendamento/${this.id}`,{signal:this.lifetime.signal}),this.api.request<{servicos:Opcao[];produtos:Opcao[]}>('/api/admin/comandas/catalogo',{signal:this.lifetime.signal})]);
      this.comanda.set(c);this.catalogo.set(cat);this.tipoDesconto=c.tipoDesconto;this.desconto=c.valorDescontoInformado;this.observacao=c.observacao||'';
      this.itens=c.itens.filter(i=>i.tipo!=='LEGADO').map(i=>({tipo:i.tipo as 'SERVICO'|'PRODUTO',id:(i.servicoId??i.produtoId)!,quantidade:i.quantidade}));
      this.pagamentos=c.fechada?c.pagamentos.map(p=>({...p})):Number(c.total)>0?[{forma:'PIX',valor:Number(c.total)}]:[];
      this.dirty.set(false);
    }catch(e){if(!this.lifetime.signal.aborted)this.error.set((e as Error).message);}
    finally{this.loading.set(false);}
  }
  get displayedItems():Item[]{
    const c=this.comanda();if(!c)return [];if(c.fechada)return c.itens;
    return this.itens.map(e=>{
      const atual=c.itens.find(i=>i.tipo===e.tipo&&(i.servicoId??i.produtoId)===e.id);
      if(atual)return {...atual,quantidade:e.quantidade};
      const opcao=(e.tipo==='SERVICO'?this.catalogo().servicos:this.catalogo().produtos).find(i=>i.id===e.id);
      return {tipo:e.tipo,servicoId:e.tipo==='SERVICO'?e.id:null,produtoId:e.tipo==='PRODUTO'?e.id:null,nome:opcao?.nome||'Item',quantidade:e.quantidade,precoUnitario:opcao?.preco||0,subtotal:0,desconto:0,total:0,percentualComissao:0,comissao:0,incluidoNoPlano:false,pontos:0};
    });
  }
  subtotalTipo(tipo:string){return this.comanda()?.itens.filter(i=>i.tipo===tipo).reduce((n,i)=>n+Number(i.subtotal),0)||0;}
  get servicos(){return this.catalogo().servicos.filter(s=>s.nome.toLocaleLowerCase().includes(this.buscaServico.toLocaleLowerCase()));}
  get produtos(){return this.catalogo().produtos.filter(p=>p.nome.toLocaleLowerCase().includes(this.buscaProduto.toLocaleLowerCase()));}
  selecionado(tipo:string,id:number){return this.itens.some(i=>i.tipo===tipo&&i.id===id);}
  adicionar(tipo:'SERVICO'|'PRODUTO',id:number){if(this.selecionado(tipo,id))return;this.itens.push({tipo,id,quantidade:1});void this.previa();}
  remover(index:number){this.itens.splice(index,1);void this.previa();}
  quantidade(index:number,valor:number){this.itens[index].quantidade=valor;void this.previa();}
  payload(){return {itens:this.itens.map(i=>({...i})),tipoDesconto:this.tipoDesconto,desconto:this.tipoDesconto==='NENHUM'?0:this.desconto,observacao:this.observacao,pagamentos:this.pagamentos.map(p=>({...p}))};}
  async previa(){
    this.request?.abort();const controller=this.request=new AbortController();this.dirty.set(true);this.calculating.set(true);this.error.set('');
    try{const c=await this.api.request<Comanda>(`/api/admin/comandas/agendamento/${this.id}/previa`,{method:'POST',signal:controller.signal,body:JSON.stringify({...this.payload(),pagamentos:[]})});if(!controller.signal.aborted){this.comanda.set(c);this.dirty.set(false);}}
    catch(e){if(!controller.signal.aborted)this.error.set((e as Error).message);}
    finally{if(!controller.signal.aborted)this.calculating.set(false);}
  }
  get pago(){return this.pagamentos.reduce((n,p)=>n+Math.round(Number(p.valor||0)*100),0);}
  get diferenca(){return Math.round(Number(this.comanda()?.total||0)*100)-this.pago;}
  get podeFechar(){return !this.loading()&&!this.saving()&&!this.calculating()&&!this.dirty()&&!this.comanda()?.fechada&&this.itens.length>0&&this.diferenca===0&&this.pagamentos.every(p=>Number(p.valor)>0&&this.formas.some(f=>f.id===p.forma));}
  adicionarPagamento(){if(this.pagamentos.length<10)this.pagamentos.push({forma:'PIX',valor:Math.max(0,this.diferenca)/100});}
  async fechar(imprimir=false){
    if(!this.podeFechar)return;this.saving.set(true);this.error.set('');
    try{const c=await this.api.request<Comanda>(`/api/admin/comandas/agendamento/${this.id}/fechar`,{method:'POST',body:JSON.stringify(this.payload())});this.comanda.set(c);this.pagamentos=c.pagamentos;if(imprimir)requestAnimationFrame(()=>window.print());}
    catch(e){this.error.set((e as Error).message);}
    finally{this.saving.set(false);}
  }
  imprimir(){window.print();}
}
