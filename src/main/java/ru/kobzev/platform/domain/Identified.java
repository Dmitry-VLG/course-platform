package ru.kobzev.platform.domain;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class Identified {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    public Integer getId() {
        return id;
    }
}
