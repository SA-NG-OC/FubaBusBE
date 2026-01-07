package com.example.Fuba_BE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by phone number
     */
    Optional<User> findByPhoneNumber(String phoneNumber);

    /**
     * Find user by email or phone number (for authentication)
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.email = :identifier OR u.phoneNumber = :identifier")
    Optional<User> findByEmailOrPhoneNumber(@Param("identifier") String email, @Param("identifier") String phoneNumber);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if phone number exists
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Find users by role
     */
    @Query("SELECT u FROM User u WHERE u.role.roleId = :roleId")
    List<User> findByRoleId(@Param("roleId") Integer roleId);

    /**
     * Find users by status
     */
    List<User> findByStatus(String status);

    /**
     * Search users by name or email
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchByNameOrEmail(@Param("keyword") String keyword);
}
