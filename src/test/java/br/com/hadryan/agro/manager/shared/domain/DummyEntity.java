package br.com.hadryan.agro.manager.shared.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Throwaway JPA entity used exclusively by {@link BaseEntityTest} to
 * exercise the auditing and optimistic-locking behaviour of {@link
 * BaseEntity} in isolation from any feature entity.
 *
 * <p>Lives in {@code src/test} so it never ships with the application.
 * It's a top-level class (rather than nested inside the test) because
 * Spring Data JPA's repository scanner ignores classes nested inside
 * test classes.
 */
@Entity
@Table(name = "dummy_entity")
public class DummyEntity extends BaseEntity {

    private String label;

    protected DummyEntity() {
        // Required by JPA.
    }

    public DummyEntity(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}