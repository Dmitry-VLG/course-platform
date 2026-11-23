package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.AnswerOption;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Integer> {
}
