package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Integer> {
}
