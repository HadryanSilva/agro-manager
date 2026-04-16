package br.com.hadryan.agro.manager.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.hadryan.agro.manager.account.service.AccountService;
import br.com.hadryan.agro.manager.config.JpaConfig;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ConflictException;
import br.com.hadryan.agro.manager.user.domain.User;
import br.com.hadryan.agro.manager.user.domain.UserRole;
import br.com.hadryan.agro.manager.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaConfig.class, AccountService.class, UserService.class, BCryptPasswordHasher.class})
class UserServiceTest {

    @Autowired private UserService userService;
    @Autowired private AccountService accountService;

    private Long accountId;

    @BeforeEach
    void setup() {
        accountId = accountService.create("Test Farm").getId();
    }

    @Test
    void createWithPassword_forcesFirstUserToBeOwner() {
        // Caller asked for MEMBER but since the account has no users yet,
        // the service promotes them to OWNER.
        User created = userService.createWithPassword(
                accountId, "alice@example.com", "Alice", "password123", UserRole.MEMBER);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getRole()).isEqualTo(UserRole.OWNER);
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.getPasswordHash()).isNotNull().isNotEqualTo("password123");
        assertThat(created.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void createWithPassword_normalizesEmailToLowercase() {
        User created = userService.createWithPassword(
                accountId, "BOB@Example.COM", "Bob", "password123", UserRole.OWNER);

        assertThat(created.getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    void createWithPassword_rejectsDuplicateEmail() {
        userService.createWithPassword(
                accountId, "alice@example.com", "Alice", "password123", UserRole.OWNER);

        assertThatThrownBy(() -> userService.createWithPassword(
                accountId, "alice@example.com", "Alice 2", "password456", UserRole.MEMBER))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createWithPassword_rejectsSecondOwner() {
        userService.createWithPassword(
                accountId, "alice@example.com", "Alice", "password123", UserRole.OWNER);

        assertThatThrownBy(() -> userService.createWithPassword(
                accountId, "bob@example.com", "Bob", "password456", UserRole.OWNER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already has an owner");
    }

    @Test
    void changeRole_cannotDemoteSoleOwner() {
        User owner = userService.createWithPassword(
                accountId, "alice@example.com", "Alice", "password123", UserRole.OWNER);

        assertThatThrownBy(() -> userService.changeRole(owner.getId(), UserRole.MEMBER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sole owner");
    }

    @Test
    void changeRole_cannotPromoteSecondOwner() {
        userService.createWithPassword(
                accountId, "alice@example.com", "Alice", "password123", UserRole.OWNER);
        User bob = userService.createWithPassword(
                accountId, "bob@example.com", "Bob", "password456", UserRole.MEMBER);

        assertThatThrownBy(() -> userService.changeRole(bob.getId(), UserRole.OWNER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already has an owner");
    }

    @Test
    void changeRole_allowsMemberToAdmin() {
        userService.createWithPassword(
                accountId, "alice@example.com", "Alice", "password123", UserRole.OWNER);
        User bob = userService.createWithPassword(
                accountId, "bob@example.com", "Bob", "password456", UserRole.MEMBER);

        User promoted = userService.changeRole(bob.getId(), UserRole.ADMIN);

        assertThat(promoted.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void deactivate_rejectsOwner() {
        User owner = userService.createWithPassword(
                accountId, "alice@example.com", "Alice", "password123", UserRole.OWNER);

        assertThatThrownBy(() -> userService.deactivate(owner.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deactivate_worksForNonOwner() {
        userService.createWithPassword(
                accountId, "alice@example.com", "Alice", "password123", UserRole.OWNER);
        User bob = userService.createWithPassword(
                accountId, "bob@example.com", "Bob", "password456", UserRole.MEMBER);

        User deactivated = userService.deactivate(bob.getId());

        assertThat(deactivated.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void changePassword_updatesHash() {
        User user = userService.createWithPassword(
                accountId, "alice@example.com", "Alice", "password123", UserRole.OWNER);
        String oldHash = user.getPasswordHash();

        User updated = userService.changePassword(user.getId(), "newpassword456");

        assertThat(updated.getPasswordHash()).isNotEqualTo(oldHash);
    }
}