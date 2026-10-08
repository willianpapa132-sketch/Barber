import { Routes } from '@angular/router';
import { AgendamentosPage } from './pages/agendamentos.page';
import { HomePage } from './pages/home.page';
import { NovoAgendamentoPage } from './pages/novo-agendamento.page';
import { PerfilPage } from './pages/perfil.page';
import { PlanoPage } from './pages/plano.page';

export const routes: Routes = [
  { path: '', component: HomePage, title: 'Inicio' },
  { path: 'plano', component: PlanoPage, title: 'Plano' },
  { path: 'agendamentos', component: AgendamentosPage, title: 'Meus Agendamentos' },
  { path: 'perfil', component: PerfilPage, title: 'Perfil' },
  { path: 'agendamentos/novo', component: NovoAgendamentoPage, title: 'Novo Agendamento' },
  { path: '**', redirectTo: '' },
];
