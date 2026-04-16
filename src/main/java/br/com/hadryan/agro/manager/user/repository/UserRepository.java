package br.com.hadryan.agro.manager.user.repository;

import br.com.hadryan.agro.manager.user.domain.User;
import br.com.hadryan.agro.manager.user.domain.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSubject(String googleSubject);

    boolean existsByEmail(String email);

    List<User> findByAccountId(Long accountId);

    long countByAccountIdAndRole(Long accountId, UserRole role);
}