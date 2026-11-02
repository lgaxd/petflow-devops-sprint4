package br.com.petflow.petflow_api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import br.com.petflow.petflow_api.enums.CouponStatus;

@Entity
@Table(name = "COUPON")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "code", "status"})
public class Coupon {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEMPLATE_ID", nullable = false)
    private CouponTemplate template;
 
    @Column(name = "CODE", length = 50, nullable = false, unique = true)
    private String code;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private CouponStatus status;

    @Column(name = "EXPIRATION_DATE")
    private LocalDate expirationDate;
 
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = CouponStatus.DISPONIVEL;
        }
    }
}