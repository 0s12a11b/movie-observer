package com.movieobserver.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "movie_cast")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MovieCast {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private MovieCastId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("movieId")
    @JoinColumn(name = "movie_id", nullable = false)
    @ToString.Exclude
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("personId")
    @JoinColumn(name = "person_id", nullable = false)
    @ToString.Exclude
    private Person person;
}
