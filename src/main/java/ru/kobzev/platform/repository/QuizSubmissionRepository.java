package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.QuizSubmission;
import java.util.List;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Integer> {
    boolean existsByQuizId(int quizId);

    List<QuizSubmission> findByQuizIdOrderById(Integer quizId);
    List<QuizSubmission> findByStudentIdOrderById(Integer studentId);
}
