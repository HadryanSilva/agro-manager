package br.com.hadryan.agro.manager.user.service;

/**
 * Strategy for hashing and verifying user passwords.
 *
 * <p>Temporary abstraction used by {@link UserService} so the feature can
 * ship before Spring Security is wired up. Task 6 will delete this
 * interface and replace the bean with
 * {@code org.springframework.security.crypto.password.PasswordEncoder},
 * which has the same two-method shape and will plug in transparently.
 */
public interface PasswordHasher {

    /** Returns an opaque hash string safe to persist. */
    String hash(String rawPassword);

    /** Returns true iff {@code rawPassword} produced {@code hash}. */
    boolean matches(String rawPassword, String hash);
}