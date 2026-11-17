package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.example.demo.database.model.ContratoEntity;
import com.example.demo.database.procedure.Procedures;

import static com.example.demo.database.procedure.Procedures.data;
import static com.example.demo.database.procedure.Procedures.inteiro;
import static com.example.demo.database.procedure.Procedures.texto;
import com.example.demo.database.repository.IContratoRepository;
import com.example.demo.dto.ContratoDto;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.RegraNegocioException;

@Service
public class ContratoService {

    private final IContratoRepository repository;
    private final Referencias referencias;
    private final Procedures procedures;

    public ContratoService(IContratoRepository repository, Referencias referencias, Procedures procedures){
        this.repository = repository;
        this.referencias = referencias;
        this.procedures = procedures;
    }

    /** Valida em Java (cliente existe, PF ou PJ, status) e grava pela procedure sp_CadContrato. */
    public ContratoEntity salvaContrato(ContratoDto dadosContrato) {
        ContratoEntity contrato = new ContratoEntity();
        preencher(contrato, dadosContrato);

        Integer codContrato = procedures.executarRetornandoCodigo("sp_CadContrato",
                texto("descricao", contrato.getDescricao()),
                data("dataInicio", contrato.getDataInicio()),
                data("dataFim", contrato.getDataFim()),
                texto("tipoContrato", contrato.getTipoContrato()),
                inteiro("status", contrato.getStatus()),
                texto("obs", contrato.getObs()),
                inteiro("codPessoaFisica", dadosContrato.getPessoaFisicaCod()),
                inteiro("codPessoaJuridica", dadosContrato.getPessoaJuridicaCod()));

        return buscar(codContrato);
    }

    public List<ContratoEntity> listarTodosContratos() {
        return repository.findAll();
    }

    public List<ContratoEntity> verContrato(Integer codContrato) {
        return List.of(buscar(codContrato));
    }

    public ContratoEntity atualizarContrato(Integer codContrato, ContratoDto dadosContrato) {
        ContratoEntity contratoExistente = buscar(codContrato);
        preencher(contratoExistente, dadosContrato);

        return repository.save(contratoExistente);
    }

    public ContratoEntity atualizaContratoParcial(Integer codContrato, Map<String, Object> dados) {
        ContratoEntity contratoExistente = buscar(codContrato);

        if(dados.containsKey("descricao")) {
            contratoExistente.setDescricao(ValoresPatch.texto(dados.get("descricao")));
        }
        if(dados.containsKey("dataInicio")) {
            contratoExistente.setDataInicio(ValoresPatch.data("dataInicio", dados.get("dataInicio")));
        }
        if(dados.containsKey("dataFim")) {
            contratoExistente.setDataFim(ValoresPatch.data("dataFim", dados.get("dataFim")));
        }
        if(dados.containsKey("tipoContrato")) {
            contratoExistente.setTipoContrato(ValoresPatch.texto(dados.get("tipoContrato")));
        }
        if(dados.containsKey("obs")) {
            contratoExistente.setObs(ValoresPatch.texto(dados.get("obs")));
        }
        if(dados.containsKey("status")) {
            contratoExistente.setStatus(ValoresPatch.inteiro("status", dados.get("status")));
        }
        if(dados.containsKey("pessoaFisicaCod")) {
            contratoExistente.setPessoaFisicaCod(referencias.pessoaFisica(
                    ValoresPatch.inteiro("pessoaFisicaCod", dados.get("pessoaFisicaCod"))));
        }
        if(dados.containsKey("pessoaJuridicaCod")) {
            contratoExistente.setPessoaJuridicaCod(referencias.pessoaJuridica(
                    ValoresPatch.inteiro("pessoaJuridicaCod", dados.get("pessoaJuridicaCod"))));
        }
        validar(contratoExistente);

        return repository.save(contratoExistente);
    }

    public void deleta(Integer codContrato) {
        repository.delete(buscar(codContrato));
    }

    private ContratoEntity buscar(Integer codContrato) {
        return repository.findById(codContrato)
                .orElseThrow(() -> new NotFoundException("Contrato " + codContrato + " não encontrado"));
    }

    private void preencher(ContratoEntity contrato, ContratoDto dados) {
        contrato.setDescricao(dados.getDescricao());
        contrato.setDataInicio(dados.getDataInicio());
        contrato.setDataFim(dados.getDataFim());
        contrato.setTipoContrato(dados.getTipoContrato());
        contrato.setObs(dados.getObs());
        contrato.setStatus(dados.getStatus() == null ? 1 : dados.getStatus());
        contrato.setPessoaFisicaCod(referencias.pessoaFisica(dados.getPessoaFisicaCod()));
        contrato.setPessoaJuridicaCod(referencias.pessoaJuridica(dados.getPessoaJuridicaCod()));
        validar(contrato);
    }

    private void validar(ContratoEntity contrato) {
        if ((contrato.getPessoaFisicaCod() == null) == (contrato.getPessoaJuridicaCod() == null)) {
            throw new RegraNegocioException(
                    "Informe exatamente um entre pessoaFisicaCod e pessoaJuridicaCod");
        }
        if (contrato.getStatus() == null || contrato.getStatus() < 1 || contrato.getStatus() > 2) {
            throw new RegraNegocioException("status do contrato deve ser 1 (ativo) ou 2 (inativo)");
        }
        if (contrato.getDataInicio() != null && contrato.getDataFim() != null
                && contrato.getDataFim().isBefore(contrato.getDataInicio())) {
            throw new RegraNegocioException("dataFim não pode ser anterior a dataInicio");
        }
    }

}
