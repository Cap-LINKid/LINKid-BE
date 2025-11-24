package com.example.linkid.repository;

import com.example.linkid.domain.Child;
import com.example.linkid.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChildRepository extends JpaRepository<Child, Long> {
    Optional<Child> findFirstByUser(User user);
}