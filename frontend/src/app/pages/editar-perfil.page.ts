import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

interface ClientePerfil {
  id: number;
  nome: string;
  telefone: string;
}

@Component({
  selector: 'app-editar-perfil-page',
  imports: [FormsModule, RouterLink],
  template: `
    <main class="client-shell profile-page-shell edit-profile-shell">
      <header class="profile-edit-topbar">
        <a routerLink="/perfil" aria-label="Voltar">&lsaquo;</a>
        <h1>Editar perfil</h1>
      </header>


      @if (errorMessage) {
        <section class="profile-state-card error">
          <strong>{{ errorMessage }}</strong>
        </section>
      }

      <form class="edit-profile-form" (ngSubmit)="save()">
        <label>
          <span>Nome</span>
          <div>
            <i aria-hidden="true">&#9817;</i>
            <input name="nome" type="text" [(ngModel)]="nome" required maxlength="120" autocomplete="name" />
          </div>
        </label>

        <label>
          <span>Telefone</span>
          <div>
            <i aria-hidden="true">&#9742;</i>
            <input name="telefone" type="tel" [(ngModel)]="telefone" required maxlength="20" autocomplete="tel" />
          </div>
        </label>


        <button class="save-profile-button" type="submit" [disabled]="saving">
          <span aria-hidden="true">&#128190;</span>
          {{ saving ? 'Salvando...' : 'Salvar alteracoes' }}
        </button>
      </form>

    </main>
  `,
})
export class EditarPerfilPage implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected nome = '';
  protected telefone = '';
  protected saving = false;
  protected errorMessage = '';


  ngOnInit(): void {
    this.http.get<ClientePerfil>('/api/cliente/perfil').subscribe({
      next: (profile) => {
        this.nome = profile.nome;
        this.telefone = profile.telefone;
      },
      error: () => {
        this.errorMessage = 'Nao foi possivel carregar seu perfil.';
      },
    });
  }

  protected save(): void {
    if (!this.nome.trim() || !this.telefone.trim()) {
      this.errorMessage = 'Informe nome e telefone.';
      return;
    }

    this.saving = true;
    this.errorMessage = '';

    this.http
      .put<ClientePerfil>('/api/cliente/perfil', {
        nome: this.nome,
        telefone: this.telefone,
      })
      .subscribe({
        next: () => {
          this.router.navigateByUrl('/perfil');
        },
        error: () => {
          this.errorMessage = 'Nao foi possivel salvar as alteracoes.';
          this.saving = false;
        },
      });
  }

}
