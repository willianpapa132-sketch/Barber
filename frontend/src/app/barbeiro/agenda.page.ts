import { Component, ElementRef, HostListener, Input, OnChanges, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { BarberNav } from './layout';
import { BarberApi, minutes, hhmm, dateLabel, money, statusNames } from './api';
import { Appointment, Customer, CustomerServices, Grade, Period, Service } from './models';

interface TimelineEvent {
  start: number; end: number; item?: Appointment; period?: Period;
  top: number; height: number; left: string; width: string;
}
@Component({ selector: 'app-barber-agenda', imports: [FormsModule, RouterLink, BarberNav, NgTemplateOutlet], templateUrl: './agenda.page.html' })
export class BarberAgendaPage implements OnInit, OnDestroy, OnChanges {
  @Input() adminBarbeiroId: number | null = null;
  @Input() adminNome = '';
  @Input() adminStatus = '';
  @Input() initialGrade: Grade | null = null;
  @Input() timelineRange: { inicio: string; fim: string } | null = null;
  get apiBase() { return this.adminBarbeiroId === null ? '/api/barbeiro/agenda' : `/api/admin/agendamentos/${this.adminBarbeiroId}`; }
  ngOnChanges() {
    if (this.initialGrade) {
      this.grade.set(this.initialGrade); this.date = this.initialGrade.data;
      this.status = this.adminStatus;
      this.buildTimeline(this.initialGrade);
    }
  }
  private api = inject(BarberApi);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private gradeController?: AbortController;
  private servicesController?: AbortController;
  private searchController?: AbortController;
  private lifetime = new AbortController();
  private searchTimer?: ReturnType<typeof setTimeout>;
  @ViewChild('dialog') dialog!: ElementRef<HTMLDialogElement>;
  grade = signal<Grade | null>(null);
  loading = signal(false); error = signal(''); notice = signal('');
  saving = signal(false); servicesLoading = signal(false); dialogError = signal('');
  customers = signal<Customer[]>([]); services = signal<Service[]>([]); activePlan = signal('');
  mode: 'menu' | 'block' | 'appointment' = 'menu';
  customerMode = 'registered'; clientId = ''; search = ''; clientName = ''; clientPhone = '';
  selectedIds: number[] = []; appointmentTime = ''; blockStart = ''; blockEnd = ''; preferredTime = '';
  date = this.route.snapshot.queryParamMap.get('data') || '';
  status = this.route.snapshot.queryParamMap.get('status') || '';
  hhmm = hhmm; dateLabel = dateLabel; money = money; statusNames = statusNames; minutes = minutes;
  events: TimelineEvent[] = []; outside: Appointment[] = [];
  rows: { time: string; top: number; height: number }[] = [];
  timelineHeight = 0;
  ngOnInit() { if (!this.initialGrade) void this.load(); }
  ngOnDestroy() {
    this.gradeController?.abort(); this.servicesController?.abort(); this.searchController?.abort();
    this.lifetime.abort(); clearTimeout(this.searchTimer); this.dialog.nativeElement.close();
  }
  @HostListener('window:focus') onFocus() { if (!this.dialog?.nativeElement.open) void this.load(); }
  async load() {
    if (this.saving()) return;
    this.gradeController?.abort(); const controller = this.gradeController = new AbortController();
    this.loading.set(true); this.error.set(''); this.notice.set('');
    const query = new URLSearchParams(); if (this.date) query.set('data', this.date); if (this.status) query.set('status', this.status);
    try {
      const grade = await this.api.request<Grade>(`${this.apiBase}/grade?${query}`, { signal: controller.signal });
      if (controller.signal.aborted) return;
      this.grade.set(grade); this.date = grade.data; this.buildTimeline(grade);
      this.notice.set(grade.aviso || (!grade.agendamentos.length ? 'Nenhum agendamento encontrado para este filtro. Os horários da jornada continuam visíveis.' : ''));
      if (this.adminBarbeiroId === null) await this.router.navigate([], { relativeTo: this.route, queryParams: { data: this.date, status: this.status || null }, replaceUrl: true });
    } catch (e) { if (!controller.signal.aborted) this.error.set((e as Error).message); }
    finally { if (!controller.signal.aborted) this.loading.set(false); }
  }
  private buildTimeline(grade: Grade) {
    this.events = []; this.outside = []; this.rows = []; this.timelineHeight = 0;
    const start = minutes(this.timelineRange?.inicio || grade.inicio), end = minutes(this.timelineRange?.fim || grade.fim);
    if (!grade.horarios.length) { this.outside = grade.agendamentos; return; }
    const events: TimelineEvent[] = [];
    const add = (s: number, e: number, item?: Appointment, period?: Period) => events.push({ start: s, end: e, item, period, top: 0, height: 0, left: '', width: '' });
    grade.agendamentos.forEach(item => {
      const s = minutes(item.horaInicio), e = minutes(item.horaFinalizacao);
      if (!Number.isFinite(s) || !Number.isFinite(e) || s < start || e > end || e <= s) this.outside.push(item);
      else add(s, e, item);
    });
    grade.indisponibilidades.filter(p => p.tipo !== 'OCUPADO').forEach(p => {
      const s = Math.max(start, Number.isFinite(minutes(p.inicio)) ? minutes(p.inicio) : start);
      const e = Math.min(end, Number.isFinite(minutes(p.fim)) ? minutes(p.fim) : end);
      if (e > s) add(s, e, undefined, p);
    });
    const scale = this.adminBarbeiroId === null ? Math.max(5, 58 / Math.max(Math.min(15, ...events.map(e => e.end - e.start)), 1)) : 5;
    this.timelineHeight = (end - start) * scale;
    this.rows = grade.horarios.map((time, index) => ({ time, top: (minutes(time) - start) * scale,
      height: ((index + 1 < grade.horarios.length ? minutes(grade.horarios[index + 1]) : minutes(grade.fim)) - minutes(time)) * scale }));
    events.sort((a, b) => a.start - b.start || b.end - a.end);
    let group: TimelineEvent[] = [], groupEnd = -1;
    const position = () => {
      const laneEnds: number[] = []; const lanes = new Map<TimelineEvent, number>();
      group.forEach(e => { let lane = laneEnds.findIndex(end => end <= e.start); if (lane < 0) lane = laneEnds.length; laneEnds[lane] = e.end; lanes.set(e, lane); });
      group.forEach(e => { e.top = (e.start - start) * scale; e.height = (e.end - e.start) * scale - 4;
        e.left = `calc(${lanes.get(e)! * 100 / laneEnds.length}% + 2px)`; e.width = `calc(${100 / laneEnds.length}% - 4px)`; });
    };
    events.forEach(e => { if (group.length && e.start >= groupEnd) { position(); group = []; groupEnd = -1; } group.push(e); groupEnd = Math.max(groupEnd, e.end); });
    if (group.length) position(); this.events = events;
  }
  canFit(time: string, duration: number) {
    const grade = this.grade(); if (!grade?.podeCriar || !time || !Number.isFinite(duration) || duration <= 0 || !Number.isFinite(minutes(time))) return false;
    const start = minutes(time), end = start + duration;
    if (start < minutes(grade.inicio) || end > minutes(grade.fim) || new Date(`${grade.data}T${hhmm(time)}:00`) <= new Date()) return false;
    return !grade.indisponibilidades.some(p => start < (Number.isFinite(minutes(p.fim)) ? minutes(p.fim) : minutes(grade.fim)) && end > (Number.isFinite(minutes(p.inicio)) ? minutes(p.inicio) : minutes(grade.inicio)));
  }
  get selectedServices() { return this.services().filter(s => this.selectedIds.includes(s.id)); }
  get duration() { return this.selectedServices.reduce((sum, s) => sum + s.duracaoMinutos, 0); }
  get serviceTotal() { return this.selectedServices.reduce((sum, s) => sum + Number(s.precoLiquido), 0); }
  get times() { return this.grade()?.horarios.map(hhmm) || []; }
  get blockEnds() { return [...this.times, hhmm(this.grade()?.fim || '')].filter(t => minutes(t) > minutes(this.blockStart)); }
  get canSaveAppointment() {
    return !this.saving() && !this.servicesLoading() && this.selectedIds.length > 0 && this.canFit(this.appointmentTime, this.duration) && (this.customerMode === 'walkin' || !!this.clientId);
  }
  updateTimes() { if (!this.canFit(this.appointmentTime, this.duration || 1)) this.appointmentTime = this.times.find(t => this.canFit(t, this.duration || 1)) || ''; }
  updateBlockEnd() { if (!this.canFit(this.blockStart, minutes(this.blockEnd) - minutes(this.blockStart))) this.blockEnd = this.blockEnds.find(t => this.canFit(this.blockStart, minutes(t) - minutes(this.blockStart))) || ''; }
  toggleService(id: number, checked: boolean) { this.selectedIds = checked ? [...this.selectedIds, id] : this.selectedIds.filter(i => i !== id); this.updateTimes(); }
  open(time = '') {
    if (this.loading() || !this.grade()?.podeCriar) return;
    this.preferredTime = hhmm(time); this.mode = 'menu'; this.dialogError.set(''); this.dialog.nativeElement.showModal();
  }
  close() { if (!this.saving()) this.dialog.nativeElement.close(); }
  async editor(kind: 'block' | 'appointment') {
    this.mode = kind; this.dialogError.set('');
    if (kind === 'block') {
      this.blockStart = this.canFit(this.preferredTime, 1) ? this.preferredTime : this.times.find(t => this.canFit(t, 1)) || '';
      this.blockEnd = ''; this.updateBlockEnd();
    } else { this.appointmentTime = this.preferredTime; await Promise.all([this.loadServices(), this.searchClients()]); }
  }
  async loadServices() {
    this.servicesController?.abort(); const controller = this.servicesController = new AbortController();
    this.services.set([]); this.selectedIds = []; this.activePlan.set(''); this.servicesLoading.set(true); this.dialogError.set('');
    if (this.customerMode === 'registered' && !this.clientId) { this.servicesLoading.set(false); this.updateTimes(); return; }
    try {
      if (this.customerMode === 'registered') {
        const data = await this.api.request<CustomerServices>(`${this.apiBase}/clientes/${encodeURIComponent(this.clientId)}/servicos`, { signal: controller.signal });
        if (controller.signal.aborted) return; this.services.set(data.servicos); this.activePlan.set(data.possuiPlano ? data.nomePlano || 'Plano mensal' : '');
      } else {
        const data = await this.api.request<Service[]>(`${this.apiBase}/servicos`, { signal: controller.signal });
        if (controller.signal.aborted) return; this.services.set(data);
      }
      this.updateTimes();
    } catch (e) { if (!controller.signal.aborted) this.dialogError.set((e as Error).message); }
    finally { if (!controller.signal.aborted) this.servicesLoading.set(false); }
  }
  searchChanged() {
    this.searchController?.abort(); this.clientId = ''; void this.loadServices(); clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => void this.searchClients(), 300);
  }
  async searchClients() {
    this.searchController?.abort(); const controller = this.searchController = new AbortController();
    try {
      const customers = await this.api.request<Customer[]>(`${this.apiBase}/clientes?busca=${encodeURIComponent(this.search)}`, { signal: controller.signal });
      if (controller.signal.aborted) return; this.customers.set(customers);
      if (this.clientId && !customers.some(c => String(c.id) === this.clientId)) { this.clientId = ''; await this.loadServices(); }
    } catch (e) { if (!controller.signal.aborted) this.dialogError.set((e as Error).message); }
  }
  async save(kind: 'block' | 'appointment') {
    if (this.saving() || this.loading()) return;
    let payload: object;
    if (kind === 'block') {
      if (!this.canFit(this.blockStart, minutes(this.blockEnd) - minutes(this.blockStart))) { this.dialogError.set('Selecione um período livre.'); return; }
      payload = { data: this.grade()!.data, horaInicio: this.blockStart, horaFim: this.blockEnd };
    } else {
      if (!this.canSaveAppointment) return;
      payload = { data: this.grade()!.data, horaInicio: this.appointmentTime, servicosIds: this.selectedIds,
        ...(this.customerMode === 'walkin' ? { nomeCliente: this.clientName.trim(), telefoneCliente: this.clientPhone.trim() } : { clienteId: Number(this.clientId) }) };
    }
    this.saving.set(true); this.dialogError.set('');
    try {
      await this.api.request(`${this.apiBase}/${kind === 'block' ? 'bloqueios' : 'agendamentos'}`, { method: 'POST', body: JSON.stringify(payload), signal: this.lifetime.signal });
      this.dialog.nativeElement.close(); this.saving.set(false); await this.load();
      this.notice.set(kind === 'block' ? 'Bloqueio criado com sucesso.' : 'Agendamento criado com sucesso. Confira a data e o filtro selecionados.');
    } catch (e) { if (!this.lifetime.signal.aborted) this.dialogError.set((e as Error).message); }
    finally { this.saving.set(false); }
  }
  async removeBlock(id: number) {
    if (this.saving() || !window.confirm('Remover este bloqueio e liberar o período?')) return;
    this.saving.set(true);
    try { await this.api.request(`${this.apiBase}/bloqueios/${id}`, { method: 'DELETE', signal: this.lifetime.signal }); this.saving.set(false); await this.load(); this.notice.set('Bloqueio removido.'); }
    catch (e) { if (!this.lifetime.signal.aborted) this.error.set((e as Error).message); }
    finally { this.saving.set(false); }
  }
}
