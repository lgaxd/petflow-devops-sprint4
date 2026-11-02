package br.com.petflow.petflow_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "REWARD_POINT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "points"})
public class RewardPoint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TUTOR_ID", nullable = false)
    private Tutor tutor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REWARD_ACTION_ID", nullable = false)
    private RewardAction rewardAction;
    
    @Column(name = "POINTS", nullable = false)
    private Integer points;
    
    @Column(name = "REFERENCE_TYPE", length = 50)
    private String referenceType;
    
    @Column(name = "REFERENCE_ID")
    private Long referenceId;
    
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}