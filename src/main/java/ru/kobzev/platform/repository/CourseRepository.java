package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.Course;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Integer> {
    @EntityGraph(attributePaths = {"teacher", "category", "modules"})
    Optional<Course> findDetailedById(Integer id);
}
