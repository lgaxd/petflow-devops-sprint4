package br.com.petflow.petflow_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

/** Tabela de domínio de espécies: somente leitura para a API (mantida por seed/scripts de banco). */
@Entity
@Immutable
@Table(name = "SPECIES")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name"})
public class Species {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;
}
