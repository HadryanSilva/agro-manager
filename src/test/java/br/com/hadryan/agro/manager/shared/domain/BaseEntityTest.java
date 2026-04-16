package br.com.hadryan.agro.manager.shared.domain;

import br.com.hadryan.agro.manager.config.JpaConfig;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that any entity extending {@link BaseEntity} automatically gets:
 * <ul>
 *   <li>A generated id after the first save.</li>
 *   <li>{@code createdAt} and {@code updatedAt} populated by JPA auditing.</li>
 *   <li>{@code updatedAt} refreshed on update while {@code createdAt} stays.</li>
 *   <li>Optimistic locking failures when a stale version is written.</li>
 * </ul>
 *
 * <p>Uses a tiny dummy entity declared inside the test package so the test is
 * self-contained and doesn't depend on any feature entity existing yet.
 */
@DataJpaTest
@Import(JpaConfig.class)
@AutoConfigureTestDatabase
@TestPropertySource(properties = {
        // The test's throwaway entity isn't covered by any Flyway migration,
        // so we let Hibernate create its table for this test only.
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class BaseEntityTest {

    @Autowired
    private DummyRepository repository;

    @Test
    void assignsIdAndAuditFieldsOnSave() {
        DummyEntity saved = repository.save(new DummyEntity("first"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
    }

    @Test
    void refreshesUpdatedAtOnUpdateButKeepsCreatedAt() throws InterruptedException {
        DummyEntity saved = repository.saveAndFlush(new DummyEntity("first"));
        var originalCreatedAt = saved.getCreatedAt();
        var originalUpdatedAt = saved.getUpdatedAt();

        // Auditing resolution is millisecond-level on some JVMs; sleep a beat
        // to guarantee the new timestamp is strictly greater.
        Thread.sleep(10);

        saved.setLabel("second");
        DummyEntity updated = repository.saveAndFlush(saved);

        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void failsOnStaleVersion() {
        DummyEntity saved = repository.saveAndFlush(new DummyEntity("first"));

        // Simulate a concurrent edit: someone else already bumped the version.
        DummyEntity stale = repository.findById(saved.getId()).orElseThrow();
        DummyEntity fresh = repository.findById(saved.getId()).orElseThrow();

        fresh.setLabel("winner");
        repository.saveAndFlush(fresh);

        stale.setLabel("loser");
        assertThatThrownBy(() -> repository.saveAndFlush(stale))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Entity
    @Table(name = "dummy_entity")
    static class DummyEntity extends BaseEntity {

        private String label;

        protected DummyEntity() {
            // Required by JPA.
        }

        DummyEntity(String label) {
            this.label = label;
        }

        String getLabel() {
            return label;
        }

        void setLabel(String label) {
            this.label = label;
        }
    }

    interface DummyRepository extends JpaRepository<DummyEntity, Long> {
    }
}