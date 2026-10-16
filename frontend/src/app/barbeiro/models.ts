export interface Appointment {
  id: number; nomeCliente: string; horaInicio: string; horaFinalizacao: string;
  duracaoMinutos: number; servicos: string[]; status: string;
}
export interface BarberHome {
  barbeiro: { id: number; nome: string; imgUrl: string }; data: string;
  agendamentosHoje: number; concluidosHoje: number; canceladosHoje: number;
  proximosAgendamentos: Appointment[];
}
export interface Period { id: number | null; tipo: string; descricao: string; inicio: string; fim: string }
export interface Grade {
  data: string; inicio: string; fim: string; intervaloGrade: number; horarios: string[];
  agendamentos: Appointment[]; indisponibilidades: Period[]; podeCriar: boolean; aviso: string;
}
export interface Service { id: number; nome: string; preco: number; duracaoMinutos: number }
export interface Customer { id: number; nome: string; telefone: string }
export interface CustomerServices { possuiPlano: boolean; nomePlano: string; servicos: Service[] }
export interface Finish {
  id: number; nomeCliente: string; data: string; horaInicio: string; horaTermino: string;
  duracaoMinutos: number; status: string; atendimentoMensal: boolean; servicos: Service[];
  valorTotalServicos: number; valorCobrar: number; formaPagamento: string | null;
  formasPagamentoDisponiveis: string[]; podeFinalizar: boolean; motivoBloqueio: string;
  comissao: { percentualAplicado: number; valorBase: number; valorComissao: number } | null;
  comissaoPlano: { nomePlano: string; percentualAplicado: number } | null;
}
