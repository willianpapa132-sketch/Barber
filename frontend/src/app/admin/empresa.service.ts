import { Injectable, signal } from '@angular/core';
@Injectable({providedIn:'root'})
export class EmpresaService {
  nome=signal('Barbearia'); private carregado=false;
  async carregar(){
    if(this.carregado)return;this.carregado=true;
    try{const r=await fetch('/api/admin/configuracao',{headers:{Accept:'application/json'}});
      if(r.ok&&!r.redirected){const d=await r.json();if(d.nome)this.nome.set(d.nome);}
    }catch{this.carregado=false;}
  }
}
