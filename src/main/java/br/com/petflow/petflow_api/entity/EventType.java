package br.com.petflow.petflow_api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "EVENT_TYPE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name"})
public class EventType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "POINTS_REWARD", nullable = false)
    private Integer pointsReward;

    @Column(name = "CATEGORY", nullable = false, length = 50)
    private String category;
}
