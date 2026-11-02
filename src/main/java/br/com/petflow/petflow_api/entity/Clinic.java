package br.com.petflow.petflow_api.entity;

import jakarta.persistence.*;
import lombok.*;
 
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
 
@Entity
@Table(name = "CLINIC")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name", "cnpj"})
public class Clinic {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", length = 100, nullable = false)
    private String name;
 
    @Column(name = "ADDRESS", length = 200)
    private String address;
 
    @Column(name = "PHONE", length = 20)
    private String phone;
 
    @Column(name = "CNPJ", length = 18, nullable = false, unique = true)
    private String cnpj;
 
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;
 
    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Plan> plans = new ArrayList<>();
 
    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}