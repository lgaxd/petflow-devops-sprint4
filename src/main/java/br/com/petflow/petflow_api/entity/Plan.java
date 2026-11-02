package br.com.petflow.petflow_api.entity;

import jakarta.persistence.*;
import lombok.*;
 
import java.math.BigDecimal;
 
@Entity
@Table(name = "PLAN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name", "price"})
public class Plan {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
 
    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", length = 200)
    private String description;
 
    @Column(name = "PRICE", nullable = false)
    private BigDecimal price;

    @Column(name = "DURATION_DAYS", nullable = false)
    private Integer durationDays;

    @Column(name = "POINTS_PER_EVENT", nullable = false)
    private Integer pointsPerEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLINIC_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_PLAN_CLINIC"))
    private Clinic clinic;
}