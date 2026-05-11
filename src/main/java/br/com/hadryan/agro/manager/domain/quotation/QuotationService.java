package br.com.hadryan.agro.manager.domain.quotation;

import java.util.List;
import java.util.UUID;

/** Contrato do serviço de gerenciamento de cotações. */
public interface QuotationService {

    QuotationResponse create(UUID accountId, UUID userId, QuotationRequest request);

    List<QuotationGroupResponse> listGrouped(UUID accountId, UUID userId);

    List<String> getProductSuggestions(UUID accountId, UUID userId);

    QuotationResponse update(UUID accountId, UUID userId, UUID quotationId, QuotationRequest request);

    void delete(UUID accountId, UUID userId, UUID quotationId);
}