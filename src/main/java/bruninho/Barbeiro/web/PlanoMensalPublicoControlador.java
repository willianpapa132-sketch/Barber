package bruninho.Barbeiro.web;

import bruninho.Barbeiro.repository.ConfiguracaoBarbeariaRepositorio;
import bruninho.Barbeiro.repository.ConfiguracaoPlanoMensalRepositorio;
import bruninho.Barbeiro.service.RegraNegocioException;
import bruninho.Barbeiro.service.PlanoMensalServico;
import bruninho.Barbeiro.web.form.PublicPlanoMensalForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/planos")
public class PlanoMensalPublicoControlador {
    private final ConfiguracaoPlanoMensalRepositorio configuracoes;
    private final ConfiguracaoBarbeariaRepositorio configBarbearia;
    private final PlanoMensalServico planosMensais;

    public PlanoMensalPublicoControlador(ConfiguracaoPlanoMensalRepositorio configuracoes, ConfiguracaoBarbeariaRepositorio configBarbearia,
                                       PlanoMensalServico planosMensais) {
        this.configuracoes = configuracoes;
        this.configBarbearia = configBarbearia;
        this.planosMensais = planosMensais;
    }

    @GetMapping
    String form(@ModelAttribute("form") PublicPlanoMensalForm form, Model model) {
        fill(model);
        return "public/planos";
    }

    @PostMapping
    String contratar(@Valid @ModelAttribute("form") PublicPlanoMensalForm form, BindingResult result,
                     Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            fill(model);
            return "public/planos";
        }
        try {
            var plano = planosMensais.contratar(form);
            redirect.addFlashAttribute("plano", plano);
            return "redirect:/planos/confirmado";
        } catch (RegraNegocioException ex) {
            model.addAttribute("erro", ex.getMessage());
            fill(model);
            return "public/planos";
        }
    }

    @GetMapping("/confirmado")
    String confirmado() {
        return "public/plano-confirmado";
    }

    private void fill(Model model) {
        model.addAttribute("config", configBarbearia.findById(1L).orElseThrow());
        model.addAttribute("planos", configuracoes.buscarPlanosPublicosAtivos());
    }
}
