package br.com.hadryan.agro.manager.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Utilizado para localizar usuários autenticados via OAuth2
    Optional<User> findByProviderIdAndAuthProvider(String providerId, AuthProvider authProvider);
}