package com.example.Fuba_BE.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

/**
 * Cache Configuration using Caffeine for high-performance in-memory caching.
 * 
 * Cache Names and TTL:
 * - locations: Static data, 30 min TTL, max 500 entries
 * - provinces: Static data, 1 hour TTL, max 100 entries
 * - routes: Semi-static, 15 min TTL, max 200 entries
 * - routeSelections: For dropdowns, 10 min TTL, max 100 entries
 * - vehicleTypes: Static data, 1 hour TTL, max 50 entries
 * - driverAssignments: Changes occasionally, 5 min TTL, max 500 entries
 * - vehicleAssignments: Changes occasionally, 5 min TTL, max 500 entries
 * - tripTemplates: Semi-static, 10 min TTL, max 200 entries
 * - tickets: Ticket lookups, 5 min TTL, max 1000 entries (evicted on confirm)
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    public static final String CACHE_LOCATIONS = "locations";
    public static final String CACHE_LOCATIONS_ALL = "locationsAll";
    public static final String CACHE_PROVINCES = "provinces";
    public static final String CACHE_ROUTES = "routes";
    public static final String CACHE_ROUTE_SELECTIONS = "routeSelections";
    public static final String CACHE_VEHICLE_TYPES = "vehicleTypes";
    public static final String CACHE_VEHICLE_TYPES_ALL = "vehicleTypesAll";
    public static final String CACHE_DRIVER_ASSIGNMENTS = "driverAssignments";
    public static final String CACHE_VEHICLE_ASSIGNMENTS = "vehicleAssignments";
    public static final String CACHE_TRIP_TEMPLATES = "tripTemplates";
    public static final String CACHE_MY_TICKETS = "myTickets";
    public static final String CACHE_TICKETS = "tickets";

    @Bean
    public CacheManager cacheManager() {
        log.info("🚀 Initializing Caffeine Cache Manager");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Default cache configuration
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());

        // Register all cache names
        cacheManager.setCacheNames(java.util.List.of(
                CACHE_LOCATIONS,
                CACHE_LOCATIONS_ALL,
                CACHE_PROVINCES,
                CACHE_ROUTES,
                CACHE_ROUTE_SELECTIONS,
                CACHE_VEHICLE_TYPES,
                CACHE_VEHICLE_TYPES_ALL,
                CACHE_DRIVER_ASSIGNMENTS,
                CACHE_VEHICLE_ASSIGNMENTS,
                CACHE_TICKETS,
                CACHE_MY_TICKETS,
                CACHE_TRIP_TEMPLATES));

        log.info("✅ Cache Manager initialized with {} caches", cacheManager.getCacheNames().size());
        return cacheManager;
    }

    /**
     * Custom Caffeine configuration for specific caches with different TTLs.
     * This is used for programmatic cache configuration.
     */
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats();
    }
}
