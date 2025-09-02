package ru.netology.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
    boolean existsByLogin(String login);
}