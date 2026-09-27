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
    private String avatarUrl;
    private String diaChi;

    public LoginResponse(Integer id, String hoTen, String username, String soDienThoai, String email, String role, String token, String avatarUrl) {
        this(id, hoTen, username, soDienThoai, email, role, token, avatarUrl, null);
    }
}
