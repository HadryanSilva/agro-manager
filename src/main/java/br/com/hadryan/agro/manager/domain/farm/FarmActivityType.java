package br.com.hadryan.agro.manager.domain.farm;

/**
 * Tipos de atividade registrados no histórico de uma lavoura.
 */
public enum FarmActivityType {

    // Despesas
    EXPENSE_CREATED,
    EXPENSE_UPDATED,
    EXPENSE_DELETED,
    EXPENSE_PAID,

    // Alterações na lavoura
    FARM_UPDATED,

    // Anotação manual do usuário
    NOTE
}