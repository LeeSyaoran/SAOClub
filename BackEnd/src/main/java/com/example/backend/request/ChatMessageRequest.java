package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatMessageRequest {
    @NotBlank(message = "Nội dung tin nhắn không được trống")
    private String noiDung;

    private String sessionId;
}
