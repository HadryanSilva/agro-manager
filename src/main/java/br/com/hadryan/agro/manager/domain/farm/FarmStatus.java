package br.com.hadryan.agro.manager.domain.farm;

/**
 * Status da lavoura calculado dinamicamente a partir das datas registradas.
 * Nunca é armazenado no banco — sempre derivado no momento da consulta.
 */
public enum FarmStatus {

    // Plantio ainda não iniciado
    EM_PREPARACAO,

    // Plantio iniciado, colheita ainda não começou
    EM_ANDAMENTO,

    // Colheita iniciada (pode estar em andamento ou concluída)
    COLHIDA,

    // Lavoura cancelada manualmente
    CANCELADA
}