package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.LoginDto;
import com.example.demo.dto.LoginRespostaDto;
import com.example.demo.service.AutenticacaoService;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {

    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    /** 200 com token e usuário, ou 401 se e-mail/senha estiverem errados. */
    @PostMapping("/login")
    public LoginRespostaDto entrar(@RequestBody LoginDto dados) {
        return service.entrar(dados);
    }
}
