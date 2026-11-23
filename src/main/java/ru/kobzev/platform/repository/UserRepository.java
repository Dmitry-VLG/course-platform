package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.User;

public interface UserRepository extends JpaRepository<User, Integer> {
}
