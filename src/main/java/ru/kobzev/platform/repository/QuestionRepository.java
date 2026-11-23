package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.Question;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
}
