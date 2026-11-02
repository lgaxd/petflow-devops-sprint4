package br.com.petflow.petflow_api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "REWARD_ACTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name"})
public class RewardAction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @Column(name = "NAME", length = 100, nullable = false, unique = true)
    private String name;
    
    @Column(name = "POINTS_VALUE", nullable = false)
    private Integer pointsValue;
    
    @Column(name = "DESCRIPTION", length = 255)
    private String description;
}