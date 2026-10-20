import { Routes } from '@angular/router';
import { AgendamentosPage } from './pages/agendamentos.page';
import { AgendamentoDetalhePage } from './pages/agendamento-detalhe.page';
import { HomePage } from './pages/home.page';
import { NovoAgendamentoPage } from './pages/novo-agendamento.page';
import { EditarPerfilPage } from './pages/editar-perfil.page';
import { PerfilPage } from './pages/perfil.page';
import { PlanoPage } from './pages/plano.page';
import { BarberLayout } from './barbeiro/layout';

export const routes: Routes = [
  { path: 'admin/cadastros/planos-mensais', loadComponent: () => import('./admin/planos-mensais.page').then(m => m.AdminPlanosMensaisPage), title: 'Planos Mensais' },
  { path: 'admin/cadastros/servicos', loadComponent: () => import('./admin/servicos.page').then(m => m.AdminServicosPage), title: 'Servi\u00e7os' },
  { path: 'admin/cadastros/barbeiros', loadComponent: () => import('./admin/barbeiros.page').then(m => m.AdminBarbeirosPage), title: 'Cadastro de Barbeiros' },
  { path: 'admin/cadastros', loadComponent: () => import('./admin/cadastros.page').then(m => m.AdminCadastrosPage), title: 'Cadastros' },
  { path: 'admin', redirectTo: 'admin/home', pathMatch: 'full' },
  { path: 'admin/home', loadComponent: () => import('./admin/home.page').then(m => m.AdminHomePage), title: 'Painel administrativo' },
  { path: 'barbeiro', component: BarberLayout, children: [
    { path: '', redirectTo: 'home', pathMatch: 'full' },
    { path: 'home', loadComponent: () => import('./barbeiro/home.page').then(m => m.BarberHomePage), title: 'Home do barbeiro' },
    { path: 'carteira/pontos', loadComponent: () => import('./barbeiro/valores-formulado.page').then(m => m.BarberStatementPage), data: { tipo: 'pontos' }, title: 'Pontos do plano' },
    { path: 'carteira/comissoes', loadComponent: () => import('./barbeiro/valores-formulado.page').then(m => m.BarberStatementPage), data: { tipo: 'comissoes' }, title: 'Valor liquido de comissao' },
    { path: 'carteira', loadComponent: () => import('./barbeiro/carteira.page').then(m => m.BarberWalletPage), title: 'Carteira do barbeiro' },
    { path: 'agenda', loadComponent: () => import('./barbeiro/agenda.page').then(m => m.BarberAgendaPage), title: 'Agenda do barbeiro' },
    { path: 'agendamentos/:id/finalizacao', loadComponent: () => import('./barbeiro/finish.page').then(m => m.BarberFinishPage), title: 'Finalizar agendamento' },
  ] },
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
