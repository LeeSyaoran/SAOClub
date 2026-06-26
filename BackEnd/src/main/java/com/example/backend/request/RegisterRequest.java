package com.example.backend.request;

import lombok.Data;

@Data
public class RegisterRequest {

    private String hoTen;

    private String soDienThoai;

    private String email;

    private String username;

    private String password;

}