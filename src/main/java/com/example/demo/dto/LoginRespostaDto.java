package com.example.demo.dto;

/**
 * Resposta de POST /auth/login, no formato que o front-end (autenticacao.js) espera:
 * { "token": "...", "usuario": { "id": 1, "nome": "...", "email": "...", "perfil": "ADMIN" } }
 */
public record LoginRespostaDto(String token, Usuario usuario) {

    /** perfil: "ADMIN" (prestador administrador) ou "FUNCIONARIO". */
    public record Usuario(Integer id, String nome, String email, String perfil) {
    }
}
