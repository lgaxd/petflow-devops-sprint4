package br.com.petflow.petflow_api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import br.com.petflow.petflow_api.enums.HealthEventStatus;
 
@Entity
@Table(name = "HEALTH_EVENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "status", "eventDate"})
public class HealthEvent {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "DESCRIPTION", length = 200)
    private String description;

    @Column(name = "EVENT_DATE", nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private HealthEventStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EVENT_TYPE_ID", nullable = false)
    private EventType eventType;
 
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PET_ID", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLINIC_ID", foreignKey = @ForeignKey(name = "FK_HE_CLINIC"))
    private Clinic clinic;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = HealthEventStatus.AGENDADO;
        }
    }
}