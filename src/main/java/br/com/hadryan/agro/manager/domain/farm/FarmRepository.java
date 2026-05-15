package br.com.hadryan.agro.manager.domain.farm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * Lista lavouras de uma conta filtrando por status calculado no banco.
     *
     * FarmStatus é um campo @Transient calculado a partir de três colunas armazenadas:
     *   CANCELADA     → cancelled = true
     *   COLHIDA       → cancelled = false AND harvestStartDate IS NOT NULL
     *   EM_ANDAMENTO  → cancelled = false AND harvestStartDate IS NULL AND plantingStartDate IS NOT NULL
     *   EM_PREPARACAO → cancelled = false AND harvestStartDate IS NULL AND plantingStartDate IS NULL
     *
     * A tradução das condições aqui reflete exatamente a lógica do método Farm.getStatus().
     */
    @Query("""
            SELECT f FROM Farm f
            WHERE f.account.id = :accountId
              AND (
                    (:status = 'CANCELADA'     AND f.cancelled = true)
                 OR (:status = 'COLHIDA'       AND f.cancelled = false AND f.harvestStartDate IS NOT NULL)
                 OR (:status = 'EM_ANDAMENTO'  AND f.cancelled = false AND f.harvestStartDate IS NULL AND f.plantingStartDate IS NOT NULL)
                 OR (:status = 'EM_PREPARACAO' AND f.cancelled = false AND f.harvestStartDate IS NULL AND f.plantingStartDate IS NULL)
                  )
            ORDER BY f.createdAt DESC
            """)
    List<Farm> findByAccountIdAndComputedStatus(UUID accountId, String status);

    @Modifying
    @Query("DELETE FROM Farm f WHERE f.account.id = :accountId")
    void deleteByAccountId(UUID accountId);
}