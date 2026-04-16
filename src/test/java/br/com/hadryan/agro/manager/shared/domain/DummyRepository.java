package br.com.hadryan.agro.manager.shared.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Test-only repository for {@link DummyEntity}. Top-level so that the
 * Spring Data scanner configured by {@code @DataJpaTest} discovers it.
 */
public interface DummyRepository extends JpaRepository<DummyEntity, Long> {
}