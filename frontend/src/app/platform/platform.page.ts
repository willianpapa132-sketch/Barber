import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
interface Company {id:string;nome:string;responsavel:string;slug:string;host:string;estado:'ATIVA'|'SUSPENSA'|'CRIANDO'|'FALHOU'}
interface Horario {diaSemana:string;horarioInicio:string;horarioFim:string;fechado:boolean;intervaloMinimoEntreAgendamentos:number;permiteAgendamentoPlano:boolean}
@Component({selector:'app-platform',imports:[FormsModule],templateUrl:'./platform.page.html',styleUrl:'./platform.page.scss'})
export class PlatformPage implements OnInit {
  empresas=signal<Company[]>([]);loading=signal(true);saving=signal(false);error=signal('');success=signal('');
  nome='';responsavel='';slug='';adminLogin='';adminPassword='';private csrf:{token:string;headerName:string}|null=null;
  telefone='';enderecoEmpresa='';
  dias:Record<string,string>={MONDAY:'Segunda',TUESDAY:'Terça',WEDNESDAY:'Quarta',THURSDAY:'Quinta',FRIDAY:'Sexta',SATURDAY:'Sábado',SUNDAY:'Domingo'};
  horarios:Horario[]=this.horariosIniciais();
  private horariosIniciais():Horario[]{return Object.keys(this.dias).map(diaSemana=>({diaSemana,horarioInicio:'09:00',horarioFim:diaSemana==='SATURDAY'?'14:00':'18:00',fechado:diaSemana==='SUNDAY',intervaloMinimoEntreAgendamentos:15,permiteAgendamentoPlano:diaSemana!=='SUNDAY'}));}
  ngOnInit(){void this.load();}
  async request<T>(path:string,options:RequestInit={}):Promise<T>{
    const response=await fetch('/api/plataforma'+path,{...options,headers:{Accept:'application/json',...(options.body?{'Content-Type':'application/json'}:{}),...(this.csrf?{[this.csrf.headerName]:this.csrf.token}:{})}});
    if(response.redirected||response.status===401){window.location.assign('/login');throw new Error('Entre novamente para continuar.');}
    const data=await response.json().catch(()=>null);
    if(!response.ok)throw new Error(data?.mensagem||'Não foi possível concluir a operação. Verifique seu acesso.');
    return data as T;
  }
  async load(){this.loading.set(true);this.error.set('');try{this.csrf=await this.request('/csrf');this.empresas.set(await this.request('/empresas'));}catch(e){this.error.set((e as Error).message);}finally{this.loading.set(false);}}
  endereco(c:Company){return `${window.location.protocol}//${c.host}${window.location.port?':'+window.location.port:''}/login`;}
  async criar(){
    if(this.saving())return;
    if(this.horarios.some(h=>!h.fechado&&(!h.horarioInicio||!h.horarioFim||h.horarioInicio>=h.horarioFim))){this.error.set('Confira a abertura e o fechamento de cada dia aberto.');return;}
    this.saving.set(true);this.error.set('');this.success.set('');
    try{const c=await this.request<Company>('/empresas',{method:'POST',body:JSON.stringify({nome:this.nome,responsavel:this.responsavel,slug:this.slug,adminLogin:this.adminLogin,adminPassword:this.adminPassword,telefone:this.telefone,endereco:this.enderecoEmpresa,horarios:this.horarios})});
      this.empresas.update(list=>[c,...list]);this.success.set(`Empresa ${c.nome} criada. O responsável já pode entrar pelo endereço abaixo.`);
      this.nome='';this.responsavel='';this.slug='';this.adminLogin='';this.adminPassword='';
      this.telefone='';this.enderecoEmpresa='';this.horarios=this.horariosIniciais();
    }catch(e){this.error.set((e as Error).message);await this.refreshList();}finally{this.saving.set(false);}
  }
  private async refreshList(){try{this.empresas.set(await this.request('/empresas'));}catch{/* Mantem o erro original do cadastro. */}}
  async alterar(c:Company){
    if(this.saving())return;this.saving.set(true);this.error.set('');this.success.set('');
    try{const updated=await this.request<Company>(`/empresas/${c.id}/status`,{method:'PUT',body:JSON.stringify({ativa:c.estado==='SUSPENSA'})});
      this.empresas.update(list=>list.map(item=>item.id===c.id?updated:item));this.success.set(`Empresa ${updated.estado==='ATIVA'?'reativada':'suspensa'}.`);
    }catch(e){this.error.set((e as Error).message);}finally{this.saving.set(false);}
  }
}
