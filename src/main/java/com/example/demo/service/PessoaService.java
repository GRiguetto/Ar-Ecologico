package com.example.demo.service;

import com.example.demo.exception.NotFoundException;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.database.model.PessoaEntity;
import com.example.demo.database.repository.IPessoaRepository;
import com.example.demo.dto.PessoaDto;

@Service
public class PessoaService {

    private final IPessoaRepository repository;

    public PessoaService(IPessoaRepository repository) {
        this.repository = repository;
    }

    public PessoaEntity salvaPessoa(PessoaDto dadosPessoa) {
        PessoaEntity pessoa = new PessoaEntity();
        pessoa.setEndereco(dadosPessoa.getEndereco());
        pessoa.setCep(dadosPessoa.getCep());
        pessoa.setNumero(dadosPessoa.getNumero());
        pessoa.setCidade(dadosPessoa.getCidade());
        pessoa.setEstado(dadosPessoa.getEstado());
        pessoa.setTelefone(dadosPessoa.getTelefone());
        pessoa.setNome(dadosPessoa.getNome());
        pessoa.setEmail(dadosPessoa.getEmail());

        return repository.save(pessoa);
    }

    public List<PessoaEntity> listarTodasPessoas() {
        return repository.findAll();
    }

    public List<PessoaEntity> verPessoa(Integer codPessoa) {
        PessoaEntity pessoaExistente = repository.findById(codPessoa)
                .orElseThrow(() -> new NotFoundException("Pessoa não encontrada"));

        return List.of(pessoaExistente);
    }

    public PessoaEntity atualizarPessoa(Integer codPessoa, PessoaDto dadosPessoa) {
        PessoaEntity pessoaExistente = repository.findById(codPessoa)
                .orElseThrow(() -> new NotFoundException("Pessoa não encontrada"));

        pessoaExistente.setEndereco(dadosPessoa.getEndereco());
        pessoaExistente.setCep(dadosPessoa.getCep());
        pessoaExistente.setNumero(dadosPessoa.getNumero());
        pessoaExistente.setCidade(dadosPessoa.getCidade());
        pessoaExistente.setEstado(dadosPessoa.getEstado());
        pessoaExistente.setTelefone(dadosPessoa.getTelefone());
        pessoaExistente.setNome(dadosPessoa.getNome());
        pessoaExistente.setEmail(dadosPessoa.getEmail());

        return repository.save(pessoaExistente);
    }

    public PessoaEntity atualizaPessoaParcial(Integer codPessoa, Map<String, Object> dados) {
        PessoaEntity pessoaExistente = repository.findById(codPessoa)
                .orElseThrow(() -> new NotFoundException("Pessoa não encontrada"));

        if (dados.containsKey("endereco")) {
            pessoaExistente.setEndereco(ValoresPatch.texto(dados.get("endereco")));
        }
        if (dados.containsKey("cep")) {
            pessoaExistente.setCep(ValoresPatch.texto(dados.get("cep")));
        }
        if (dados.containsKey("numero")) {
            pessoaExistente.setNumero(ValoresPatch.texto(dados.get("numero")));
        }
        if (dados.containsKey("cidade")) {
            pessoaExistente.setCidade(ValoresPatch.texto(dados.get("cidade")));
        }
        if (dados.containsKey("estado")) {
            pessoaExistente.setEstado(ValoresPatch.texto(dados.get("estado")));
        }
        if (dados.containsKey("telefone")) {
            pessoaExistente.setTelefone(ValoresPatch.texto(dados.get("telefone")));
        }
        if (dados.containsKey("nome")) {
            pessoaExistente.setNome(ValoresPatch.texto(dados.get("nome")));
        }
        if (dados.containsKey("email")) {
            pessoaExistente.setEmail(ValoresPatch.texto(dados.get("email")));
        }

        return repository.save(pessoaExistente);
    }

    public void deletaPessoa(Integer codPessoa) {
        PessoaEntity pessoaExistente = repository.findById(codPessoa)
                .orElseThrow(() -> new NotFoundException("Pessoa não encontrada"));

        repository.delete(pessoaExistente);
    }
}
