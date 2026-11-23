package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.CourseReview;
import java.util.List;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Integer> {
    List<CourseReview> findByCourseIdOrderById(Integer courseId);
}
