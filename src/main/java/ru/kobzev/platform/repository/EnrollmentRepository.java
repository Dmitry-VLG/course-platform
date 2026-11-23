package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.Enrollment;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {
    boolean existsByCourseIdAndUserId(Integer courseId, Integer userId);
    Optional<Enrollment> findByCourseIdAndUserId(Integer courseId, Integer userId);
    List<Enrollment> findByCourseIdOrderById(Integer courseId);
    List<Enrollment> findByUserIdOrderById(Integer userId);
}
