package com.hero.bikestore.repository;

import com.hero.bikestore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByKeycloakUserId(String keycloakId);

    boolean existsByEmail(String email);

}
