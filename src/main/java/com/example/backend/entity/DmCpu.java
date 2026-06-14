package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "dm_cpu")
public class DmCpu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cpu_id")
    private Integer cpuId;

    @Column(name = "ten_cpu", length = 100, nullable = false, unique = true)
    private String tenCpu;
}
