import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class BarberApi {
  async request<T>(url: string, options: RequestInit = {}): Promise<T> {
    const response = await fetch(url, { credentials: 'same-origin', ...options,
      headers: { Accept: 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers } });
    if (response.redirected || response.status === 401) {
      window.location.assign('/login');
      throw new Error('Sua sessão expirou. Entre novamente.');
    }
    if (response.status === 204) return undefined as T;
    const data = await response.json().catch(() => null);
    if (!response.ok) throw new Error(data?.mensagem || data?.message || 'Não foi possível concluir esta ação. Tente novamente.');
    if (!data) throw new Error('Resposta inválida ao consultar o atendimento.');
    return data as T;
  }
}
export const statusNames: Record<string, string> = {
  AGENDADO: 'Agendado', CONCLUIDO: 'Concluído', CANCELADO: 'Cancelado', NAO_COMPARECEU: 'Não compareceu',
};
export const hhmm = (time: string) => time?.slice(0, 5) || '';
export const dateLabel = (date: string) => date?.split('-').reverse().join('/') || '';
export const money = (value: number) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
export const minutes = (time: string | null | undefined): number => {
  if (!time) return NaN;
  const [hours, mins, secs = 0] = time.split(':').map(Number);
  return hours * 60 + mins + secs / 60;
};
