(() => {
    'use strict';
    const app = document.getElementById('agenda-app');
    if (!app) return;
    const el = (id) => document.getElementById(id);
    const dialog = el('agenda-dialog');
    const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    const statuses = { AGENDADO: 'Agendado', CONCLUIDO: 'Concluído', CANCELADO: 'Cancelado', NAO_COMPARECEU: 'Não compareceu' };
    let data = null, services = [], selectedTime = null, saving = false, loading = false;
    let searchController = null, searchTimer = null;
    let gradeController = null, loadVersion = 0;
    let servicesController = null, servicesVersion = 0, servicesLoading = false, activePlan = null;
    const minutes = (time) => {
        if (!time) return null;
        const parts = time.split(':').map(Number);
        return parts[0] * 60 + parts[1] + (parts[2] || 0) / 60;
    };
    const hhmm = (time) => time ? time.slice(0, 5) : '';
    const dateLabel = (date) => date.split('-').reverse().join('/');
    function node(tag, className, text) {
        const result = document.createElement(tag);
        if (className) result.className = className;
        if (text != null) result.textContent = text;
        return result;
    }
    async function request(path, options = {}) {
        const response = await fetch(path, { credentials: 'same-origin', ...options,
            headers: { Accept: 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers } });
        if (response.redirected || response.status === 401) {
            window.location.assign('/login'); throw new Error('Sua sessão expirou. Entre novamente.');
        }
        if (response.status === 204) return null;
        const body = await response.json().catch(() => null);
        if (!response.ok) throw new Error(body?.mensagem || 'Não foi possível concluir esta ação. Tente novamente.');
        if (!body) throw new Error('Resposta inválida ao consultar a agenda.');
        return body;
    }
    function notice(message, error = false) {
        el('agenda-message').textContent = message || '';
        el('agenda-message').className = `finish-notice ${error ? 'notice-error' : ''}`;
        el('agenda-message').hidden = !message;
    }
    function dialogError(error) {
        el('agenda-dialog-error').textContent = error.message || String(error);
        el('agenda-dialog-error').hidden = false;
    }
    function appointmentNode(item) {
        const active = item.status === 'AGENDADO';
        const card = node(active ? 'a' : 'article', `timeline-event ${item.status}`);
        if (active) card.href = `/barbeiro/agendamentos/${item.id}/finalizacao`;
        card.title = `${hhmm(item.horaInicio)}–${hhmm(item.horaFinalizacao)} · ${item.nomeCliente || 'Cliente'} · ${item.servicos.join(' + ')}`;
        const text = node('div', 'event-description');
        text.append(node('time', '', `${hhmm(item.horaInicio)} – ${hhmm(item.horaFinalizacao)} (${item.duracaoMinutos} min)`),
            node('strong', '', item.nomeCliente || 'Cliente'), node('small', '', item.servicos.join(' + ')));
        card.append(text, node('span', `status ${item.status}`, statuses[item.status] || item.status));
        if (active) card.append(node('span', 'event-chevron', '›'));
        return card;
    }
    function periodNode(period) {
        const card = node('article', `timeline-event is-period ${period.tipo}`);
        const text = node('div', 'event-description');
        text.append(node('time', '', `${hhmm(period.inicio || data.inicio)} – ${hhmm(period.fim || data.fim)}`),
            node('strong', '', period.descricao));
        card.append(text);
        if (period.tipo === 'BLOQUEIO' && period.id) {
            const remove = node('button', 'remove-block', 'Remover'); remove.type = 'button';
            remove.setAttribute('aria-label', `Remover bloqueio das ${hhmm(period.inicio || data.inicio)}`);
            remove.addEventListener('click', () => removeBlock(period.id, remove)); card.append(remove);
        }
        return card;
    }
    function render() {
        const host = el('agenda-timeline'), outside = el('agenda-outside');
        host.replaceChildren(); outside.replaceChildren(); outside.hidden = true;
        el('agenda-fallback').hidden = true; host.hidden = false;
        el('agenda-date-title').textContent = dateLabel(data.data);
        el('agenda-count').textContent = `${data.agendamentos.length} atendimento(s)`;
        el('agenda-grid-label').textContent = data.intervaloGrade
            ? `◷ Grade configurada: ${data.intervaloGrade} min · Jornada ${hhmm(data.inicio)} – ${hhmm(data.fim)}` : data.aviso;
        el('agenda-add').hidden = false;
        el('agenda-add').disabled = !data.podeCriar;
        el('agenda-add').title = data.podeCriar ? 'Criar atendimento ou bloqueio' : data.aviso || 'Não é possível criar horários neste dia';
        if (!data.horarios.length) {
            host.append(node('p', 'finish-notice', data.aviso || 'Sem jornada para este dia.'));
            if (data.agendamentos.length) {
                outside.hidden = false; outside.append(node('h2', '', 'Atendimentos cadastrados'));
                data.agendamentos.forEach((item) => { const card = appointmentNode(item); card.classList.add('outside-event'); outside.append(card); });
            }
            return;
        }
        const start = minutes(data.inicio), end = minutes(data.fim);
        const events = [];
        data.agendamentos.forEach((item) => {
            const s = minutes(item.horaInicio), e = minutes(item.horaFinalizacao);
            if (s == null || e == null || s < start || e > end || e <= s) {
                if (outside.hidden) { outside.hidden = false; outside.append(node('h2', '', 'Atendimentos fora da jornada')); }
                const card = appointmentNode(item); card.classList.add('outside-event'); outside.append(card); return;
            }
            events.push({ start: s, end: e, card: appointmentNode(item) });
        });
        data.indisponibilidades.filter((period) => period.tipo !== 'OCUPADO').forEach((period) => {
            const s = Math.max(start, minutes(period.inicio) ?? start), e = Math.min(end, minutes(period.fim) ?? end);
            if (e > s) events.push({ start: s, end: e, card: periodNode(period) });
        });
        const shortest = Math.min(15, ...events.map((event) => event.end - event.start));
        const scale = Math.max(5, 58 / Math.max(shortest, 1));
        const grid = node('div', 'day-timeline'); grid.style.height = `${(end - start) * scale}px`;
        data.horarios.forEach((time, index) => {
            const top = (minutes(time) - start) * scale;
            const next = index + 1 < data.horarios.length ? minutes(data.horarios[index + 1]) : end;
            const label = node('div', 'timeline-time', hhmm(time));
            label.style.top = `${top}px`; label.style.height = `${(next - minutes(time)) * scale}px`;
            const line = node('div', 'timeline-line'); line.style.top = `${top}px`;
            grid.append(label, line);
        });
        const layer = node('div', 'timeline-events');
        events.sort((a, b) => a.start - b.start || b.end - a.end);
        let group = [], groupEnd = -1;
        function positionGroup() {
            const laneEnds = [];
            group.forEach((event) => {
                let lane = laneEnds.findIndex((value) => value <= event.start);
                if (lane < 0) lane = laneEnds.length;
                laneEnds[lane] = event.end; event.lane = lane;
            });
            group.forEach((event) => {
                const card = event.card;
                card.style.top = `${(event.start - start) * scale}px`;
                card.style.height = `${(event.end - event.start) * scale - 4}px`;
                card.style.left = `calc(${event.lane * 100 / laneEnds.length}% + 2px)`;
                card.style.width = `calc(${100 / laneEnds.length}% - 4px)`;
                if (event.end - event.start <= 15) card.classList.add('compact');
                layer.append(card);
            });
        }
        events.forEach((event) => {
            if (group.length && event.start >= groupEnd) { positionGroup(); group = []; groupEnd = -1; }
            group.push(event); groupEnd = Math.max(groupEnd, event.end);
        });
        if (group.length) positionGroup();
        grid.append(layer); host.append(grid, node('div', 'timeline-end', `${hhmm(data.fim)} · Fim da jornada`));
        if (!data.agendamentos.length) notice('Nenhum agendamento encontrado para este filtro. Os horários da jornada continuam visíveis.');
        else if (data.aviso) notice(data.aviso);
    }
    async function load() {
        if (saving) return;
        const version = ++loadVersion;
        gradeController?.abort(); gradeController = new AbortController();
        loading = true; el('agenda-retry').hidden = true; el('agenda-add').disabled = true;
        el('agenda-timeline').setAttribute('aria-busy', 'true');
        try {
            const form = el('agenda-filters');
            const query = new URLSearchParams(new FormData(form));
            const result = await request(`/api/barbeiro/agenda/grade?${query}`, { signal: gradeController.signal });
            if (version !== loadVersion) return;
            data = result;
            notice(null); render();
            window.history.replaceState(null, '', `/barbeiro/agenda?${query}`);
        } catch (error) {
            if (error.name !== 'AbortError' && version === loadVersion) {
                notice(error.message, true); el('agenda-retry').hidden = false;
            }
        } finally {
            if (version === loadVersion) { loading = false; el('agenda-timeline').removeAttribute('aria-busy'); }
        }
    }
    function canFit(time, duration) {
        if (!data?.podeCriar || !time) return false;
        const start = minutes(time), end = start + duration;
        if (start < minutes(data.inicio) || end > minutes(data.fim)) return false;
        if (new Date(`${data.data}T${hhmm(time)}:00`) <= new Date()) return false;
        return !data.indisponibilidades.some((period) => {
            const s = minutes(period.inicio) ?? minutes(data.inicio), e = minutes(period.fim) ?? minutes(data.fim);
            return start < e && end > s;
        });
    }
    function options(select, values, preferred, duration = 1) {
        select.replaceChildren();
        values.forEach((time) => {
            const option = node('option', '', hhmm(time)); option.value = hhmm(time);
            option.disabled = !canFit(time, duration); select.append(option);
        });
        const available = [...select.options].filter((option) => !option.disabled);
        select.value = available.some((option) => option.value === preferred) ? preferred : available[0]?.value || '';
    }
    function selectedServices() {
        const ids = [...el('agenda-services').querySelectorAll('input:checked')].map((input) => Number(input.value));
        return services.filter((service) => ids.includes(service.id));
    }
    function serviceSummary() {
        const list = selectedServices(), duration = list.reduce((sum, service) => sum + service.duracaoMinutos, 0);
        const total = list.reduce((sum, service) => sum + Number(service.preco), 0);
        const select = el('agenda-appointment-time'), previous = select.value || selectedTime;
        options(select, data.horarios, previous, duration || 1);
        el('agenda-appointment-summary').textContent = list.length
            ? `${duration} min · ${activePlan ? 'Incluso no plano mensal' : `Serviços avulsos: ${money.format(total)}`}${select.value ? '' : ' · Sem horário livre para esta duração.'}` : 'Selecione pelo menos um serviço.';
        updateCreateButton();
    }
    function updateCreateButton() {
        const needsClient = el('agenda-customer-mode').value === 'registered' && !el('agenda-client').value;
        el('agenda-save-appointment').disabled = saving || servicesLoading || needsClient
            || !selectedServices().length || !el('agenda-appointment-time').value;
    }
    async function loadServices() {
        const version = ++servicesVersion;
        servicesController?.abort(); servicesController = new AbortController();
        services = []; activePlan = null; servicesLoading = true;
        el('agenda-services').replaceChildren(node('p', 'subtitle', 'Carregando serviços...'));
        el('agenda-dialog-error').hidden = true;
        serviceSummary();
        const registered = el('agenda-customer-mode').value === 'registered';
        const clientId = el('agenda-client').value;
        if (registered && !clientId) {
            el('agenda-services').replaceChildren(node('p', 'subtitle', 'Selecione um cliente para consultar os serviços.'));
            el('agenda-plan-hint').textContent = 'Os serviços serão atualizados conforme o plano do cliente selecionado.';
            servicesLoading = false; updateCreateButton(); return;
        }
        try {
            const url = registered ? `/api/barbeiro/agenda/clientes/${encodeURIComponent(clientId)}/servicos` : '/api/barbeiro/agenda/servicos';
            const result = await request(url, { signal: servicesController.signal });
            if (version !== servicesVersion) return;
            services = registered ? result.servicos : result;
            activePlan = registered && result.possuiPlano ? result.nomePlano || 'Plano mensal' : null;
            const picker = el('agenda-services'); picker.replaceChildren();
            services.forEach((service) => {
                const label = node('label'); const input = node('input'); input.type = 'checkbox'; input.value = service.id;
                label.append(input, node('span', '', `${service.nome} · ${service.duracaoMinutos} min · ${activePlan ? 'Incluso no plano' : money.format(service.preco)}`));
                input.addEventListener('change', serviceSummary); picker.append(label);
            });
            if (!services.length) picker.append(node('p', 'subtitle', activePlan ? 'Nenhum serviço ativo disponível neste plano.' : 'Nenhum serviço ativo cadastrado.'));
            el('agenda-plan-hint').textContent = activePlan
                ? `${activePlan}: apenas os serviços incluídos no plano estão disponíveis.`
                : 'Atendimento avulso: selecione os serviços desejados.';
            serviceSummary();
        } catch (error) {
            if (error.name !== 'AbortError' && version === servicesVersion) {
                el('agenda-services').replaceChildren(node('p', 'subtitle', 'Não foi possível carregar os serviços. Selecione novamente o cliente.'));
                el('agenda-plan-hint').textContent = '';
                dialogError(error);
            }
        } finally {
            if (version === servicesVersion) { servicesLoading = false; updateCreateButton(); }
        }
    }
    function blockEndOptions() {
        const start = el('agenda-block-start').value, end = el('agenda-block-end'), previous = end.value;
        end.replaceChildren();
        [...data.horarios, data.fim].filter((time) => minutes(time) > minutes(start)).forEach((time) => {
            const option = node('option', '', hhmm(time)); option.value = hhmm(time);
            option.disabled = !canFit(start, minutes(time) - minutes(start)); end.append(option);
        });
        const available = [...end.options].filter((option) => !option.disabled);
        end.value = available.some((option) => option.value === previous) ? previous : available[0]?.value || '';
    }
    function customerMode() {
        const walkin = el('agenda-customer-mode').value === 'walkin';
        el('agenda-walkin-fields').hidden = !walkin; el('agenda-registered-fields').hidden = walkin;
        el('agenda-client').required = !walkin;
        el('agenda-client-name').required = walkin; el('agenda-client-phone').required = walkin;
    }
    async function searchClients() {
        searchController?.abort(); searchController = new AbortController();
        const select = el('agenda-client');
        try {
            const results = await request(`/api/barbeiro/agenda/clientes?busca=${encodeURIComponent(el('agenda-client-search').value)}`, { signal: searchController.signal });
            const previous = select.value;
            select.replaceChildren(node('option', '', results.length ? 'Selecione um cliente' : 'Nenhum cliente encontrado'));
            select.options[0].value = '';
            results.forEach((client) => { const option = node('option', '', `${client.nome} · ${client.telefone}`); option.value = client.id; select.append(option); });
            select.value = [...select.options].some((option) => option.value === previous) ? previous : '';
            if (select.value !== previous && el('agenda-customer-mode').value === 'registered') loadServices();
        } catch (error) { if (error.name !== 'AbortError') dialogError(error); }
    }
    function menu() {
        if (saving) return;
        el('agenda-action-menu').hidden = false; el('agenda-appointment-form').hidden = true; el('agenda-block-form').hidden = true;
        el('agenda-dialog-error').hidden = true; el('agenda-dialog-title').textContent = 'Novo';
        el('agenda-dialog-subtitle').textContent = 'Escolha uma ação';
    }
    async function editor(kind) {
        el('agenda-action-menu').hidden = true; el('agenda-dialog-error').hidden = true;
        el('agenda-appointment-form').hidden = kind !== 'appointment'; el('agenda-block-form').hidden = kind !== 'block';
        el('agenda-dialog-title').textContent = kind === 'block' ? 'Criar bloqueio' : 'Criar agendamento';
        el('agenda-dialog-subtitle').textContent = 'Reserve um horário na sua agenda';
        dialog.querySelectorAll('.dialog-date').forEach((label) => { label.textContent = `Data: ${dateLabel(data.data)}`; });
        if (kind === 'block') {
            options(el('agenda-block-start'), data.horarios, selectedTime); blockEndOptions();
        } else {
            try {
                customerMode(); await loadServices(); await searchClients();
            } catch (error) { dialogError(error); }
        }
    }
    async function save(kind) {
        if (saving) return;
        const walkin = el('agenda-customer-mode').value === 'walkin';
        let payload;
        if (kind === 'block') {
            payload = { data: data.data, horaInicio: el('agenda-block-start').value, horaFim: el('agenda-block-end').value };
            if (!payload.horaInicio || !payload.horaFim) { dialogError(new Error('Selecione um período livre.')); return; }
        } else {
            if (servicesLoading) return;
            const list = selectedServices();
            if (!list.length) { dialogError(new Error('Selecione pelo menos um serviço.')); return; }
            payload = { data: data.data, horaInicio: el('agenda-appointment-time').value, servicosIds: list.map((service) => service.id) };
            if (!payload.horaInicio) { dialogError(new Error('Sem horário livre para estes serviços.')); return; }
            if (walkin) { payload.nomeCliente = el('agenda-client-name').value.trim(); payload.telefoneCliente = el('agenda-client-phone').value.trim(); }
            else payload.clienteId = Number(el('agenda-client').value);
        }
        saving = true; el('agenda-dialog-error').hidden = true;
        dialog.querySelectorAll('button, input, select').forEach((control) => { control.disabled = true; });
        try {
            await request(`/api/barbeiro/agenda/${kind === 'block' ? 'bloqueios' : 'agendamentos'}`, { method: 'POST', body: JSON.stringify(payload) });
            dialog.close(); saving = false; await load();
            notice(kind === 'block' ? 'Bloqueio criado com sucesso.' : 'Agendamento criado com sucesso. Confira a data e o filtro selecionados.');
        } catch (error) { dialogError(error); }
        finally {
            saving = false; dialog.querySelectorAll('button, input, select').forEach((control) => { control.disabled = false; });
            updateCreateButton();
        }
    }
    async function removeBlock(id, button) {
        if (!window.confirm('Remover este bloqueio e liberar o período?')) return;
        button.disabled = true;
        try { await request(`/api/barbeiro/agenda/bloqueios/${id}`, { method: 'DELETE' }); await load(); notice('Bloqueio removido.'); }
        catch (error) { notice(error.message, true); button.disabled = false; }
    }
    el('agenda-filters').addEventListener('submit', (event) => { event.preventDefault(); load(); });
    el('agenda-retry').addEventListener('click', load);
    el('agenda-add').addEventListener('click', () => { if (data?.podeCriar) { selectedTime = null; menu(); dialog.showModal(); } });
    el('agenda-dialog-close').addEventListener('click', () => { if (!saving) dialog.close(); });
    dialog.addEventListener('cancel', (event) => { if (saving) event.preventDefault(); });
    el('choose-block').addEventListener('click', () => editor('block'));
    el('choose-appointment').addEventListener('click', () => editor('appointment'));
    dialog.querySelectorAll('.dialog-back').forEach((button) => button.addEventListener('click', menu));
    el('agenda-customer-mode').addEventListener('change', () => { customerMode(); loadServices(); });
    el('agenda-client').addEventListener('change', loadServices);
    el('agenda-appointment-time').addEventListener('change', updateCreateButton);
    el('agenda-client-search').addEventListener('input', () => {
        el('agenda-client').value = '';
        if (el('agenda-customer-mode').value === 'registered') loadServices();
        clearTimeout(searchTimer); searchTimer = setTimeout(searchClients, 300);
    });
    el('agenda-block-start').addEventListener('change', blockEndOptions);
    el('agenda-appointment-form').addEventListener('submit', (event) => { event.preventDefault(); save('appointment'); });
    el('agenda-block-form').addEventListener('submit', (event) => { event.preventDefault(); save('block'); });
    window.addEventListener('focus', () => { if (!dialog.open) load(); });
    load();
})();
