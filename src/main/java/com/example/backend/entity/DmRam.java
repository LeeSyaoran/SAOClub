package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "dm_ram")
public class DmRam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ram_id")
    private Integer ramId;

    @Column(name = "dung_luong", length = 50, nullable = false, unique = true)
    private String dungLuong;
}
