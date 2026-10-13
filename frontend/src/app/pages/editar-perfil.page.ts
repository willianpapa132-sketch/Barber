import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

interface ClientePerfil {
  id: number;
  nome: string;
  telefone: string;
  imgUrl: string | null;
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

      <section class="edit-photo-block">
        <div class="profile-photo-wrap large">
          <img class="profile-photo" [alt]="'Foto de ' + nome" [src]="avatarUrl" />
          <button type="button" class="profile-edit-fab camera" (click)="photoSheetOpen = true" aria-label="Alterar foto">
            &#128247;
          </button>
        </div>
        <button type="button" class="change-photo-button" (click)="photoSheetOpen = true">Alterar foto</button>
      </section>

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

        <label>
          <span>Imagem</span>
          <div>
            <i aria-hidden="true">&#128247;</i>
            <input name="imgUrl" type="url" [(ngModel)]="imgUrl" maxlength="500" placeholder="URL da foto" />
          </div>
        </label>

        <button class="save-profile-button" type="submit" [disabled]="saving">
          <span aria-hidden="true">&#128190;</span>
          {{ saving ? 'Salvando...' : 'Salvar alteracoes' }}
        </button>
      </form>

      @if (photoSheetOpen) {
        <div class="profile-sheet-backdrop" (click)="photoSheetOpen = false"></div>
        <section class="profile-photo-sheet" aria-label="Alterar foto de perfil">
          <h2>Alterar foto de perfil</h2>
          <button type="button" (click)="focusImageInput()">
            <span aria-hidden="true">&#128247;</span>
            Informar URL da foto
          </button>
          <button type="button" class="danger" (click)="removePhoto()">
            <span aria-hidden="true">&#128465;</span>
            Remover foto atual
          </button>
          <button type="button" class="cancel" (click)="photoSheetOpen = false">Cancelar</button>
        </section>
      }
    </main>
  `,
})
export class EditarPerfilPage implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly fallbackAvatar =
    'https://images.unsplash.com/photo-1628157588553-5eeea00af15c?auto=format&fit=crop&w=300&q=80';

  protected nome = '';
  protected telefone = '';
  protected imgUrl = '';
  protected saving = false;
  protected photoSheetOpen = false;
  protected errorMessage = '';

  protected get avatarUrl(): string {
    return this.imgUrl || this.fallbackAvatar;
  }

  ngOnInit(): void {
    this.http.get<ClientePerfil>('/api/cliente/perfil').subscribe({
      next: (profile) => {
        this.nome = profile.nome;
        this.telefone = profile.telefone;
        this.imgUrl = profile.imgUrl ?? '';
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
        imgUrl: this.imgUrl || null,
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

  protected focusImageInput(): void {
    this.photoSheetOpen = false;
    window.setTimeout(() => {
      document.querySelector<HTMLInputElement>('input[name="imgUrl"]')?.focus();
    });
  }

  protected removePhoto(): void {
    this.imgUrl = '';
    this.photoSheetOpen = false;
  }
}
