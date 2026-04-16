package br.com.hadryan.agro.manager.user.service;

import br.com.hadryan.agro.manager.account.domain.Account;
import br.com.hadryan.agro.manager.account.service.AccountService;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ConflictException;
import br.com.hadryan.agro.manager.shared.exception.NotFoundException;
import br.com.hadryan.agro.manager.user.domain.User;
import br.com.hadryan.agro.manager.user.domain.UserRole;
import br.com.hadryan.agro.manager.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations on {@link User}.
 *
 * <p>Key invariants enforced here:
 * <ul>
 *   <li>Email is unique across the whole system.</li>
 *   <li>Each account has exactly one {@code OWNER}. Role changes that
 *       would break this invariant are rejected.</li>
 *   <li>At least one credential (password or Google) is always present;
 *       the entity itself guards this on every mutation.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository repository;
    private final AccountService accountService;
    private final PasswordHasher passwordHasher;

    // ------------------------------------------------------------------ //
    //  Queries                                                           //
    // ------------------------------------------------------------------ //

    public User findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> NotFoundException.of("User", id));
    }

    public List<User> findByAccount(Long accountId) {
        return repository.findByAccountId(accountId);
    }

    // ------------------------------------------------------------------ //
    //  Creation                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Creates a user with a local password. The caller is expected to
     * enforce that the requester is allowed to add users to the target
     * account — that check lands in the Security task.
     *
     * <p>If the account has no users yet, the new user is forced to be
     * {@code OWNER} regardless of the requested role. Otherwise the
     * requested role is validated against the OWNER-uniqueness rule.
     */
    @Transactional
    public User createWithPassword(Long accountId, String email, String name,
                                   String rawPassword, UserRole requestedRole) {
        Account account = accountService.findById(accountId);
        String normalizedEmail = email.toLowerCase();

        if (repository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email already in use: " + normalizedEmail);
        }

        UserRole effectiveRole = chooseInitialRole(accountId, requestedRole);
        String hash = passwordHasher.hash(rawPassword);

        return repository.save(User.withPassword(account, normalizedEmail, name, hash, effectiveRole));
    }

    /**
     * Forces OWNER when this is the first user of the account, and
     * rejects a second OWNER otherwise.
     */
    private UserRole chooseInitialRole(Long accountId, UserRole requested) {
        long ownerCount = repository.countByAccountIdAndRole(accountId, UserRole.OWNER);
        if (ownerCount == 0) {
            // First user must be the owner, whatever the caller asked for.
            return UserRole.OWNER;
        }
        if (requested == UserRole.OWNER) {
            throw new BusinessException(
                    "Account already has an owner. Transfer ownership instead.");
        }
        return requested;
    }

    // ------------------------------------------------------------------ //
    //  Updates                                                           //
    // ------------------------------------------------------------------ //

    @Transactional
    public User rename(Long id, String newName) {
        User user = findById(id);
        user.rename(newName);
        return repository.save(user);
    }

    @Transactional
    public User changePassword(Long id, String newRawPassword) {
        User user = findById(id);
        user.changePassword(passwordHasher.hash(newRawPassword));
        return repository.save(user);
    }

    /**
     * Changes a user's role. Rejects changes that would leave the account
     * without an owner or create a second owner.
     *
     * <p>To transfer ownership, call this method twice: first promote the
     * new owner, then demote the old one. Do it inside a single
     * application-level transaction to avoid a transient state with two
     * owners (TODO: once we have a multi-user concept in the HTTP layer,
     * expose a dedicated {@code transferOwnership} endpoint that does
     * both changes atomically).
     */
    @Transactional
    public User changeRole(Long id, UserRole newRole) {
        User user = findById(id);
        Long accountId = user.getAccount().getId();

        if (user.getRole() == UserRole.OWNER && newRole != UserRole.OWNER) {
            long ownerCount = repository.countByAccountIdAndRole(accountId, UserRole.OWNER);
            if (ownerCount <= 1) {
                throw new BusinessException(
                        "Cannot demote the sole owner. Promote another user to OWNER first.");
            }
        }
        if (user.getRole() != UserRole.OWNER && newRole == UserRole.OWNER) {
            long ownerCount = repository.countByAccountIdAndRole(accountId, UserRole.OWNER);
            if (ownerCount >= 1) {
                throw new BusinessException(
                        "Account already has an owner. Demote the current owner first.");
            }
        }

        user.changeRole(newRole);
        return repository.save(user);
    }

    @Transactional
    public User deactivate(Long id) {
        User user = findById(id);
        if (user.isOwner()) {
            throw new BusinessException(
                    "Cannot deactivate the owner. Transfer ownership first.");
        }
        user.deactivate();
        return repository.save(user);
    }

    @Transactional
    public User activate(Long id) {
        User user = findById(id);
        user.activate();
        return repository.save(user);
    }
}