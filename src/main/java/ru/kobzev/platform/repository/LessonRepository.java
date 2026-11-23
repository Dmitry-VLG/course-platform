package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {
}
