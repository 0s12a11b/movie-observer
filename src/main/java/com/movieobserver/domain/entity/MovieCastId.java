package com.movieobserver.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MovieCastId implements Serializable {

    @Column(name = "movie_id")
    private UUID movieId;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "role_type", length = 50)
    private String roleType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovieCastId that = (MovieCastId) o;
        return Objects.equals(movieId, that.movieId) &&
               Objects.equals(personId, that.personId) &&
               Objects.equals(roleType, that.roleType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(movieId, personId, roleType);
    }
}
