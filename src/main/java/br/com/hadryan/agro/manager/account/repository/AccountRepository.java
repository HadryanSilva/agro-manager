package br.com.hadryan.agro.manager.account.repository;

import br.com.hadryan.agro.manager.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}