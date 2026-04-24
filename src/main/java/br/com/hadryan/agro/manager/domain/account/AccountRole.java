package br.com.hadryan.agro.manager.domain.account;

/**
 * Papel do usuário dentro de uma conta.
 * Define o nível de acesso às funcionalidades da conta.
 */
public enum AccountRole {

    // Criador da conta — acesso total, incluindo exclusão
    OWNER,

    // Pode gerenciar membros e convidar novos usuários
    ADMIN,

    // Acesso às funcionalidades do domínio da conta
    MEMBER
}