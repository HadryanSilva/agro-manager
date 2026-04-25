package br.com.hadryan.agro.manager.domain.farm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FarmRepository extends JpaRepository<Farm, UUID> {

    // Lista todas as lavouras de uma conta ordenadas pela mais recente
    List<Farm> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    // Busca uma lavoura garantindo que pertence à conta informada
    Optional<Farm> findByIdAndAccountId(UUID id, UUID accountId);

    // Verifica existência antes de criar com nome duplicado
    boolean existsByNameAndAccountId(String name, UUID accountId);
}