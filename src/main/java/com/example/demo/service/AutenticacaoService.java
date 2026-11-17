package com.example.demo.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.database.model.PrestadorServicoEntity;
import com.example.demo.database.repository.IPrestadorServicoRepository;
import com.example.demo.dto.LoginDto;
import com.example.demo.dto.LoginRespostaDto;
import com.example.demo.exception.NaoAutorizadoException;

/**
 * Login dos prestadores de serviço (e-mail + senha conferida contra o hash BCrypt).
 *
 * ATENÇÃO: o token devolvido ainda NÃO é exigido pelos demais endpoints.
 * Ele só identifica a sessão no front-end. Proteger a API (Spring Security/JWT)
 * é um passo seguinte.
 */
@Service
public class AutenticacaoService {

    private static final String MENSAGEM_INVALIDA = "E-mail ou senha incorretos";

    private final IPrestadorServicoRepository prestadores;
    private final PasswordEncoder senhas;

    public AutenticacaoService(IPrestadorServicoRepository prestadores, PasswordEncoder senhas) {
        this.prestadores = prestadores;
        this.senhas = senhas;
    }

    public LoginRespostaDto entrar(LoginDto dados) {
        if (dados == null || dados.getEmail() == null || dados.getSenha() == null) {
            throw new NaoAutorizadoException(MENSAGEM_INVALIDA);
        }
        PrestadorServicoEntity prestador = prestadores.findFirstByEmailIgnoreCase(dados.getEmail().trim())
                .filter(p -> senhas.matches(dados.getSenha(), p.getSenha()))
                .orElseThrow(() -> new NaoAutorizadoException(MENSAGEM_INVALIDA));

        String perfil = Boolean.TRUE.equals(prestador.getAdministrador()) ? "ADMIN" : "FUNCIONARIO";
        return new LoginRespostaDto(UUID.randomUUID().toString(), new LoginRespostaDto.Usuario(
                prestador.getCodPessoa(), prestador.getNome(), prestador.getEmail(), perfil));
    }
}
