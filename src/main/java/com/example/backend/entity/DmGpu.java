package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "dm_gpu")
public class DmGpu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gpu_id")
    private Integer gpuId;

    @Column(name = "ten_gpu", length = 100, nullable = false, unique = true)
    private String tenGpu;
}
