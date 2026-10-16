(() => {
    'use strict';
    const app = document.getElementById('finish-app');
    if (!app) return;
    const byId = (id) => document.getElementById(id);
    const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    const percent = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 });
    const names = { DINHEIRO: 'Dinheiro', PIX: 'Pix', DEBITO: 'Débito', CREDITO: 'Crédito', MENSAL: 'Mensal' };
    const icons = { DINHEIRO: '＄', PIX: '◇', DEBITO: '▣', CREDITO: '▤', MENSAL: '▦' };
    let appointment = null;
    let selected = null;
    let busy = false;
    let completed = false;

    function updateButton() {
        byId('finish-submit').disabled = busy || completed || !appointment?.podeFinalizar || !selected;
        byId('finish-submit').textContent = busy ? 'Aguarde...' : completed ? 'Atendimento finalizado' : 'Finalizar atendimento';
        document.querySelectorAll('[name="formaPagamento"]').forEach((input) => { input.disabled = busy || completed; });
    }

    async function request(url, options = {}) {
        const response = await fetch(url, { credentials: 'same-origin', headers: { Accept: 'application/json', ...options.headers }, ...options });
        if (response.redirected || response.status === 401) {
            window.location.assign('/login');
            throw new Error('Sua sessão expirou. Entre novamente.');
        }
        const data = await response.json().catch(() => null);
        if (!response.ok) throw new Error(data?.mensagem || 'Não foi possível acessar este atendimento. Volte à agenda e tente novamente.');
        if (!data) throw new Error('Não foi possível ler os dados do atendimento. Tente novamente.');
        return data;
    }

    function render(data) {
        appointment = data;
        selected = data.formaPagamento || (data.formasPagamentoDisponiveis.includes(selected) ? selected : null);
        const name = data.nomeCliente || 'Cliente';
        byId('client-name').textContent = name;
        byId('client-initials').textContent = name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase();
        byId('plan-tag').hidden = !data.atendimentoMensal;
        byId('appointment-date').textContent = data.data.split('-').reverse().join('/');
        byId('appointment-hours').textContent = `${data.horaInicio.slice(0, 5)} – ${data.horaTermino.slice(0, 5)} · ${data.duracaoMinutos} min`;
        byId('attendance-title').textContent = completed ? 'Atendimento concluído' : data.podeFinalizar ? 'Atendimento pronto para finalizar' : 'Aguarde o término previsto';
        byId('attendance-description').textContent = completed ? 'Registrado com sucesso' : data.podeFinalizar ? 'Confira o pagamento antes de confirmar' : data.motivoBloqueio;
        byId('attendance-status').textContent = completed ? 'Concluído' : data.podeFinalizar ? 'Pronto' : 'Agendado';
        byId('attendance-status').className = `status ${completed ? 'CONCLUIDO' : 'AGENDADO'}`;
        if (data.comissaoPlano) {
            byId('commission-title').textContent = 'Registro de comissão do plano';
            byId('commission-description').textContent = `${data.comissaoPlano.nomePlano} · ${completed ? '1 atendimento contabilizado.' : '1 atendimento será contabilizado ao finalizar.'} Valor em reais ainda não calculado.`;
            byId('commission-value').textContent = `${percent.format(data.comissaoPlano.percentualAplicado)}%`;
        } else {
            byId('commission-title').textContent = 'Sua comissão';
            byId('commission-description').textContent = `${percent.format(data.comissao.percentualAplicado)}% sobre ${money.format(data.comissao.valorBase)}`;
            byId('commission-value').textContent = money.format(data.comissao.valorComissao);
        }
        byId('charge-value').textContent = money.format(data.valorCobrar);
        byId('charge-description').textContent = data.atendimentoMensal ? 'Atendimento coberto pelo plano mensal' : 'Valor do atendimento';
        const list = byId('service-list');
        list.replaceChildren();
        data.servicos.forEach((service) => {
            const row = document.createElement('li');
            const label = document.createElement('span');
            const value = document.createElement('span');
            label.textContent = service.nome;
            value.textContent = money.format(service.preco);
            row.append(label, value);
            list.append(row);
        });
        if (!data.servicos.length) {
            const row = document.createElement('li');
            row.textContent = 'Nenhum serviço detalhado neste atendimento.';
            list.append(row);
        }
        byId('services-total').textContent = money.format(data.valorTotalServicos);
        const options = byId('payment-options');
        options.replaceChildren();
        data.formasPagamentoDisponiveis.forEach((method) => {
            const label = document.createElement('label');
            label.className = 'payment-choice';
            const input = document.createElement('input');
            input.type = 'radio'; input.name = 'formaPagamento'; input.value = method; input.required = true;
            input.checked = selected === method;
            const tile = document.createElement('span');
            const icon = document.createElement('span');
            icon.className = 'payment-icon'; icon.setAttribute('aria-hidden', 'true'); icon.textContent = icons[method];
            const title = document.createElement('span'); title.textContent = names[method];
            tile.append(icon, title); label.append(input, tile); options.append(label);
            input.addEventListener('change', () => load(method));
        });
        byId('finish-hint').textContent = completed ? '' : !data.podeFinalizar ? data.motivoBloqueio : !selected ? 'Selecione a forma de pagamento para finalizar.' : data.atendimentoMensal ? 'A conclusão registrará um atendimento para este plano.' : 'Ao confirmar, o atendimento será concluído e a comissão registrada.';
        byId('finish-content').hidden = false;
        updateButton();
    }

    function showError(error) {
        byId('finish-error').textContent = error.message || 'Não foi possível concluir. Tente novamente.';
        byId('finish-error').hidden = false;
        byId('finish-retry').hidden = false;
    }

    async function load(method = selected) {
        if (busy || completed) return;
        busy = true;
        byId('finish-error').hidden = true;
        byId('finish-retry').hidden = true;
        updateButton();
        try {
            const url = new URL(app.dataset.apiUrl, window.location.origin);
            if (method) url.searchParams.set('formaPagamento', method);
            render(await request(url));
        } catch (error) {
            // Invalidate the previous preview so a failed recheck cannot enable submission.
            if (appointment) appointment.podeFinalizar = false;
            showError(error);
        } finally {
            busy = false;
            byId('finish-loading').hidden = true;
            updateButton();
        }
    }

    byId('finish-form').addEventListener('submit', async (event) => {
        event.preventDefault();
        if (busy || completed || !appointment?.podeFinalizar || !selected) return;
        busy = true;
        byId('finish-error').hidden = true;
        byId('finish-retry').hidden = true;
        updateButton();
        try {
            const data = await request(app.dataset.apiUrl, {
                method: 'POST', headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
                body: JSON.stringify({ formaPagamento: selected })
            });
            completed = true;
            render(data);
            byId('finish-success').hidden = false;
            byId('finish-success').scrollIntoView({ behavior: 'smooth', block: 'start' });
        } catch (error) {
            appointment.podeFinalizar = false;
            showError(error);
        } finally {
            busy = false;
            updateButton();
        }
    });
    byId('finish-retry').addEventListener('click', () => load(selected));
    function recheck() {
        if (document.visibilityState === 'visible' && appointment && !appointment.podeFinalizar && !completed && !busy) load(selected);
    }
    window.addEventListener('focus', recheck);
    window.setInterval(recheck, 30000);
    load();
})();
