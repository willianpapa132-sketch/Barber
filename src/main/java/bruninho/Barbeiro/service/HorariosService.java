package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.configuracao.*;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
public class HorariosService {
    private final HorarioFuncionamentoRepository horarios;
    private final ConfiguracaoBarbeariaRepository configuracoes;
    private final BarbeiroRepository barbeiros;
    private final JornadaBarbeiroRepository jornadas;
    private final AgendamentoRepository agendamentos;
    private final ComissaoBarbeiroRepository comissoes;
    private final ComissaoPlanoRepository pontos;
    public HorariosService(HorarioFuncionamentoRepository horarios, ConfiguracaoBarbeariaRepository configuracoes,
            BarbeiroRepository barbeiros, JornadaBarbeiroRepository jornadas, AgendamentoRepository agendamentos,
            ComissaoBarbeiroRepository comissoes, ComissaoPlanoRepository pontos) {
        this.horarios=horarios; this.configuracoes=configuracoes; this.barbeiros=barbeiros; this.jornadas=jornadas;
        this.agendamentos=agendamentos; this.comissoes=comissoes; this.pontos=pontos;
    }
    @Transactional(readOnly=true)
    public ConfiguracaoResponse consultar() { return resposta(configuracoes.findById(1L).orElseThrow(),0,0); }

    @Transactional
    public ConfiguracaoResponse salvar(SalvarConfiguracaoRequest r) {
        var dias=new EnumMap<DayOfWeek,HorarioFuncionamentoBarber>(DayOfWeek.class);
        for(var d:r.horarios()) {
            if(dias.put(d.diaSemana(),d)!=null) throw erro("Informe cada dia da semana uma unica vez");
            if(!d.fechado() && (d.horarioInicio()==null || d.horarioFim()==null || !d.horarioInicio().isBefore(d.horarioFim())))
                throw erro("Informe abertura anterior ao fechamento em "+d.diaSemana());
        }
        if(dias.size()!=7) throw erro("Informe os sete dias da semana");
        var config=configuracoes.bloquear().orElseThrow();
        // Mesmo bloqueio utilizado na criacao de agendamentos e na edicao das jornadas.
        for(var b:barbeiros.findAll(org.springframework.data.domain.Sort.by("id"))) barbeiros.findByIdParaAgendar(b.getId()).orElseThrow();
        int ajustadas=0;
        for(var j:jornadas.findAll()) {
            var d=dias.get(j.getDiaSemana());
            if(d.fechado()) {
                if(!Boolean.TRUE.equals(j.getFolga())) { j.setFolga(true); ajustadas++; }
            } else if(!Boolean.TRUE.equals(j.getFolga()) && j.getHoraInicio()!=null && j.getHoraFim()!=null) {
                var inicio=j.getHoraInicio().isBefore(d.horarioInicio())?d.horarioInicio():j.getHoraInicio();
                var fim=j.getHoraFim().isAfter(d.horarioFim())?d.horarioFim():j.getHoraFim();
                if(!inicio.isBefore(fim)) { j.setFolga(true); ajustadas++; }
                else if(!inicio.equals(j.getHoraInicio()) || !fim.equals(j.getHoraFim())) {
                    j.setHoraInicio(inicio); j.setHoraFim(fim); ajustadas++;
                    if(j.getIntervaloInicio()!=null && j.getIntervaloFim()!=null) {
                        var pausaInicio=j.getIntervaloInicio().isBefore(inicio)?inicio:j.getIntervaloInicio();
                        var pausaFim=j.getIntervaloFim().isAfter(fim)?fim:j.getIntervaloFim();
                        j.setIntervaloInicio(pausaInicio.isBefore(pausaFim)?pausaInicio:null);
                        j.setIntervaloFim(pausaInicio.isBefore(pausaFim)?pausaFim:null);
                    }
                }
            }
        }
        int cancelados=0;
        for(var a:agendamentos.pendentesParaConfiguracao(LocalDate.now())) {
            var d=dias.get(a.getData().getDayOfWeek());
            String motivo=null;
            if(d.fechado()) motivo="Barbearia fechada neste dia da semana";
            else if(a.getHoraInicio()==null || a.getHoraFinalizacao()==null || a.getHoraInicio().isBefore(d.horarioInicio()) || a.getHoraFinalizacao().isAfter(d.horarioFim()))
                motivo="Atendimento fora do novo horario de funcionamento";
            else if(!d.permiteAgendamentoPlano() && a.getPlanoMensalCliente()!=null)
                motivo="Dia nao permite mais agendamentos de plano";
            if(motivo!=null) {
                if(Boolean.TRUE.equals(a.getPagamentoRecebido()) || comissoes.existsByAgendamento_Id(a.getId()) || pontos.existsByAgendamento_Id(a.getId()))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"O agendamento #"+a.getId()+" possui pagamento ou comissao. Regularize-o antes de alterar o funcionamento.");
                a.setStatus(StatusAgendamento.CANCELADO); a.setMotivoCancelamento("Configuracao: "+motivo);
                a.setAtualizadoEm(LocalDateTime.now()); cancelados++;
            }
        }
        for(var d:r.horarios()) {
            var h=horarios.findByDiaSemana(d.diaSemana());
            if(h==null) { h=new HorarioFuncionamento(); h.setDiaSemana(d.diaSemana()); }
            h.setFechado(d.fechado()); h.setHoraAbertura(d.horarioInicio()); h.setHoraFechamento(d.horarioFim());
            h.setIntervaloMinimoEntreAgendamentos(d.intervaloMinimoEntreAgendamentos());
            h.setPermiteAgendamentoPlano(!d.fechado() && d.permiteAgendamentoPlano()); horarios.save(h);
        }
        config.setNome(r.nome().trim()); config.setTelefone(r.telefone()==null?null:r.telefone().trim());
        config.setEndereco(r.endereco()==null?null:r.endereco().trim()); config.setDiasMaximoAntecedentia(r.diasMaximoAntecedencia());
        return resposta(config,ajustadas,cancelados);
    }
    private ConfiguracaoResponse resposta(ConfiguracaoBarbearia c,int ajustadas,int cancelados) {
        var lista=Arrays.stream(DayOfWeek.values()).map(d->{
            var h=horarios.findByDiaSemana(d);
            return h==null?new HorarioFuncionamentoBarber(d,null,null,true,15,false):new HorarioFuncionamentoBarber(d,
                    h.getHoraAbertura(),h.getHoraFechamento(),Boolean.TRUE.equals(h.getFechado()),
                    h.getIntervaloMinimoEntreAgendamentos(),Boolean.TRUE.equals(h.getPermiteAgendamentoPlano()));
        }).toList();
        return new ConfiguracaoResponse(c.getNome(),c.getTelefone(),c.getEndereco(),c.getDiasMaximoAntecedentia(),lista,ajustadas,cancelados);
    }
    private ResponseStatusException erro(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,m); }
}
