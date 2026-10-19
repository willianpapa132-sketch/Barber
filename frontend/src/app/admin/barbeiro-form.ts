import { Component, OnInit, AfterViewInit, OnDestroy, ElementRef, ViewChild, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface Dia { diaSemana: string; folga: boolean; horaInicio: string | null; horaFim: string | null; intervaloInicio: string | null; intervaloFim: string | null }
interface Cadastro { id?: number; nome: string; telefone: string; login: string; senha?: string; comissao: number | null; ativo: boolean; imgUrl: string | null; jornada: Dia[] }

@Component({
  selector: 'app-barbeiro-form', imports: [FormsModule],
  templateUrl: './barbeiro-form.html', styleUrl: './barbeiro-form.scss'
})
export class BarbeiroForm implements OnInit, AfterViewInit, OnDestroy {
  id = input<number | null>(null);
  closed = output<void>();
  saved = output<void>();
  @ViewChild('dialog', { static: true }) dialog!: ElementRef<HTMLDialogElement>;
  loading = signal(false); saving = signal(false); error = signal('');
  showPassword = false;
  private controller = new AbortController();
  readonly nomes = ['Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'Sábado', 'Domingo'];
  model: Cadastro = {
    nome: '', telefone: '', login: '', senha: '', comissao: 50, ativo: true, imgUrl: null,
    jornada: ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'].map((diaSemana, i) => ({
      diaSemana, folga: i >= 5, horaInicio: '09:00', horaFim: '18:00', intervaloInicio: '12:00', intervaloFim: '13:00'
    }))
  };
  ngOnInit() { if (this.id() !== null) void this.load(); }
  ngAfterViewInit() { this.dialog.nativeElement.showModal(); }
  ngOnDestroy() { this.controller.abort(); this.dialog.nativeElement.close(); }
  close() { if (!this.saving()) this.closed.emit(); }
  async load() {
    this.loading.set(true);
    try { this.model = { ...await this.request<Cadastro>('/' + this.id()), senha: '' }; }
    catch (e) { this.error.set((e as Error).message); }
    finally { this.loading.set(false); }
  }
  async request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const response = await fetch('/api/admin/barbeiros' + path, { ...options, signal: this.controller.signal,
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' } });
    if (response.redirected || response.status === 401) { window.location.assign('/login'); throw new Error('Entre novamente.'); }
    const data = await response.json().catch(() => null);
    if (!response.ok) throw new Error(data?.mensagem || 'Não foi possível salvar o cadastro.');
    return data as T;
  }
  async foto(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (!['image/png','image/jpeg'].includes(file.type) || file.size > 2 * 1024 * 1024) {
      this.error.set('Selecione uma foto PNG ou JPG de até 2 MB.'); input.value = ''; return;
    }
    const reader = new FileReader();
    reader.onload = () => { this.model.imgUrl = reader.result as string; this.error.set(''); };
    reader.onerror = () => this.error.set('Não foi possível ler a foto.');
    reader.readAsDataURL(file);
  }
  async salvar() {
    this.error.set('');
    for (let i = 0; i < 7; i++) {
      const dia = this.model.jornada[i];
      if (dia.folga) continue;
      if (!dia.horaInicio || !dia.horaFim || dia.horaInicio >= dia.horaFim) { this.error.set('Confira o início e fim da jornada de ' + this.nomes[i] + '.'); return; }
      if (!!dia.intervaloInicio !== !!dia.intervaloFim || (dia.intervaloInicio && dia.intervaloFim &&
          (dia.intervaloInicio >= dia.intervaloFim || dia.intervaloInicio < dia.horaInicio || dia.intervaloFim > dia.horaFim))) {
        this.error.set('Confira o intervalo de ' + this.nomes[i] + '.'); return;
      }
    }
    this.saving.set(true);
    const payload = { ...this.model, jornada: this.model.jornada.map(d => ({ ...d,
      horaInicio: d.folga ? null : d.horaInicio, horaFim: d.folga ? null : d.horaFim,
      intervaloInicio: d.folga || !d.intervaloInicio ? null : d.intervaloInicio,
      intervaloFim: d.folga || !d.intervaloFim ? null : d.intervaloFim
    })) };
    try { await this.request(this.id() === null ? '' : '/' + this.id(), { method: this.id() === null ? 'POST' : 'PUT', body: JSON.stringify(payload) }); this.saved.emit(); }
    catch (e) { this.error.set((e as Error).message); }
    finally { this.saving.set(false); }
  }
}
