package bruninho.Barbeiro.service;

import org.springframework.stereotype.Component;

@Component
public class NormalizadorTelefone {
    public String normalize(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }

    public void validate(String phone) {
        String digits = normalize(phone);
        if (digits.length() < 10 || digits.length() > 11) {
            throw new RegraNegocioException("Informe um telefone brasileiro com DDD.");
        }
    }
}
