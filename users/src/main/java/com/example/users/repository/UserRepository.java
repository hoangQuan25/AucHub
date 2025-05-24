package com.example.users.repository;

import com.example.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> { // Primary key is String

    // Optional: Add custom find methods if needed later
    // Optional<User> findByEmail(String email);
     Optional<User> findByUsername(String username);

}
