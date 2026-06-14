package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "dm_o_cung")
public class DmOcung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "o_cung_id")
    private Integer oCungId;

    @Column(name = "loai_o_cung", length = 100, nullable = false, unique = true)
    private String loaiOcung;
}
