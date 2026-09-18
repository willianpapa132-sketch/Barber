package bruninho.Barber.service;

import org.springframework.stereotype.Component;

@Component
public class PhoneNormalizer {
    public String normalize(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }

    public void validate(String phone) {
        String digits = normalize(phone);
        if (digits.length() < 10 || digits.length() > 11) {
            throw new BusinessException("Informe um telefone brasileiro com DDD.");
        }
    }
}
