package com.movieobserver.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "persons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;
}
