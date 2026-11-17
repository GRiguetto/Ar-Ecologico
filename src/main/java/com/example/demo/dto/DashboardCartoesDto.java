package com.example.demo.dto;

import java.math.BigDecimal;

/**
 * JSON de GET /dashboard/cartoes. Cada campo vem de uma view ou function do banco:
 *   totalClientes, contratosAtivos, osEmAberto, contratosVencendo30Dias -> VW_DASHBOARD_CARTOES
 *   faturamento  -> dbo.FN_FATURAMENTO_PERIODO(inicio, fim)
 *   ticketMedio  -> dbo.FC_ORDEM_SERVICO_MEDIA(inicio, fim)
 */
public record DashboardCartoesDto(
        Integer totalClientes,
        Integer contratosAtivos,
        Integer osEmAberto,
        Integer contratosVencendo30Dias,
        BigDecimal faturamento,
        BigDecimal ticketMedio) {
}
