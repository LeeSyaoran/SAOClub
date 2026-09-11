package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SerialLockResponse {
    private boolean success;
    private int lockedCount;
    private List<Integer> failedIds;
    private String message;
}
