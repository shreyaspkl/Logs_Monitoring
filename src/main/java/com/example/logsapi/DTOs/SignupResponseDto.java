package com.example.logsapi.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SignupResponseDto {
    private long id;
    private String jwt;

    public SignupResponseDto(long id, String jwt) {
        this.id = id;
        this.jwt = jwt;
    }
}
