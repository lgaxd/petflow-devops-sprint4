package br.com.petflow.petflow_api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiPageableTest {

    @Test
    void createsPageableForAnAllowedProperty() {
        var pageable = ApiPageable.of(1, 20, "name", "desc", Set.of("name", "createdAt"));

        assertEquals(1, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("name").getDirection());
    }

    @Test
    void rejectsUnknownSortPropertiesAndInvalidPageBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> ApiPageable.of(0, 10, "password", "asc", Set.of("name")));
        assertThrows(IllegalArgumentException.class,
                () -> ApiPageable.of(-1, 10, "name", "asc", Set.of("name")));
    }
}