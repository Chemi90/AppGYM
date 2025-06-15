package com.example.AppGYM.dto;

import lombok.Data;

@Data
public class RegisterDto {
    private String email;
    private String password;
    private String confirm;
    private String recaptcha;
}
