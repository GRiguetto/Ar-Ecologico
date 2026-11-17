package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ContratoVencendoDto;
import com.example.demo.dto.DashboardCartoesDto;
import com.example.demo.service.DashboardService;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    /**
     * GET /dashboard/cartoes?inicio=2026-01-01&fim=2026-12-31
     * inicio e fim são opcionais (sem eles, faturamento e ticket médio consideram todo o período).
     */
    @GetMapping("/cartoes")
    public DashboardCartoesDto cartoes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return service.cartoes(inicio, fim);
    }

    /** GET /dashboard/contratos-vencendo : contratos ativos que terminam nos próximos 30 dias. */
    @GetMapping("/contratos-vencendo")
    public List<ContratoVencendoDto> contratosVencendo() {
        return service.contratosVencendo();
    }
}
