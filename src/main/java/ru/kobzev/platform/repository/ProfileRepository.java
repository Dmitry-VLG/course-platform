package ru.kobzev.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kobzev.platform.domain.Profile;

public interface ProfileRepository extends JpaRepository<Profile, Integer> {
}
