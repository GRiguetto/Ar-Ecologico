package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Corpo de POST /auth/login. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginDto {
    private String email;

    private String senha;
}
