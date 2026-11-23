package ru.kobzev.platform.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.kobzev.platform.domain.Quiz;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select quiz from Quiz quiz where quiz.id = :id")
    Optional<Quiz> findForUpdate(@Param("id") int id);
}
