package br.com.hadryan.agro.manager.domain.quotation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, UUID> {

    // Lista todas as cotações de uma conta com dados da lavoura, mais recentes primeiro
    @Query("""
            SELECT q FROM Quotation q
            LEFT JOIN FETCH q.farm
            WHERE q.account.id = :accountId
            ORDER BY q.productName ASC, q.unitPrice ASC
            """)
    List<Quotation> findByAccountIdOrderByProductAndPrice(UUID accountId);

    // Busca cotação garantindo que pertence à conta
    Optional<Quotation> findByIdAndAccountId(UUID id, UUID accountId);

    // Lista nomes distintos de produtos para sugestão de autocomplete
    @Query("SELECT DISTINCT q.productName FROM Quotation q WHERE q.account.id = :accountId ORDER BY q.productName")
    List<String> findDistinctProductNamesByAccountId(UUID accountId);
}