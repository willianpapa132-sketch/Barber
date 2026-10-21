import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminLayout } from './layout';
import { EmpresaService } from './empresa.service';
import { BarberApi } from '../barbeiro/api';
interface Horario {
  diaSemana:string; horarioInicio:string|null; horarioFim:string|null; fechado:boolean;
  intervaloMinimoEntreAgendamentos:number; permiteAgendamentoPlano:boolean;
}
interface Configuracao {
  nome:string; telefone:string|null; endereco:string|null; diasMaximoAntecedencia:number;
  horarios:Horario[]; jornadasAjustadas:number; agendamentosCancelados:number;
}
@Component({selector:'app-admin-configuracao',imports:[AdminLayout,FormsModule],
  templateUrl:'./configuracao.page.html',styleUrl:'./configuracao.page.scss'})
export class AdminConfiguracaoPage implements OnInit,OnDestroy {
  private api=inject(BarberApi); private empresa=inject(EmpresaService); private controller=new AbortController();
  dados:Configuracao|null=null;loading=signal(true);saving=signal(false);error=signal('');success=signal('');
  dias:Record<string,string>={MONDAY:'Segunda-feira',TUESDAY:'Terça-feira',WEDNESDAY:'Quarta-feira',THURSDAY:'Quinta-feira',FRIDAY:'Sexta-feira',SATURDAY:'Sábado',SUNDAY:'Domingo'};
  ngOnInit(){void this.load();}ngOnDestroy(){this.controller.abort();}
  private preencher(d:Configuracao){this.dados={...d,horarios:d.horarios.map(h=>({...h,horarioInicio:h.horarioInicio?.slice(0,5)||'09:00',horarioFim:h.horarioFim?.slice(0,5)||'18:00'}))};}
  async load(){
    this.loading.set(true);this.error.set('');
    try{this.preencher(await this.api.request<Configuracao>('/api/admin/configuracao',{signal:this.controller.signal}));}
    catch(e){if(!this.controller.signal.aborted)this.error.set((e as Error).message);}
    finally{this.loading.set(false);}
  }
  async salvar(){
    if(this.saving()||!this.dados)return;
    if(this.dados.horarios.some(h=>!h.fechado&&(!h.horarioInicio||!h.horarioFim||h.horarioInicio>=h.horarioFim))){this.error.set('A abertura deve ser anterior ao fechamento em todos os dias abertos.');return;}
    this.saving.set(true);this.error.set('');this.success.set('');
    try{
      const d=await this.api.request<Configuracao>('/api/admin/configuracao',{method:'PUT',body:JSON.stringify(this.dados),signal:this.controller.signal});
      this.preencher(d);this.empresa.nome.set(d.nome);
      this.success.set(`Configurações salvas. ${d.jornadasAjustadas} jornada(s) ajustada(s) e ${d.agendamentosCancelados} agendamento(s) cancelado(s).`);
    }catch(e){if(!this.controller.signal.aborted)this.error.set((e as Error).message);}
    finally{this.saving.set(false);}
  }
}
