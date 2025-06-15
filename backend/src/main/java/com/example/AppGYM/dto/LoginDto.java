package com.example.AppGYM.dto;

import lombok.Data;

@Data
public class LoginDto {
    private String email;
    private String password;
    private String recaptcha;
}