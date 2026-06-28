package com.example.backend.request;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MergeOrderRequest {
    private Integer targetId;
    private List<Integer> sourceIds;
}
