package br.com.hadryan.agro.manager.account.service;

import br.com.hadryan.agro.manager.account.domain.Account;
import br.com.hadryan.agro.manager.account.repository.AccountRepository;
import br.com.hadryan.agro.manager.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations on {@link Account}.
 *
 * <p>Methods are transactional: read-only by default to let JPA skip dirty
 * checking, write-enabled where state changes happen. Keeping the
 * transaction boundary here (not in the controller) means we can add cache
 * evictions, event publishing, and other cross-cutting concerns in one
 * place later.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository repository;

    public Account findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Account", id));
    }

    @Transactional
    public Account create(String name) {
        return repository.save(Account.create(name));
    }

    @Transactional
    public Account rename(Long id, String newName) {
        Account account = findById(id);
        account.rename(newName);
        // Save is redundant under the managed persistence context but makes
        // the intent obvious to readers and keeps behaviour stable if the
        // method is ever called from a detached context.
        return repository.save(account);
    }

    @Transactional
    public Account suspend(Long id) {
        Account account = findById(id);
        account.suspend();
        return repository.save(account);
    }

    @Transactional
    public Account activate(Long id) {
        Account account = findById(id);
        account.activate();
        return repository.save(account);
    }
}