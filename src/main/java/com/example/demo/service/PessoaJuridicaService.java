package com.example.demo.service;

import com.example.demo.exception.NotFoundException;
import com.example.demo.database.procedure.Procedures;

import static com.example.demo.database.procedure.Procedures.texto;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

import com.example.demo.database.model.PessoaJuridicaEntity;
import com.example.demo.database.repository.IPessoaJuridicaRepository;
import com.example.demo.dto.PessoaJuridicaDto;

@Service 
public class PessoaJuridicaService {

    private final IPessoaJuridicaRepository repository;
    private final Procedures procedures;

    public PessoaJuridicaService(IPessoaJuridicaRepository repository, Procedures procedures){
        this.repository = repository;
        this.procedures = procedures;
    }

    /** Cadastro pela procedure sp_CadPessoaJuridica (grava PESSOAS + PESSOAS_JURIDICAS numa transação). */
    public PessoaJuridicaEntity salvaPessoa(PessoaJuridicaDto dados) {
        Integer codPessoa = procedures.executarRetornandoCodigo("sp_CadPessoaJuridica",
                texto("nome", dados.getNome()),
                texto("endereco", dados.getEndereco()),
                texto("cep", dados.getCep()),
                texto("numero", dados.getNumero()),
                texto("cidade", dados.getCidade()),
                texto("estado", dados.getEstado()),
                texto("telefone", dados.getTelefone()),
                texto("cnpj", dados.getCnpj()),
                texto("razaoSocial", dados.getRazaoSocial()),
                texto("email", dados.getEmail()));

        return repository.findById(codPessoa).orElseThrow();
    }

    public List<PessoaJuridicaEntity> listarTodasPessoasFisicas() {
        return repository.findAll();
    }

    public List<PessoaJuridicaEntity> verPessoaJuridica(Integer codPessoa) {
        PessoaJuridicaEntity pessoaJuridicaExistente = repository.findById(codPessoa).orElseThrow(() -> new NotFoundException("Pessoa não encontrada"));

        return List.of(pessoaJuridicaExistente);
    }

    public PessoaJuridicaEntity atualizarPessoaJuridica(Integer codPessoa, PessoaJuridicaDto dadosPessoa) {
        PessoaJuridicaEntity pessoaJuridicaExistente = repository.findById(codPessoa).orElseThrow(() -> new NotFoundException("Pessoa não encontrada"));

        pessoaJuridicaExistente.setEndereco(dadosPessoa.getEndereco());
        pessoaJuridicaExistente.setCep(dadosPessoa.getCep());
        pessoaJuridicaExistente.setNumero(dadosPessoa.getNumero());
        pessoaJuridicaExistente.setCidade(dadosPessoa.getCidade());
        pessoaJuridicaExistente.setEstado(dadosPessoa.getEstado());
        pessoaJuridicaExistente.setTelefone(dadosPessoa.getTelefone());
        pessoaJuridicaExistente.setNome(dadosPessoa.getNome());
        pessoaJuridicaExistente.setEmail(dadosPessoa.getEmail());
        pessoaJuridicaExistente.setCnpj(dadosPessoa.getCnpj());
        pessoaJuridicaExistente.setRazaoSocial(dadosPessoa.getRazaoSocial());

        return repository.save(pessoaJuridicaExistente);
    }

    public PessoaJuridicaEntity atualizaPessoaJuridicaParcial(Integer codPessoa, Map<String, Object> dados) {
        PessoaJuridicaEntity pessoaJuridicaExistente = repository.findById(codPessoa).orElseThrow(() -> new NotFoundException("Pessoa não encontrada"));

        if(dados.containsKey("endereco")) {
            pessoaJuridicaExistente.setEndereco(ValoresPatch.texto(dados.get("endereco")));
        }
        if(dados.containsKey("cep")) {
            pessoaJuridicaExistente.setCep(ValoresPatch.texto(dados.get("cep")));
        }
        if(dados.containsKey("numero")) {
            pessoaJuridicaExistente.setNumero(ValoresPatch.texto(dados.get("numero")));
        }
        if(dados.containsKey("cidade")) {
            pessoaJuridicaExistente.setCidade(ValoresPatch.texto(dados.get("cidade")));
        }
        if(dados.containsKey("estado")) {
            pessoaJuridicaExistente.setEstado(ValoresPatch.texto(dados.get("estado")));
        }
        if(dados.containsKey("telefone")) {
            pessoaJuridicaExistente.setTelefone(ValoresPatch.texto(dados.get("telefone")));
        }
        if(dados.containsKey("nome")) {
            pessoaJuridicaExistente.setNome(ValoresPatch.texto(dados.get("nome")));
        }
        if(dados.containsKey("email")) {
            pessoaJuridicaExistente.setEmail(ValoresPatch.texto(dados.get("email")));
        }
        if(dados.containsKey("cnpj")) {
            pessoaJuridicaExistente.setCnpj(ValoresPatch.texto(dados.get("cnpj")));
        }
        if(dados.containsKey("razaoSocial")) {
            pessoaJuridicaExistente.setRazaoSocial(ValoresPatch.texto(dados.get("razaoSocial")));
        }

        return repository.save(pessoaJuridicaExistente);        
    }

    public void deletaPessoaJuridica(Integer codPessoa) {
        PessoaJuridicaEntity pessoaJuridicaExistente = repository.findById(codPessoa).orElseThrow(() -> new NotFoundException("Pessoa não encontrada"));

        repository.delete(pessoaJuridicaExistente);
    }
    
}
