import { Routes } from '@angular/router';
import { AgendamentosPage } from './pages/agendamentos.page';
import { AgendamentoDetalhePage } from './pages/agendamento-detalhe.page';
import { HomePage } from './pages/home.page';
import { NovoAgendamentoPage } from './pages/novo-agendamento.page';
import { EditarPerfilPage } from './pages/editar-perfil.page';
import { PerfilPage } from './pages/perfil.page';
import { PlanoPage } from './pages/plano.page';

export const routes: Routes = [
  { path: '', component: HomePage, title: 'Inicio' },
  { path: 'cliente/home', component: HomePage, title: 'Inicio' },
  { path: 'plano', component: PlanoPage, title: 'Plano' },
  { path: 'agendamentos', component: AgendamentosPage, title: 'Meus Agendamentos' },
  { path: 'agendamentos/novo', component: NovoAgendamentoPage, title: 'Novo Agendamento' },
  { path: 'agendamentos/:id', component: AgendamentoDetalhePage, title: 'Detalhes do Agendamento' },
  { path: 'perfil', component: PerfilPage, title: 'Perfil' },
  { path: 'perfil/editar', component: EditarPerfilPage, title: 'Editar Perfil' },
  { path: '**', redirectTo: '' },
];
