package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.Tag;

public interface TagRepository extends JpaRepository<Tag, Integer> {
}
