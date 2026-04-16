package br.com.hadryan.agro.manager.user.domain;

import br.com.hadryan.agro.manager.account.domain.Account;
import br.com.hadryan.agro.manager.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A person who can log into the system.
 *
 * <p>Every user belongs to exactly one {@link Account}; the relationship is
 * immutable after creation (changing accounts would mean a new user record).
 *
 * <p>Authentication is flexible: a user can have a local password, a Google
 * identity ({@code googleSubject} = the OIDC {@code sub} claim), or both.
 * At least one credential must always be present — enforced both by the
 * constructor and by a DB check constraint.
 *
 * <p>Deactivation is a soft delete: setting {@code status = INACTIVE} blocks
 * login while keeping the row so historical foreign keys stay valid.
 */
@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 120)
    private String name;

    /** BCrypt hash. Never the raw password. Null if the user only logs in via Google. */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /** OIDC {@code sub} claim from Google. Null if the user only uses password login. */
    @Column(name = "google_subject", unique = true, length = 255)
    private String googleSubject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    private User(Account account, String email, String name,
                 String passwordHash, String googleSubject, UserRole role) {
        if (passwordHash == null && googleSubject == null) {
            throw new IllegalArgumentException(
                    "At least one credential (password or Google) is required");
        }
        this.account = account;
        this.email = email.toLowerCase();
        this.name = name;
        this.passwordHash = passwordHash;
        this.googleSubject = googleSubject;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    /**
     * Factory for a user authenticating with a local password.
     */
    public static User withPassword(Account account, String email, String name,
                                    String passwordHash, UserRole role) {
        return new User(account, email, name, passwordHash, null, role);
    }

    /**
     * Factory for a user authenticating exclusively through Google.
     */
    public static User withGoogle(Account account, String email, String name,
                                  String googleSubject, UserRole role) {
        return new User(account, email, name, null, googleSubject, role);
    }

    // ------------------------------------------------------------------ //
    //  Behaviour                                                         //
    // ------------------------------------------------------------------ //

    public void rename(String newName) {
        this.name = newName;
    }

    /** Sets a new BCrypt hash. Passing {@code null} is allowed only if Google is linked. */
    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null && this.googleSubject == null) {
            throw new IllegalArgumentException(
                    "Cannot remove the password when no other credential is configured");
        }
        this.passwordHash = newPasswordHash;
    }

    /** Links (or re-links) a Google identity to this user. */
    public void linkGoogle(String googleSubject) {
        this.googleSubject = googleSubject;
    }

    /** Unlinks Google. Requires a password to already be set. */
    public void unlinkGoogle() {
        if (this.passwordHash == null) {
            throw new IllegalArgumentException(
                    "Cannot unlink Google when no other credential is configured");
        }
        this.googleSubject = null;
    }

    public void changeRole(UserRole newRole) {
        this.role = newRole;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isOwner() {
        return role == UserRole.OWNER;
    }
}