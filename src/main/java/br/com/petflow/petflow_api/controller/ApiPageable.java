package br.com.petflow.petflow_api.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

final class ApiPageable {

    private ApiPageable() {}

    static Pageable of(int page, int size, String sortBy, String direction, Set<String> allowedProperties) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page deve ser maior ou igual a 0 e size deve ser maior que 0.");
        }
        if (!allowedProperties.contains(sortBy)) {
            throw new IllegalArgumentException("Campo de ordenação inválido: " + sortBy);
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Direção inválida. Use 'asc' ou 'desc'.", ex);
        }
        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }
}