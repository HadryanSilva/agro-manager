package br.com.hadryan.agro.manager.domain.trading;

/**
 * Status de um lote de compra.
 * Atualizado automaticamente ao registrar ou remover vendas,
 * e pode ser encerrado manualmente pelo atravessador.
 */
public enum PurchaseLotStatus {

    // Lote com estoque disponível para venda
    OPEN,

    // Lote totalmente vendido ou encerrado manualmente
    CLOSED
}
