import { Component } from '@angular/core';
import { AdminLayout } from './layout';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-cadastros', imports: [AdminLayout, RouterLink],
  templateUrl: './cadastros.page.html', styleUrl: './cadastros.page.scss'
})
export class AdminCadastrosPage {
  readonly cadastros = [
    { icon: 'barbeiro', titulo: 'Barbeiro e Jornada da Semana', descricao: 'Cadastre os barbeiros, defina suas jornadas de trabalho na semana e configure o horário de almoço.' },
    { icon: 'servico', titulo: 'Serviços', descricao: 'Cadastre e gerencie os serviços disponíveis na barbearia, com preços, duração e informações.' },
    { icon: 'plano', titulo: 'Planos Mensais', descricao: 'Cadastre e gerencie os planos mensais da barbearia, com valores, benefícios e regras de utilização.' }
  ];
}
