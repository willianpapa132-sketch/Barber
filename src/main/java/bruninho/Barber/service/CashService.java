package bruninho.Barber.service;

import bruninho.Barber.domain.*;
import bruninho.Barber.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashService {
    private final CashSessionRepository sessions;
    private final CashMovementRepository movements;
    private final AppointmentRepository appointments;
    private final CurrentUserService currentUser;

    public CashService(CashSessionRepository sessions, CashMovementRepository movements,
                       AppointmentRepository appointments, CurrentUserService currentUser) {
        this.sessions = sessions;
        this.movements = movements;
        this.appointments = appointments;
        this.currentUser = currentUser;
    }

    @Transactional
    public CashSession open(BigDecimal initialCash) {
        if (sessions.findByStatus(CashSessionStatus.ABERTO).isPresent()) throw new BusinessException("Já existe caixa aberto.");
        CashSession session = new CashSession();
        session.setInitialCash(initialCash);
        session.setExpectedCash(initialCash);
        session.setOpenedByUser(currentUser.requiredUser());
        return sessions.save(session);
    }

    @Transactional
    public CashMovement receipt(Long appointmentId, PaymentMethod method) {
        CashSession session = sessions.lockOpenSession().orElseThrow(() -> new BusinessException("Abra o caixa antes de registrar recebimentos."));
        Appointment appt = appointments.findById(appointmentId).orElseThrow();
        if (appt.getStatus() != AppointmentStatus.CONCLUIDO) throw new BusinessException("Somente atendimento concluído pode ser recebido.");
        if (movements.findByAppointmentIdAndTypeAndReversedFalse(appointmentId, CashMovementType.RECEBIMENTO_SERVICO).isPresent()) {
            throw new BusinessException("Este atendimento já possui recebimento ativo.");
        }
        CashMovement m = base(session, CashMovementType.RECEBIMENTO_SERVICO, appt.getServicePriceSnapshot(), "Recebimento de atendimento");
        m.setAppointment(appt);
        m.setPaymentMethod(method);
        appt.setPaymentReceived(true);
        appointments.save(appt);
        return movements.save(m);
    }

    @Transactional
    public CashMovement manual(CashMovementType type, BigDecimal amount, String description, String category) {
        CashSession session = sessions.lockOpenSession().orElseThrow(() -> new BusinessException("Caixa fechado não aceita movimentação."));
        if (type == CashMovementType.RECEBIMENTO_SERVICO || type == CashMovementType.ESTORNO) throw new BusinessException("Tipo inválido para lançamento manual.");
        CashMovement m = base(session, type, amount, description);
        m.setCategory(category);
        return movements.save(m);
    }

    @Transactional
    public CashMovement reverse(Long movementId, String reason) {
        CashSession session = sessions.lockOpenSession().orElseThrow(() -> new BusinessException("Abra o caixa para lançar estorno."));
        CashMovement original = movements.findById(movementId).orElseThrow();
        if (original.isReversed()) throw new BusinessException("Lançamento já estornado.");
        original.setReversed(true);
        CashMovement reversal = base(session, CashMovementType.ESTORNO, original.getAmount().negate(), "Estorno: " + original.getDescription());
        reversal.setOriginalMovement(original);
        reversal.setAppointment(original.getAppointment());
        reversal.setPaymentMethod(original.getPaymentMethod());
        reversal.setReversalReason(reason);
        if (original.getAppointment() != null && original.getType() == CashMovementType.RECEBIMENTO_SERVICO) {
            original.getAppointment().setPaymentReceived(false);
        }
        return movements.save(reversal);
    }

    @Transactional
    public CashSession close(BigDecimal countedCash) {
        CashSession session = sessions.lockOpenSession().orElseThrow(() -> new BusinessException("Não há caixa aberto."));
        BigDecimal expected = expectedCash(session.getId(), session.getInitialCash());
        session.setExpectedCash(expected);
        session.setCountedCash(countedCash);
        session.setDifferenceCash(countedCash.subtract(expected));
        session.setClosedAt(LocalDateTime.now());
        session.setClosedByUser(currentUser.requiredUser());
        session.setStatus(CashSessionStatus.FECHADO);
        return session;
    }

    public BigDecimal expectedCash(Long sessionId, BigDecimal initialCash) {
        BigDecimal total = initialCash;
        List<CashMovement> list = movements.findByCashSessionIdOrderByCreatedAt(sessionId);
        for (CashMovement m : list) {
            if (m.isReversed()) continue;
            if (m.getType() == CashMovementType.RECEBIMENTO_SERVICO && m.getPaymentMethod() == PaymentMethod.DINHEIRO) total = total.add(m.getAmount());
            if (m.getType() == CashMovementType.SUPRIMENTO) total = total.add(m.getAmount());
            if (m.getType() == CashMovementType.SANGRIA || m.getType() == CashMovementType.SAIDA_MANUAL) total = total.subtract(m.getAmount());
            if (m.getType() == CashMovementType.ENTRADA_MANUAL) total = total.add(m.getAmount());
            if (m.getType() == CashMovementType.ESTORNO && m.getPaymentMethod() == PaymentMethod.DINHEIRO) total = total.add(m.getAmount());
        }
        return total;
    }

    private CashMovement base(CashSession session, CashMovementType type, BigDecimal amount, String description) {
        CashMovement m = new CashMovement();
        m.setCashSession(session);
        m.setType(type);
        m.setAmount(amount);
        m.setDescription(description);
        m.setCreatedByUser(currentUser.requiredUser());
        return m;
    }
}
