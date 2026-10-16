# Frontend da barbearia

As áreas de cliente e barbeiro usam a mesma aplicação Angular. O Spring Boot fornece as APIs e encaminha as rotas das telas para a aplicação compilada.

- Cliente: `src/app/pages/`.
- Barbeiro: `src/app/barbeiro/` (home, agenda, bloqueios, criação de atendimentos e finalização).
- Rotas: `src/app/app.routes.ts`.
- Estilos do barbeiro: `src/app/barbeiro/_barbeiro.scss`, isolados por `.barber-area` para preservar o tema escuro sem afetar o cliente.

Para compilar, execute nesta pasta:

```powershell
npm ci
npm run build
```

O build grava os arquivos em `../src/main/resources/static/client/`. Não edite os arquivos gerados diretamente. Depois da compilação, execute o Spring Boot e abra `/barbeiro/home` ou `/barbeiro/agenda` com uma conta de barbeiro. Acesso direto e atualização das páginas são tratados pelo backend.

As APIs continuam em `/api/barbeiro/**`, com autenticação por sessão e autorização no Spring Security. A tela compartilhada de login permanece no backend em `templates/login.html`.