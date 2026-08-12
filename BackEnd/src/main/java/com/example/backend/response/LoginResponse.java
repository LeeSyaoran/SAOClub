package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LoginResponse {
    private Integer id;
    private String hoTen;
    private String username;
    private String soDienThoai;
    private String email;
    private String role;
    private String token;
}
