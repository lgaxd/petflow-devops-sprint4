package br.com.petflow.petflow_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PARTNER_DISCOUNT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "partnerName"})
public class PartnerDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLINIC_ID", nullable = false)
    private Clinic clinic;

    @Column(name = "PARTNER_NAME", length = 100, nullable = false)
    private String partnerName;

    @Column(name = "CATEGORY", length = 50)
    private String category;

    @Column(name = "DISCOUNT_PERCENT")
    private Double discountPercent;

    @OneToMany(mappedBy = "partnerDiscount", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CouponTemplate> templates = new ArrayList<>();
}