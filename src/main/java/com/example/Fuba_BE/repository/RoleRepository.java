package com.example.Fuba_BE.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    
    Optional<Role> findByRoleName(String roleName);
}
