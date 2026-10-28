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
  { path: 'plataforma', loadComponent: () => import('./platform/platform.page').then(m => m.PlatformPage), title: 'Administração da plataforma' },
  { path: 'admin/configuracao', loadComponent: () => import('./admin/configuracao.page').then(m => m.AdminConfiguracaoPage), title: 'Configurações' },
  { path: 'admin/dashboard', loadComponent: () => import('./admin/dashboard.page').then(m => m.AdminDashboardPage), title: 'Dashboard' },
  { path: 'admin/comissao', loadComponent: () => import('./admin/comissao.page').then(m => m.AdminComissaoPage), title: 'Comissões' },
  { path: 'admin/agendamentos', loadComponent: () => import('./admin/agendamentos.page').then(m => m.AdminAgendamentosPage), title: 'Agendamentos' },
  { path: 'admin/caixa/:id/movimentacoes', loadComponent: () => import('./admin/caixa-movimentacoes.page').then(m => m.CaixaMovimentacoesPage), title: 'Movimentações do caixa' },
  { path: 'admin/gerenciamento-planos', loadComponent: () => import('./admin/gerenciamento-planos.page').then(m => m.GerenciamentoPlanosPage), title: 'Gerenciamento de Planos' },
  { path: 'admin/financeiro', loadComponent: () => import('./admin/financeiro.page').then(m => m.AdminFinanceiroPage), title: 'Financeiro' },
  { path: 'admin/caixa', loadComponent: () => import('./admin/caixa.page').then(m => m.AdminCaixaPage), title: 'Caixa' },
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
