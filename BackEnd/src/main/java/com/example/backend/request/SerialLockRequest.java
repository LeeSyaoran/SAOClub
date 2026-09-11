package com.example.backend.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class SerialLockRequest {
    @NotEmpty(message = "Danh sach serial rong")
    private List<Integer> chiTietIds;

    @NotNull(message = "Session ID rong")
    private String sessionId;

    @NotNull(message = "NhanVien ID rong")
    private Integer nhanVienId;
}
