package com.user.repositories;

import com.user.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByIdAndEmail(Long id, String email);

    Optional<User> findByEmail(String email);

    List<User> findByEstado(String estado);
}