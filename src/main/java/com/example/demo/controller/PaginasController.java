package com.example.demo.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrega as duas páginas que ficam na raiz de src/main/resources.
 * (As demais telas ficam em /templates/** e /static/**, servidas pelo WebConfig.)
 */
@RestController
public class PaginasController {

    @GetMapping("/index.html")
    public ResponseEntity<Resource> inicio() {
        return pagina("index.html");
    }

    @GetMapping("/login.html")
    public ResponseEntity<Resource> login() {
        return pagina("login.html");
    }

    private ResponseEntity<Resource> pagina(String arquivo) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(new ClassPathResource(arquivo));
    }
}
