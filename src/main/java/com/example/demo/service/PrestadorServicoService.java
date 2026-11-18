package com.example.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.example.demo.database.model.PrestadorServicoEntity;
import com.example.demo.database.procedure.Procedures;

import static com.example.demo.database.procedure.Procedures.booleano;
import static com.example.demo.database.procedure.Procedures.data;
import static com.example.demo.database.procedure.Procedures.decimal;
import static com.example.demo.database.procedure.Procedures.texto;
import com.example.demo.database.repository.IPrestadorServicoRepository;
import com.example.demo.dto.PrestadorServicoDto;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.RegraNegocioException;

@Service
public class PrestadorServicoService {

    private final IPrestadorServicoRepository repository;
    private final Procedures procedures;

    /** A coluna SENHA guarda apenas o hash BCrypt (regra da documentação do banco). */
    private final PasswordEncoder senhas;

    public PrestadorServicoService(IPrestadorServicoRepository repository, Procedures procedures, PasswordEncoder senhas){
        this.repository = repository;
        this.procedures = procedures;
        this.senhas = senhas;
    }

    /**
     * Cadastro pela procedure sp_CadPrestadorServico
     * (grava PESSOAS + PESSOAS_FISICAS + PRESTADOR_SERVICOS numa transação).
     * A senha vai para a procedure já convertida em hash.
     */
    public PrestadorServicoEntity salvaPessoa(PrestadorServicoDto dados) {
        if (dados.getSenha() == null || dados.getSenha().isBlank()) {
            throw new RegraNegocioException("Informe uma senha para o funcionário.");
        }
        Integer codPessoa = procedures.executarRetornandoCodigo("sp_CadPrestadorServico",
                texto("nome", dados.getNome()),
                texto("endereco", dados.getEndereco()),
                texto("cep", dados.getCep()),
                texto("numero", dados.getNumero()),
                texto("cidade", dados.getCidade()),
                texto("estado", dados.getEstado()),
                texto("telefone", dados.getTelefone()),
                texto("cpf", dados.getCpf()),
                data("dataNascimento", dados.getDataNascimento()),
                decimal("pagamento", dados.getPagamento()),
                texto("senha", senhas.encode(dados.getSenha())),
                booleano("administrador", dados.getAdministrador() == null ? true : dados.getAdministrador()),
                texto("email", dados.getEmail()),
                texto("rg", dados.getRg()));

        return repository.findById(codPessoa).orElseThrow();
    }

    public List<PrestadorServicoEntity> listarTodosPrestadoresServicos() {
        return repository.findAll();
    }

    public List<PrestadorServicoEntity> verPrestadorServico(Integer codPessoa) {
        return List.of(buscar(codPessoa));
    }

    /** PUT: substitui os dados. Se senha ou administrador vierem vazios, mantém os atuais. */
    public PrestadorServicoEntity atualizarPrestadorServico(Integer codPessoa, PrestadorServicoDto dados) {
        PrestadorServicoEntity existente = buscar(codPessoa);
        preencher(existente, dados);
        if (dados.getAdministrador() != null) {
            existente.setAdministrador(dados.getAdministrador());
        }
        if (dados.getSenha() != null && !dados.getSenha().isBlank()) {
            existente.setSenha(senhas.encode(dados.getSenha()));
        }

        return repository.save(existente);
    }

    public PrestadorServicoEntity atualizaPrestadorServicoParcial(Integer codPessoa, Map<String, Object> dados) {
        PrestadorServicoEntity p = buscar(codPessoa);

        if(dados.containsKey("endereco")) {
            p.setEndereco(ValoresPatch.texto(dados.get("endereco")));
        }
        if(dados.containsKey("cep")) {
            p.setCep(ValoresPatch.texto(dados.get("cep")));
        }
        if(dados.containsKey("numero")) {
            p.setNumero(ValoresPatch.texto(dados.get("numero")));
        }
        if(dados.containsKey("cidade")) {
            p.setCidade(ValoresPatch.texto(dados.get("cidade")));
        }
        if(dados.containsKey("estado")) {
            p.setEstado(ValoresPatch.texto(dados.get("estado")));
        }
        if(dados.containsKey("telefone")) {
            p.setTelefone(ValoresPatch.texto(dados.get("telefone")));
        }
        if(dados.containsKey("nome")) {
            p.setNome(ValoresPatch.texto(dados.get("nome")));
        }
        if(dados.containsKey("email")) {
            p.setEmail(ValoresPatch.texto(dados.get("email")));
        }
        if(dados.containsKey("cpf")) {
            p.setCpf(ValoresPatch.texto(dados.get("cpf")));
        }
        if(dados.containsKey("rg")) {
            p.setRg(ValoresPatch.texto(dados.get("rg")));
        }
        if(dados.containsKey("dataNascimento")) {
            p.setDataNascimento(ValoresPatch.data("dataNascimento", dados.get("dataNascimento")));
        }
        // "pagamento" é o nome da coluna; "salario" é aceito por compatibilidade.
        for (String campo : new String[] {"pagamento", "salario"}) {
            if(dados.containsKey(campo)) {
                p.setPagamento(ValoresPatch.decimal(campo, dados.get(campo)));
            }
        }
        if(dados.containsKey("administrador")) {
            p.setAdministrador(ValoresPatch.booleano("administrador", dados.get("administrador")));
        }
        if(dados.containsKey("senha")) {
            String senha = ValoresPatch.texto(dados.get("senha"));
            if (senha == null || senha.isBlank()) {
                throw new RegraNegocioException("A senha não pode ficar em branco.");
            }
            p.setSenha(senhas.encode(senha));
        }

        return repository.save(p);
    }

    public void deletaPrestadorServico(Integer codPessoa) {
        repository.delete(buscar(codPessoa));
    }

    private PrestadorServicoEntity buscar(Integer codPessoa) {
        return repository.findById(codPessoa)
                .orElseThrow(() -> new NotFoundException("Prestador de serviço " + codPessoa + " não encontrado"));
    }

    private void preencher(PrestadorServicoEntity p, PrestadorServicoDto dados) {
        p.setEndereco(dados.getEndereco());
        p.setCep(dados.getCep());
        p.setNumero(dados.getNumero());
        p.setCidade(dados.getCidade());
        p.setEstado(dados.getEstado());
        p.setTelefone(dados.getTelefone());
        p.setNome(dados.getNome());
        p.setEmail(dados.getEmail());
        p.setCpf(dados.getCpf());
        p.setRg(dados.getRg());
        p.setDataNascimento(dados.getDataNascimento());
        p.setPagamento(dados.getPagamento());
    }

}
