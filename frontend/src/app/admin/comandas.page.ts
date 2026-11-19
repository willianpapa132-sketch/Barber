import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminLayout } from './layout';
import { BarberApi, dateLabel, hhmm } from '../barbeiro/api';
interface Resumo { agendamentoId:number; cliente:string; barbeiro:string; data:string; horaInicio:string; fechada:boolean; fechadoPor:string|null; fechadoEm:string|null }
@Component({selector:'app-admin-comandas',imports:[AdminLayout,FormsModule,RouterLink,DatePipe],template:`
<app-admin-layout>
<section class="hero">
  <div><h1>Comandas</h1><p>Selecione um atendimento para conferir os itens e fechar a comanda.</p></div>
  <form class="periodo" (ngSubmit)="load()">
    <label>Data inicial<input type="date" name="de" [(ngModel)]="de" required [disabled]="loading()"></label>
    <label>Data final<input type="date" name="ate" [(ngModel)]="ate" required [min]="de" [disabled]="loading()"></label>
    <button class="gold" type="submit" [disabled]="loading()">Filtrar</button>
  </form>
</section>
<nav class="tabs" aria-label="Situação das comandas"><button type="button" (click)="selecionar(false)" [class.active]="!encerradas" [attr.aria-pressed]="!encerradas">Abertas</button><button type="button" (click)="selecionar(true)" [class.active]="encerradas" [attr.aria-pressed]="encerradas">Encerradas</button></nav>
@if(error()){<p class="error" role="alert">{{error()}}</p>}
@if(loading()){<p class="loading" role="status">Carregando comandas...</p>}@else{
<section class="panel" [attr.aria-label]="encerradas?'Comandas encerradas':'Comandas abertas'">
<div class="table-wrap"><table><thead><tr><th scope="col">Cliente</th><th scope="col">Barbeiro</th><th scope="col">Agendamento</th>@if(encerradas){<th scope="col">Fechado por</th><th scope="col">Fechado em</th>}<th scope="col">Ação</th></tr></thead><tbody>
@for(c of comandas();track c.agendamentoId){<tr><td>{{c.cliente}}</td><td>{{c.barbeiro}}</td><td>{{dateLabel(c.data)}} às {{hhmm(c.horaInicio)}}</td>@if(encerradas){<td>{{c.fechadoPor || 'Não informado'}}</td><td>{{c.fechadoEm | date:'dd/MM/yyyy HH:mm'}}</td>}<td><a [routerLink]="['/admin/comandas',c.agendamentoId]">{{encerradas?'Ver comanda':'Fechar comanda'}}</a></td></tr>}
@empty{<tr><td class="empty" [attr.colspan]="encerradas?6:4">Nenhuma comanda {{encerradas?'encerrada':'aberta'}} no período.</td></tr>}
</tbody></table></div></section>}
</app-admin-layout>`,styleUrl:'./comandas.page.scss'})
export class AdminComandasPage implements OnInit,OnDestroy {
 private api=inject(BarberApi);private controller?:AbortController;
 private today(){const d=new Date();return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;}
 de=this.today();ate=this.today();encerradas=false;comandas=signal<Resumo[]>([]);loading=signal(false);error=signal('');dateLabel=dateLabel;hhmm=hhmm;
 ngOnInit(){void this.load();}ngOnDestroy(){this.controller?.abort();}
 selecionar(valor:boolean){this.encerradas=valor;void this.load();}
 async load(){this.controller?.abort();const c=this.controller=new AbortController();this.error.set('');this.comandas.set([]);this.loading.set(false);if(!this.de||!this.ate||this.de>this.ate){this.error.set('Informe um período válido.');return;}this.loading.set(true);try{const q=new URLSearchParams({de:this.de,ate:this.ate,encerradas:String(this.encerradas)});const rows=await this.api.request<Resumo[]>(`/api/admin/comandas?${q}`,{signal:c.signal});if(!c.signal.aborted)this.comandas.set(rows);}catch(e){if(!c.signal.aborted)this.error.set((e as Error).message);}finally{if(!c.signal.aborted)this.loading.set(false);}}
}
