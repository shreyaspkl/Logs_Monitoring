package com.example.logsapi.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class LoginResponseDto {
    String jwt;
    Long userId;

    public LoginResponseDto(String jwt, Long userId) {
        this.jwt = jwt;
        this.userId = userId;
    }
}
