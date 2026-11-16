package bruninho.Barbeiro.domain;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
@Entity @Table(name="comanda") @Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name="empresa",condition="{e}.empresa_id = :empresaId",deduceAliasInjectionPoints=false,aliases=@org.hibernate.annotations.SqlFragmentAlias(alias="e",table="comanda"))
public class Comanda extends bruninho.Barbeiro.platform.EmpresaScopedEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(optional=false) @JoinColumn(nullable=false,unique=true) private Agendamento agendamento;
    @ManyToOne(optional=false) @JoinColumn(nullable=false) private Barbeiro barbeiro;
    @Column(nullable=false) private String nomeCliente;
    @Column(nullable=false) private LocalDateTime fechadoEm;
    @Column(nullable=false) private String fechadoPor;
    @Column(nullable=false) private boolean legado;
    @Column(length=500) private String observacao;
    @Column(nullable=false) private String tipoDesconto;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal valorDescontoInformado;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal subtotal;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal desconto;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal total;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal totalComissao;
    @Column(nullable=false) private int pontos;
    @Column(nullable=false) private int minutos;
    @ElementCollection @CollectionTable(name="comanda_item",joinColumns=@JoinColumn(name="comanda_id")) @OrderColumn(name="ordem")
    private List<ItemComanda> itens=new ArrayList<>();
    @ElementCollection @CollectionTable(name="comanda_pagamento",joinColumns=@JoinColumn(name="comanda_id")) @OrderColumn(name="ordem")
    private List<PagamentoComanda> pagamentos=new ArrayList<>();
}
