package ru.netology.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.entity.FileEntity;
import ru.netology.entity.User;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByUserOrderByCreatedDateDesc(User user);
    Optional<FileEntity> findByUserAndFilename(User user, String filename);
    boolean existsByUserAndFilename(User user, String filename);
    void deleteByUserAndFilename(User user, String filename);
}