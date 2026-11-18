package com.example.demo.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.database.model.ContratoEquipamentoEntity;
import com.example.demo.database.model.ContratoEquipamentoId;
import com.example.demo.database.procedure.Procedures;

import static com.example.demo.database.procedure.Procedures.inteiro;
import com.example.demo.database.repository.IContratoEquipamentoRepository;
import com.example.demo.dto.ContratoEquipamentoDto;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.RegraNegocioException;

@Service
public class ContratoEquipamentoService {

    private final IContratoEquipamentoRepository repository;
    private final Referencias referencias;
    private final Procedures procedures;

    public ContratoEquipamentoService(IContratoEquipamentoRepository repository, Referencias referencias,
            Procedures procedures) {
        this.repository = repository;
        this.referencias = referencias;
        this.procedures = procedures;
    }

    public ContratoEquipamentoEntity salvaContratoEquipamento(ContratoEquipamentoDto dados) {
        if (dados.getContratoCod() == null || dados.getEquipamentoCod() == null) {
            throw new RegraNegocioException("Escolha o contrato e o equipamento.");
        }
        ContratoEquipamentoId id = new ContratoEquipamentoId(dados.getContratoCod(), dados.getEquipamentoCod());
        if (repository.existsById(id)) {
            throw new RegraNegocioException("Esse equipamento já está no contrato. "
                    + "Edite a quantidade em vez de cadastrar de novo.");
        }

        ContratoEquipamentoEntity contratoEquipamento = new ContratoEquipamentoEntity(
                referencias.contrato(dados.getContratoCod()),
                referencias.equipamento(dados.getEquipamentoCod()),
                dados.getQtd());
        validar(contratoEquipamento);

        // Gravação pela procedure sp_CadContratoEquipamento (chave composta: não devolve código).
        procedures.executar("sp_CadContratoEquipamento",
                inteiro("contratoCod", dados.getContratoCod()),
                inteiro("equipamentoCod", dados.getEquipamentoCod()),
                inteiro("qtd", dados.getQtd()));

        return buscar(id);
    }

    public List<ContratoEquipamentoEntity> listarTodosContratosEquipamentos() {
        return repository.findAll();
    }

    public List<ContratoEquipamentoEntity> verContratoEquipamento(ContratoEquipamentoId id) {
        return List.of(buscar(id));
    }

    public ContratoEquipamentoEntity atualizarContratoEquipamento(
            ContratoEquipamentoId id, ContratoEquipamentoDto dados) {
        ContratoEquipamentoEntity existente = buscar(id);
        existente.setQtd(dados.getQtd());
        validar(existente);

        return repository.save(existente);
    }

    public ContratoEquipamentoEntity atualizaContratoEquipamentoParcial(
            ContratoEquipamentoId id, Map<String, Object> dados) {
        ContratoEquipamentoEntity existente = buscar(id);

        if (dados.containsKey("qtd")) {
            existente.setQtd(ValoresPatch.inteiro("qtd", dados.get("qtd")));
        }
        validar(existente);

        return repository.save(existente);
    }

    public void deletaContratoEquipamento(ContratoEquipamentoId id) {
        repository.delete(buscar(id));
    }

    private ContratoEquipamentoEntity buscar(ContratoEquipamentoId id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipamento " + id.getEquipamentoCod()
                        + " não encontrado no contrato " + id.getContratoCod()));
    }

    private void validar(ContratoEquipamentoEntity ce) {
        if (ce.getQtd() == null || ce.getQtd() <= 0) {
            throw new RegraNegocioException("A quantidade deve ser maior que zero.");
        }
    }
}
