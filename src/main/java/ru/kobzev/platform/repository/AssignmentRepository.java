package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.Assignment;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {
    @Query("""
           select a from Assignment a where a.lesson.module.course.id in
             (select e.course.id from Enrollment e where e.user.id = :studentId)
           order by a.dueDate, a.id
           """)
    List<Assignment> findForStudent(Integer studentId);
}
