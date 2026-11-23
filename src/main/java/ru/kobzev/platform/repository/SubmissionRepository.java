package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.Submission;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Integer> {
    boolean existsByAssignmentIdAndStudentId(Integer assignmentId, Integer studentId);
    List<Submission> findByAssignmentIdOrderById(Integer assignmentId);
    List<Submission> findByStudentIdOrderById(Integer studentId);
}
