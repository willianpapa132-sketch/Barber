import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BottomNav } from '../shared/bottom-nav';
import { FormsModule } from '@angular/forms';

interface BarbeiroPreferencia { id: number; nome: string }

interface ServicoPlano {
  id: number;
  nome: string;
  preco: number;
  duracaoMinutos: number;
}

interface PlanoDisponivel {
  id: number;
  nome: string;
  descricao: string;
  valorMensal: number;
  diasMaximoAntecedencia: number;
  servicos: ServicoPlano[];
}

interface PlanoAtual extends PlanoDisponivel {
  planoMensalId: number;
  contratadoEm: string;
  barbeiroPreferidoId: number | null;
  barbeiroPreferidoNome: string | null;
}

interface PlanoPageResponse {
  possuiPlano: boolean;
  planoAtual: PlanoAtual | null;
  planosDisponiveis: PlanoDisponivel[];
  barbeiros: BarbeiroPreferencia[];
}

@Component({
  selector: 'app-plano-page',
  imports: [RouterLink, BottomNav, FormsModule],
  styles: [`.preference-card{margin:20px 0;padding:20px;border:1px solid #354650;border-radius:14px;background:#12212a;color:#edf2f5}.preference-card label{display:block;margin-bottom:10px}.preference-card select{width:100%;padding:12px;border-radius:8px;background:#0b1923;color:#edf2f5;border:1px solid #354650;font:inherit}.preference-card p{font-size:14px;line-height:1.5}.preference-card button{margin-top:14px}.plan-feedback{padding:12px;color:#ff9ca5}.plan-feedback.success{color:#64dca4}`],
  template: `
    <main class="client-shell plan-page-shell">
      <header class="plan-topbar">
        <a class="plan-back" routerLink="/" aria-label="Voltar para o inicio">&lsaquo;</a>
        <h1>Plano Mensal</h1>
      </header>

      @if (loading) {
        <section class="plan-state-card">
          <strong>Carregando planos...</strong>
        </section>
      } @else if (errorMessage) {
        <section class="plan-state-card error">
          <strong>{{ errorMessage }}</strong>
          <button type="button" (click)="loadPlans()">Tentar novamente</button>
        </section>
      } @else {
        @if(operationError){<p class="plan-feedback" role="alert">{{operationError}}</p>}
        @if(successMessage){<p class="plan-feedback success" role="status">{{successMessage}}</p>}
        <section class="preference-card">
          <label for="barbeiro-preferido">Barbeiro de preferência (opcional)</label>
          <select id="barbeiro-preferido" [(ngModel)]="barbeiroPreferidoId" [disabled]="savingPlanId !== null || savingPreference">
            <option [ngValue]="null">Sem preferência — escolher ao agendar</option>
            @if(planoAtual?.barbeiroPreferidoId && !preferidoDisponivel){
              <option [ngValue]="planoAtual!.barbeiroPreferidoId" disabled>{{planoAtual!.barbeiroPreferidoNome}} (indisponível)</option>
            }
            @for(barbeiro of barbeiros;track barbeiro.id){<option [ngValue]="barbeiro.id">{{barbeiro.nome}}</option>}
          </select>
          <p>Se escolher um profissional, seus próximos agendamentos abrirão com ele selecionado. A equipe pode agendar com outro barbeiro quando necessário.</p>
          @if(planoAtual){
            <p>Preferência atual: <strong>{{planoAtual.barbeiroPreferidoNome || 'Sem preferência'}}</strong></p>
            <button class="choose-plan-button" type="button" (click)="salvarPreferencia()" [disabled]="savingPreference || savingPlanId !== null">{{savingPreference ? 'Salvando...' : 'Salvar preferência'}}</button>
          }
        </section>
        @if (planoAtual && !showAvailablePlans) {
          <section class="current-plan-view">
            <h2>Seu plano atual</h2>
            <article class="current-plan-card">
              <div class="current-plan-head">
                <span class="plan-crown" aria-hidden="true">&#9819;</span>
                <div>
                  <strong>{{ planoAtual.nome }}</strong>
                  <small class="current-badge"><span aria-hidden="true">&#9819;</span> Seu plano atual</small>
                </div>
                <p><b>{{ formatPrice(planoAtual.valorMensal) }}</b><small>/mes</small></p>
              </div>

              <div class="plan-card-divider"></div>

              <div class="current-plan-details">
                <ul class="plan-service-list">
                  @for (servico of planoAtual.servicos; track servico.id) {
                    <li><span aria-hidden="true">&#10003;</span>{{ servico.nome }}</li>
                  }
                </ul>
                <div class="advance-info">
                  <span aria-hidden="true">&#128197;</span>
                  <p>Agende com ate<br />{{ planoAtual.diasMaximoAntecedencia }} dias de antecedencia</p>
                </div>
              </div>
            </article>

            <button class="upgrade-card" type="button" (click)="showAvailablePlans = true">
              <span aria-hidden="true">&#8599;</span>
              <span>
                <strong>Quer mais beneficios?</strong>
                <small>Veja outros planos e faca um upgrade do seu plano atual.</small>
              </span>
              <i aria-hidden="true">&rsaquo;</i>
            </button>

            <section class="plan-benefits-card">
              <h3><span aria-hidden="true">%</span> Vantagens do plano mensal</h3>
              <ul>
                <li><span aria-hidden="true">&#10003;</span>Mais economia no seu estilo</li>
                <li><span aria-hidden="true">&#10003;</span>Agendamento com antecedencia</li>
                <li><span aria-hidden="true">&#10003;</span>Seus servicos favoritos sempre disponiveis</li>
                <li><span aria-hidden="true">&#10003;</span>Praticidade no dia a dia</li>
              </ul>
            </section>
          </section>
        } @else {
          <section class="plans-list-view">
            @if (!planoAtual) {
              <section class="plans-intro-card">
                <span class="plan-crown" aria-hidden="true">&#9819;</span>
                <div>
                  <strong>Planos mensais</strong>
                  <p>Mais praticidade, economia e vantagens para o seu estilo.</p>
                </div>
              </section>
            } @else {
              <button class="plan-inline-back" type="button" (click)="showAvailablePlans = false">&lsaquo; Meu plano atual</button>
            }

            <h2>Nossos planos</h2>

            @if (planosDisponiveis.length) {
              <div class="plans-grid">
                @for (plano of planosDisponiveis; track plano.id) {
                  <article class="available-plan-card">
                    <div class="available-plan-head">
                      <strong>{{ plano.nome }}</strong>
                      <p><b>{{ formatPrice(plano.valorMensal) }}</b><small>/mes</small></p>
                    </div>
                    @if (plano.descricao) {
                      <p class="plan-description">{{ plano.descricao }}</p>
                    }
                    <div class="available-plan-body">
                      <ul class="plan-service-list">
                        @for (servico of plano.servicos; track servico.id) {
                          <li><span aria-hidden="true">&#10003;</span>{{ servico.nome }}</li>
                        }
                      </ul>
                      <div class="advance-info">
                        <span aria-hidden="true">&#128197;</span>
                        <p>Agende com ate<br />{{ plano.diasMaximoAntecedencia }} dias de antecedencia</p>
                      </div>
                    </div>
                    <button
                      class="choose-plan-button"
                      type="button"
                      [disabled]="savingPlanId !== null || savingPreference"
                      (click)="contratarPlano(plano.id)"
                    >
                      {{ savingPlanId === plano.id ? 'Contratando...' : 'Escolher plano' }}
                      <span aria-hidden="true">&rsaquo;</span>
                    </button>
                  </article>
                }
              </div>
            } @else {
              <section class="plan-state-card">
                <strong>Nenhum plano disponivel no momento.</strong>
              </section>
            }
          </section>
        }
      }
    </main>
    <app-bottom-nav current="plano" />
  `,
})
export class PlanoPage implements OnInit {
  private readonly http = inject(HttpClient);

  protected loading = true;
  protected errorMessage = '';
  protected showAvailablePlans = false;
  protected savingPlanId: number | null = null;
  protected planoAtual: PlanoAtual | null = null;
  protected planosDisponiveis: PlanoDisponivel[] = [];
  protected barbeiros: BarbeiroPreferencia[] = [];
  protected barbeiroPreferidoId: number | null = null;
  protected savingPreference = false;
  protected operationError = '';
  protected successMessage = '';
  protected get preferidoDisponivel(): boolean { return this.barbeiros.some(b => b.id === this.planoAtual?.barbeiroPreferidoId); }

  protected salvarPreferencia(): void {
    if(this.savingPreference || this.savingPlanId !== null)return;
    this.savingPreference=true;this.operationError='';this.successMessage='';
    this.http.put<PlanoAtual>('/api/cliente/planos/preferencia',{barbeiroPreferidoId:this.barbeiroPreferidoId}).subscribe({
      next: plano => { this.planoAtual=plano;this.savingPreference=false;this.successMessage='Preferência atualizada. Seus agendamentos existentes foram mantidos.'; },
      error: error => { this.operationError=error.error?.mensagem || 'Não foi possível salvar a preferência.';this.savingPreference=false; }
    });
  }

  ngOnInit(): void {
    this.loadPlans();
  }

  protected loadPlans(): void {
    this.loading = true;
    this.errorMessage = '';

    this.http.get<PlanoPageResponse>('/api/cliente/planos').subscribe({
      next: (response) => {
        this.planoAtual = response.planoAtual;
        this.planosDisponiveis = response.planosDisponiveis;
        this.barbeiros = response.barbeiros;
        this.barbeiroPreferidoId = response.planoAtual?.barbeiroPreferidoId ?? null;
        this.showAvailablePlans = !response.possuiPlano;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Nao foi possivel carregar os planos.';
        this.loading = false;
      },
    });
  }

  protected contratarPlano(planoId: number): void {
    if(this.savingPlanId !== null || this.savingPreference)return;
    this.savingPlanId = planoId;
    this.operationError = '';this.successMessage='';

    this.http.post<PlanoAtual>(`/api/cliente/planos/${planoId}/contratar`, {barbeiroPreferidoId:this.barbeiroPreferidoId}).subscribe({
      next: (planoAtual) => {
        this.planoAtual = planoAtual;
        this.showAvailablePlans = false;
        this.savingPlanId = null;
        this.loadPlans();
      },
      error: (error) => {
        this.operationError = error.error?.mensagem || 'Nao foi possivel contratar o plano selecionado.';
        this.savingPlanId = null;
      },
    });
  }

  protected formatPrice(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }
}
