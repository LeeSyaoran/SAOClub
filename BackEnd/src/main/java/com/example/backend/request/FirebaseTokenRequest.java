package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseTokenRequest {

    @NotBlank(message = "Token không được để trống")
    private String idToken;

    // 'google' or 'facebook'
    private String provider;
}
