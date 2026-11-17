package com.example.demo.service;

import com.example.demo.exception.NotFoundException;
import com.example.demo.database.procedure.Procedures;

import static com.example.demo.database.procedure.Procedures.data;
import static com.example.demo.database.procedure.Procedures.texto;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.example.demo.database.model.PessoaFisicaEntity;
import com.example.demo.database.repository.IPessoaFisicaRepository;
import com.example.demo.dto.PessoaFisicaDto;

@Service 
public class PessoaFisicaService {

    private final IPessoaFisicaRepository repository;
    private final Procedures procedures;

    public PessoaFisicaService(IPessoaFisicaRepository repository, Procedures procedures){
        this.repository = repository;
        this.procedures = procedures;
    }

    /** Cadastro pela procedure sp_CadPessoaFisica (grava PESSOAS + PESSOAS_FISICAS numa transação). */
    public PessoaFisicaEntity salvaPessoa(PessoaFisicaDto dados) {
        Integer codPessoa = procedures.executarRetornandoCodigo("sp_CadPessoaFisica",
                texto("nome", dados.getNome()),
                texto("endereco", dados.getEndereco()),
                texto("cep", dados.getCep()),
                texto("numero", dados.getNumero()),
                texto("cidade", dados.getCidade()),
                texto("estado", dados.getEstado()),
                texto("telefone", dados.getTelefone()),
                texto("cpf", dados.getCpf()),
                data("dataNascimento", dados.getDataNascimento()),
                texto("email", dados.getEmail()),
                texto("rg", dados.getRg()));

        return repository.findById(codPessoa).orElseThrow();
    }

    public List<PessoaFisicaEntity> listarTodasPessoasFisicas() {
        return repository.findAll();
    }

    public List<PessoaFisicaEntity> verPessoaFisica(Integer codPessoa) {
        PessoaFisicaEntity pessoaFisicaExistente = repository.findById(codPessoa).orElseThrow(() -> new NotFoundException("Pessoa não encontrada"));

        return List.of(pessoaFisicaExistente);
    }

    public PessoaFisicaEntity atualizarPessoaFisica(Integer codPessoa, PessoaFisicaDto dadosPessoa) {
        PessoaFisicaEntity pessoaFisicaExistente = repository.findById(codPessoa).orElseThrow(() -> new NotFoundException("Pessoa não encontrada"));

        pessoaFisicaExistente.setEndereco(dadosPessoa.getEndereco());
        pessoaFisicaExistente.setCep(dadosPessoa.getCep());
        pessoaFisicaExistente.setNumero(dadosPessoa.getNumero());
        pessoaFisicaExistente.setCidade(dadosPessoa.getCidade());
        pessoaFisicaExistente.setEstado(dadosPessoa.getEstado());
        pessoaFisicaExistente.setTelefone(dadosPessoa.getTelefone());
        pessoaFisicaExistente.setNome(dadosPessoa.getNome());
        pessoaFisicaExistente.setEmail(dadosPessoa.getEmail());
        pessoaFisicaExistente.setCpf(dadosPessoa.getCpf());
        pessoaFisicaExistente.setRg(dadosPessoa.getRg());
        pessoaFisicaExistente.setDataNascimento(dadosPessoa.getDataNascimento());

        return repository.save(pessoaFisicaExistente);
    }

    public PessoaFisicaEntity atualizaPessoaFisicaParcial(Integer codPessoa, Map<String, Object> dados) {
        PessoaFisicaEntity pessoaFisicaExistente = repository.findById(codPessoa).orElseThrow(() -> new NotFoundException("Pessoa não encontrada"));

        if(dados.containsKey("endereco")) {
            pessoaFisicaExistente.setEndereco(ValoresPatch.texto(dados.get("endereco")));
        }
        if(dados.containsKey("cep")) {
            pessoaFisicaExistente.setCep(ValoresPatch.texto(dados.get("cep")));
        }
        if(dados.containsKey("numero")) {
            pessoaFisicaExistente.setNumero(ValoresPatch.texto(dados.get("numero")));
        }
        if(dados.containsKey("cidade")) {
            pessoaFisicaExistente.setCidade(ValoresPatch.texto(dados.get("cidade")));
        }
        if(dados.containsKey("estado")) {
            pessoaFisicaExistente.setEstado(ValoresPatch.texto(dados.get("estado")));
        }
        if(dados.containsKey("telefone")) {
            pessoaFisicaExistente.setTelefone(ValoresPatch.texto(dados.get("telefone")));
        }
        if(dados.containsKey("nome")) {
            pessoaFisicaExistente.setNome(ValoresPatch.texto(dados.get("nome")));
        }
        if(dados.containsKey("email")) {
            pessoaFisicaExistente.setEmail(ValoresPatch.texto(dados.get("email")));
        }
        if(dados.containsKey("cpf")) {
            pessoaFisicaExistente.setCpf(ValoresPatch.texto(dados.get("cpf")));
        }
        if(dados.containsKey("rg")) {
            pessoaFisicaExistente.setRg(ValoresPatch.texto(dados.get("rg")));
        }
        if(dados.containsKey("dataNascimento")) {
            pessoaFisicaExistente.setDataNascimento(ValoresPatch.data("dataNascimento", dados.get("dataNascimento")));
        }

        return repository.save(pessoaFisicaExistente);        
    }

    public void deletaPessoaFisica(Integer codPessoa) {
        PessoaFisicaEntity pessoaFisicaExistente = repository.findById(codPessoa).orElseThrow(() -> new NotFoundException("Pessoa não encontrada"));

        repository.delete(pessoaFisicaExistente);
    }
    
}
