package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
