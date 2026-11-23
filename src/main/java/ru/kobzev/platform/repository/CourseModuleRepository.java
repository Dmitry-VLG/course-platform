package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.CourseModule;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Integer> {
}
