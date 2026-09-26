package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.DTOS.CriarBloqueioBarbeiro;
import bruninho.Barbeiro.domain.BloqueioBarbeiro;
import bruninho.Barbeiro.repository.BarbeiroRepository;
import bruninho.Barbeiro.repository.BloqueioBarbeiroRepository;
import org.springframework.stereotype.Service;

@Service
public class BloqueioBarbeiroService {

    private BloqueioBarbeiroRepository bloqueioBarbeiroRepository;

    private BarbeiroRepository  barbeiroRepository;

    public BloqueioBarbeiroService(BloqueioBarbeiroRepository bloqueioBarbeiroRepository, BarbeiroRepository barbeiroRepository) {
        this.bloqueioBarbeiroRepository = bloqueioBarbeiroRepository;
        this.barbeiroRepository = barbeiroRepository;
    }


    //retornar nesse metodo e fazer uma verificação se o barbeiro não possui agendamentos nesse dia e horario
    public void criarBloqueioBarbeiro(CriarBloqueioBarbeiro criarBloqueioBarbeiro) {

        BloqueioBarbeiro bloqueioBarbeiro = new BloqueioBarbeiro();
        bloqueioBarbeiro.setBarbeiro(
                barbeiroRepository.findById(criarBloqueioBarbeiro.getBarbeiro_id())
                        .orElseThrow(()-> new RuntimeException("não localizado o barbeiro"))
        );
        bloqueioBarbeiro.setDataBloqueio( criarBloqueioBarbeiro.getDataBloqueio());
        bloqueioBarbeiro.setHoraInicio(criarBloqueioBarbeiro.getHoraInicio());
        bloqueioBarbeiro.setHoraFim(criarBloqueioBarbeiro.getHoraFim());
        bloqueioBarbeiroRepository.save(bloqueioBarbeiro);

    }
}
