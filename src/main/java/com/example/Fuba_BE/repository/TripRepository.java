package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Integer> { }
