package br.com.petflow.petflow_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "COUPON_TEMPLATE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "title"})
public class CouponTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARTNER_DISCOUNT_ID", nullable = false)
    private PartnerDiscount partnerDiscount;
    
    @Column(name = "TITLE", length = 100, nullable = false)
    private String title;
    
    @Column(name = "DISCOUNT_VALUE", nullable = false)
    private Double discountValue;
    
    @Column(name = "DISCOUNT_TYPE", length = 20, nullable = false)
    private String discountType;
    
    @Column(name = "POINTS_REQUIRED", nullable = false)
    private Integer pointsRequired;
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Coupon> coupons = new ArrayList<>();
}