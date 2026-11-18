import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminLayout } from './layout';
import { BarberApi, dateLabel, hhmm } from '../barbeiro/api';
interface Resumo { agendamentoId:number; cliente:string; barbeiro:string; data:string; horaInicio:string; fechada:boolean; fechadoPor:string|null; fechadoEm:string|null }
@Component({selector:'app-admin-comandas',imports:[AdminLayout,FormsModule,RouterLink,DatePipe],template:`
<app-admin-layout>
<h1>Comandas</h1><p>Selecione um atendimento para conferir os itens e fechar a comanda.</p>
<form (ngSubmit)="load()"><label>De: <input type="date" name="de" [(ngModel)]="de" required></label> <label>Até: <input type="date" name="ate" [(ngModel)]="ate" required></label> <button type="submit">Filtrar</button></form>
<nav aria-label="Situação das comandas"><button (click)="selecionar(false)" [attr.aria-pressed]="!encerradas">Abertas</button><button (click)="selecionar(true)" [attr.aria-pressed]="encerradas">Encerradas</button></nav>
@if(error()){<p role="alert">{{error()}}</p>}
@if(loading()){<p role="status">Carregando comandas...</p>}@else{
<div class="table-wrap"><table><thead><tr><th>Cliente</th><th>Barbeiro</th><th>Agendamento</th>@if(encerradas){<th>Fechado por</th><th>Fechado em</th>}<th>Ação</th></tr></thead><tbody>
@for(c of comandas();track c.agendamentoId){<tr><td>{{c.cliente}}</td><td>{{c.barbeiro}}</td><td>{{dateLabel(c.data)}} às {{hhmm(c.horaInicio)}}</td>@if(encerradas){<td>{{c.fechadoPor || 'Não informado'}}</td><td>{{c.fechadoEm | date:'dd/MM/yyyy HH:mm'}}</td>}<td><a [routerLink]="['/admin/comandas',c.agendamentoId]">{{encerradas?'Ver comanda':'Fechar comanda'}}</a></td></tr>}
@empty{<tr><td [attr.colspan]="encerradas?6:4">Nenhuma comanda {{encerradas?'encerrada':'aberta'}} no período.</td></tr>}
</tbody></table></div>}
</app-admin-layout>`,styles:[`form,nav{display:flex;gap:16px;flex-wrap:wrap;margin:24px 0}input,button{padding:10px;border:1px solid #ddd;border-radius:6px}button,a{cursor:pointer}button[aria-pressed=true]{background:#d5af55;color:#172018}table{width:100%;border-collapse:collapse;background:white}th,td{text-align:left;padding:16px;border-bottom:1px solid #ddd}.table-wrap{overflow:auto}`]})
export class AdminComandasPage implements OnInit,OnDestroy {
 private api=inject(BarberApi);private controller?:AbortController;
 private today(){const d=new Date();return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;}
 de=this.today();ate=this.today();encerradas=false;comandas=signal<Resumo[]>([]);loading=signal(false);error=signal('');dateLabel=dateLabel;hhmm=hhmm;
 ngOnInit(){void this.load();}ngOnDestroy(){this.controller?.abort();}
 selecionar(valor:boolean){this.encerradas=valor;void this.load();}
 async load(){this.controller?.abort();const c=this.controller=new AbortController();this.error.set('');this.comandas.set([]);this.loading.set(false);if(!this.de||!this.ate||this.de>this.ate){this.error.set('Informe um período válido.');return;}this.loading.set(true);try{const q=new URLSearchParams({de:this.de,ate:this.ate,encerradas:String(this.encerradas)});const rows=await this.api.request<Resumo[]>(`/api/admin/comandas?${q}`,{signal:c.signal});if(!c.signal.aborted)this.comandas.set(rows);}catch(e){if(!c.signal.aborted)this.error.set((e as Error).message);}finally{if(!c.signal.aborted)this.loading.set(false);}}
}
