package com.example.demo.service;

import org.springframework.stereotype.Component;

import com.example.demo.database.model.ContratoEntity;
import com.example.demo.database.model.EquipamentoEntity;
import com.example.demo.database.model.OrdemServicoEntity;
import com.example.demo.database.model.PessoaFisicaEntity;
import com.example.demo.database.model.PessoaJuridicaEntity;
import com.example.demo.database.model.PrestadorServicoEntity;
import com.example.demo.database.model.ServicoEntity;
import com.example.demo.database.repository.IContratoRepository;
import com.example.demo.database.repository.IEquipamentoRepository;
import com.example.demo.database.repository.IOrdemServicoRepository;
import com.example.demo.database.repository.IPessoaFisicaRepository;
import com.example.demo.database.repository.IPessoaJuridicaRepository;
import com.example.demo.database.repository.IPrestadorServicoRepository;
import com.example.demo.database.repository.IServicoRepository;
import com.example.demo.exception.NotFoundException;

/**
 * Converte os códigos recebidos nos DTOs (ex.: "pessoaFisicaCod": 1) nas entidades
 * correspondentes. Código nulo devolve null; código inexistente gera 404.
 */
@Component
public class Referencias {

    private final IPessoaFisicaRepository pessoasFisicas;
    private final IPessoaJuridicaRepository pessoasJuridicas;
    private final IPrestadorServicoRepository prestadores;
    private final IContratoRepository contratos;
    private final IEquipamentoRepository equipamentos;
    private final IOrdemServicoRepository ordensServico;
    private final IServicoRepository servicos;

    public Referencias(IPessoaFisicaRepository pessoasFisicas, IPessoaJuridicaRepository pessoasJuridicas,
            IPrestadorServicoRepository prestadores, IContratoRepository contratos,
            IEquipamentoRepository equipamentos, IOrdemServicoRepository ordensServico,
            IServicoRepository servicos) {
        this.pessoasFisicas = pessoasFisicas;
        this.pessoasJuridicas = pessoasJuridicas;
        this.prestadores = prestadores;
        this.contratos = contratos;
        this.equipamentos = equipamentos;
        this.ordensServico = ordensServico;
        this.servicos = servicos;
    }

    public PessoaFisicaEntity pessoaFisica(Integer cod) {
        return cod == null ? null : pessoasFisicas.findById(cod)
                .orElseThrow(() -> new NotFoundException("Pessoa física " + cod + " não encontrada"));
    }

    public PessoaJuridicaEntity pessoaJuridica(Integer cod) {
        return cod == null ? null : pessoasJuridicas.findById(cod)
                .orElseThrow(() -> new NotFoundException("Pessoa jurídica " + cod + " não encontrada"));
    }

    public PrestadorServicoEntity prestador(Integer cod) {
        return cod == null ? null : prestadores.findById(cod)
                .orElseThrow(() -> new NotFoundException("Prestador de serviço " + cod + " não encontrado"));
    }

    public ContratoEntity contrato(Integer cod) {
        return cod == null ? null : contratos.findById(cod)
                .orElseThrow(() -> new NotFoundException("Contrato " + cod + " não encontrado"));
    }

    public EquipamentoEntity equipamento(Integer cod) {
        return cod == null ? null : equipamentos.findById(cod)
                .orElseThrow(() -> new NotFoundException("Equipamento " + cod + " não encontrado"));
    }

    public OrdemServicoEntity ordemServico(Integer cod) {
        return cod == null ? null : ordensServico.findById(cod)
                .orElseThrow(() -> new NotFoundException("Ordem de serviço " + cod + " não encontrada"));
    }

    public ServicoEntity servico(Integer cod) {
        return cod == null ? null : servicos.findById(cod)
                .orElseThrow(() -> new NotFoundException("Serviço " + cod + " não encontrado"));
    }
}
