package br.com.hadryan.agro.manager.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.hadryan.agro.manager.config.JpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that any entity extending {@link BaseEntity} automatically gets:
 * <ul>
 *   <li>A generated id after the first save.</li>
 *   <li>{@code createdAt} and {@code updatedAt} populated by JPA auditing.</li>
 *   <li>{@code updatedAt} refreshed on update while {@code createdAt} stays.</li>
 *   <li>Optimistic locking failures when a stale version is written.</li>
 * </ul>
 *
 * <p>Exercises the behaviour through {@link DummyEntity} — a test-only
 * top-level entity — instead of depending on any feature entity existing.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
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
        // Persist a row so there's something to collide with.
        DummyEntity persisted = repository.saveAndFlush(new DummyEntity("first"));
        Long id = persisted.getId();

        // Build a brand-new, detached entity with the same id but a
        // version number that will never match the DB (9999). When
        // Hibernate generates the UPDATE, the WHERE clause won't match
        // any row and the optimistic-lock check fails.
        //
        // This pattern is more reliable than re-reading the entity twice
        // inside @DataJpaTest, because the single persistence context
        // would hand out the same managed instance for both reads.
        DummyEntity stale = new DummyEntity("loser");
        setField(stale, "id", id);
        setField(stale, "version", 9999L);

        assertThatThrownBy(() -> repository.saveAndFlush(stale))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    /**
     * Writes to a private field via reflection. Used only to forge a
     * "stale" copy of an entity with a controlled version — production
     * code never needs this because Hibernate manages both id and version.
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> type = target.getClass();
            while (type != null) {
                try {
                    var field = type.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    type = type.getSuperclass();
                }
            }
            throw new IllegalStateException("Field not found: " + fieldName);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}