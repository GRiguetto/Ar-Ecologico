package com.example.demo.service;

import com.example.demo.exception.NotFoundException;
import com.example.demo.database.procedure.Procedures;

import static com.example.demo.database.procedure.Procedures.data;
import static com.example.demo.database.procedure.Procedures.decimal;
import static com.example.demo.database.procedure.Procedures.texto;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.example.demo.database.model.ServicoEntity;
import com.example.demo.database.repository.IServicoRepository;
import com.example.demo.dto.ServicoDto;

@Service 
public class ServicoService {

    private final IServicoRepository repository;
    private final Procedures procedures;

    public ServicoService(IServicoRepository repository, Procedures procedures){
        this.repository = repository;
        this.procedures = procedures;
    }

    /** Cadastro pela procedure sp_CadServico. */
    public ServicoEntity salvaServico(ServicoDto dados) {
        Integer codServico = procedures.executarRetornandoCodigo("sp_CadServico",
                data("dataInicio", dados.getDataInicio()),
                data("dataFim", dados.getDataFim()),
                decimal("valorServico", dados.getValorServico()),
                texto("descricao", dados.getDescricao()));

        return repository.findById(codServico).orElseThrow();
    }

    public List<ServicoEntity> listarTodosServicos() {
        return repository.findAll();
    }

    public List<ServicoEntity> verServico(Integer codServico) {
        ServicoEntity servicoExistente = repository.findById(codServico).orElseThrow(() -> new NotFoundException("Serviço não encontrado"));

        return List.of(servicoExistente);
    }

    public ServicoEntity atualizarServico(Integer codServico, ServicoDto dadosServico) {
        ServicoEntity servicoExistente = repository.findById(codServico).orElseThrow(() -> new NotFoundException("Serviço não encontrado"));

        servicoExistente.setDescricao(dadosServico.getDescricao());
        servicoExistente.setDataInicio(dadosServico.getDataInicio());
        servicoExistente.setDataFim(dadosServico.getDataFim());
        servicoExistente.setValorServico(dadosServico.getValorServico());

        return repository.save(servicoExistente);
    }

    public ServicoEntity atualizaServicoParcial(Integer codServico, Map<String, Object> dados) {
        ServicoEntity servicoExistente = repository.findById(codServico).orElseThrow(() -> new NotFoundException("Serviço não encontrado"));

        if(dados.containsKey("descricao")) {
            servicoExistente.setDescricao(ValoresPatch.texto(dados.get("descricao")));
        }
        if(dados.containsKey("dataInicio")) {
            servicoExistente.setDataInicio(ValoresPatch.data("dataInicio", dados.get("dataInicio")));
        }
        if(dados.containsKey("dataFim")) {
            servicoExistente.setDataFim(ValoresPatch.data("dataFim", dados.get("dataFim")));
        }
        if(dados.containsKey("valorServico")) {
            servicoExistente.setValorServico(ValoresPatch.decimal("valorServico", dados.get("valorServico")));
        }

        return repository.save(servicoExistente);
    }

    public void deleta(Integer codServico) {
        ServicoEntity servicoExistente = repository.findById(codServico).orElseThrow(() -> new NotFoundException("Serviço não encontrado"));

        repository.delete(servicoExistente);
    }
    
}
