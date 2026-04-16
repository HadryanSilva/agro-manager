package br.com.hadryan.agro.manager.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.hadryan.agro.manager.account.domain.Account;
import br.com.hadryan.agro.manager.account.domain.AccountStatus;
import br.com.hadryan.agro.manager.account.repository.AccountRepository;
import br.com.hadryan.agro.manager.config.JpaConfig;
import br.com.hadryan.agro.manager.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link AccountService} using a real (H2) persistence
 * layer so we catch mapping mistakes early.
 *
 * <p>{@code @ActiveProfiles("test")} activates {@code application-test.yaml},
 * which configures Hibernate to build the schema from the entity model and
 * disables Flyway. This keeps the test independent of migration ordering.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({JpaConfig.class, AccountService.class})
class AccountServiceTest {

    @Autowired
    private AccountService service;

    @Autowired
    private AccountRepository repository;

    @Test
    void create_startsActive() {
        Account created = service.create("Fazenda Santa Luzia");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(created.getName()).isEqualTo("Fazenda Santa Luzia");
    }

    @Test
    void findById_missing_throwsNotFound() {
        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Account with id 999");
    }

    @Test
    void rename_updatesNameOnly() {
        Account created = service.create("Old name");

        Account renamed = service.rename(created.getId(), "New name");

        assertThat(renamed.getName()).isEqualTo("New name");
        assertThat(renamed.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        // Sanity-check the change was persisted.
        assertThat(repository.findById(created.getId()).orElseThrow().getName())
                .isEqualTo("New name");
    }

    @Test
    void suspendThenActivate_roundTrips() {
        Account created = service.create("Some account");

        service.suspend(created.getId());
        assertThat(service.findById(created.getId()).isActive()).isFalse();

        service.activate(created.getId());
        assertThat(service.findById(created.getId()).isActive()).isTrue();
    }
}