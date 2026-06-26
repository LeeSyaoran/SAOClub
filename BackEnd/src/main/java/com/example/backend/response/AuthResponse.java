package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private boolean success;

    private String message;

    private Object data;

}