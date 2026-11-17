package com.example.demo.service;

import com.example.demo.exception.NotFoundException;
import com.example.demo.database.procedure.Procedures;

import static com.example.demo.database.procedure.Procedures.inteiro;
import static com.example.demo.database.procedure.Procedures.texto;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.example.demo.database.model.EquipamentoEntity;
import com.example.demo.database.repository.IEquipamentoRepository;
import com.example.demo.dto.EquipamentoDto;

@Service 
public class EquipamentoService {

    private final IEquipamentoRepository repository;
    private final Procedures procedures;

    public EquipamentoService(IEquipamentoRepository repository, Procedures procedures){
        this.repository = repository;
        this.procedures = procedures;
    }

    /** Cadastro pela procedure sp_CadEquipamento. */
    public EquipamentoEntity salvaEquipamento(EquipamentoDto dados) {
        Integer codEquipamento = procedures.executarRetornandoCodigo("sp_CadEquipamento",
                inteiro("btus", dados.getBtus()),
                texto("modelo", dados.getModelo()),
                texto("marca", dados.getMarca()),
                texto("obs", dados.getObs()));

        return repository.findById(codEquipamento).orElseThrow();
    }

    public List<EquipamentoEntity> listarTodosEquipamentos() {
        return repository.findAll();
    }

    public List<EquipamentoEntity> verEquipamento(Integer codEquipamento) {
        EquipamentoEntity equipamentoExistente = repository.findById(codEquipamento).orElseThrow(() -> new NotFoundException("Equipamento não encontrado"));

        return List.of(equipamentoExistente);
    }

    public EquipamentoEntity atualizarEquipamento(Integer codEquipamento, EquipamentoDto dadosEquipamento) {
        EquipamentoEntity equipamentoExistente = repository.findById(codEquipamento).orElseThrow(() -> new NotFoundException("Equipamento não encontrado"));

        equipamentoExistente.setBtus(dadosEquipamento.getBtus());
        equipamentoExistente.setModelo(dadosEquipamento.getModelo());
        equipamentoExistente.setMarca(dadosEquipamento.getMarca());
        equipamentoExistente.setObs(dadosEquipamento.getObs());

        return repository.save(equipamentoExistente);
    }

    public EquipamentoEntity atualizaEquipamentoParcial(Integer codEquipamento, Map<String, Object> dados) {
        EquipamentoEntity equipamentoExistente = repository.findById(codEquipamento).orElseThrow(() -> new NotFoundException("Equipamento não encontrado"));
        
        if(dados.containsKey("btus")) {
            equipamentoExistente.setBtus(ValoresPatch.inteiro("btus", dados.get("btus")));
        }
        if(dados.containsKey("modelo")) {
            equipamentoExistente.setModelo(ValoresPatch.texto(dados.get("modelo")));
        }
        if(dados.containsKey("marca")) {
            equipamentoExistente.setMarca(ValoresPatch.texto(dados.get("marca")));
        }
        if(dados.containsKey("obs")) {
            equipamentoExistente.setObs(ValoresPatch.texto(dados.get("obs")));
        }

        return repository.save(equipamentoExistente);
    }

    public void deleta(Integer codEquipamento) {
        EquipamentoEntity equipamentoExistente = repository.findById(codEquipamento).orElseThrow(() -> new NotFoundException("Equipamento não encontrado"));

        repository.delete(equipamentoExistente);
    }
}