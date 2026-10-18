export const dataLocal = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
export const hojeCarteira = () => dataLocal(new Date());
export const inicioMesCarteira = () => dataLocal(new Date(new Date().getFullYear(), new Date().getMonth(), 1));
export function dataValidaCarteira(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date = new Date(`${value}T12:00:00`);
  return Number.isFinite(date.getTime()) && dataLocal(date) === value;
}
export function fimMaximoCarteira(inicio: string): string {
  if (!dataValidaCarteira(inicio)) return '';
  const start = new Date(`${inicio}T12:00:00`);
  const target = new Date(start.getFullYear(), start.getMonth() + 3, 1, 12);
  const lastDay = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate();
  target.setDate(Math.min(start.getDate(), lastDay)); target.setDate(target.getDate() - 1);
  return dataLocal(target);
}
export function validarPeriodoCarteira(inicio: string, fim: string): string {
  if (!dataValidaCarteira(inicio) || !dataValidaCarteira(fim)) return 'Informe as datas de início e fim.';
  if (fim < inicio) return 'A data final deve ser igual ou posterior à data inicial.';
  if (fim > fimMaximoCarteira(inicio)) return 'Selecione um período de no máximo 3 meses.';
  return '';
}
