package br.com.hadryan.agro.manager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing so {@code @CreatedDate} and
 * {@code @LastModifiedDate} fields on {@link
 * br.com.hadryan.agro.manager.shared.domain.BaseEntity} are populated
 * automatically.
 *
 * <p>When user auditing ({@code @CreatedBy} / {@code @LastModifiedBy}) is
 * needed later, register an {@code AuditorAware<Long>} bean here that
 * returns the authenticated user's id from the security context.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}