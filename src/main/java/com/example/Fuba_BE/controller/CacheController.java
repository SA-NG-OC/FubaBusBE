package com.example.Fuba_BE.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.payload.ApiResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for Cache Management (Admin only)
 * Provides endpoints to view cache statistics and clear caches
 */
@RestController
@RequestMapping("/admin/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheController {

    private final CacheManager cacheManager;

    /**
     * Get all cache statistics
     * GET /admin/cache/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStats() {
        log.info("📊 Fetching cache statistics");

        Map<String, Object> allStats = new HashMap<>();

        for (String cacheName : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);
            if (springCache instanceof CaffeineCache caffeineCache) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                CacheStats stats = nativeCache.stats();

                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("size", nativeCache.estimatedSize());
                cacheInfo.put("hitCount", stats.hitCount());
                cacheInfo.put("missCount", stats.missCount());
                cacheInfo.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
                cacheInfo.put("evictionCount", stats.evictionCount());
                cacheInfo.put("loadSuccessCount", stats.loadSuccessCount());
                cacheInfo.put("loadFailureCount", stats.loadFailureCount());
                cacheInfo.put("averageLoadPenalty", String.format("%.2f ms", stats.averageLoadPenalty() / 1_000_000.0));

                allStats.put(cacheName, cacheInfo);
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Cache statistics retrieved", allStats));
    }

    /**
     * Get summary of all caches
     * GET /admin/cache/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheSummary() {
        log.info("📊 Fetching cache summary");

        Map<String, Object> summary = new HashMap<>();
        long totalHits = 0;
        long totalMisses = 0;
        long totalSize = 0;

        for (String cacheName : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);
            if (springCache instanceof CaffeineCache caffeineCache) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                CacheStats stats = nativeCache.stats();

                totalHits += stats.hitCount();
                totalMisses += stats.missCount();
                totalSize += nativeCache.estimatedSize();
            }
        }

        summary.put("totalCaches", cacheManager.getCacheNames().size());
        summary.put("cacheNames", cacheManager.getCacheNames());
        summary.put("totalHits", totalHits);
        summary.put("totalMisses", totalMisses);
        summary.put("totalEntries", totalSize);
        summary.put("overallHitRate", totalHits + totalMisses > 0
                ? String.format("%.2f%%", (double) totalHits / (totalHits + totalMisses) * 100)
                : "N/A");

        return ResponseEntity.ok(ApiResponse.success("Cache summary retrieved", summary));
    }

    /**
     * Clear a specific cache
     * DELETE /admin/cache/{cacheName}
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<ApiResponse<String>> clearCache(@PathVariable String cacheName) {
        log.info("🗑 Clearing cache: {}", cacheName);

        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cache not found: " + cacheName, "CACHE_NOT_FOUND"));
        }

        cache.clear();
        log.info("✅ Cache cleared: {}", cacheName);

        return ResponseEntity.ok(ApiResponse.success("Cache cleared successfully", cacheName));
    }

    /**
     * Clear all caches
     * DELETE /admin/cache/all
     */
    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<String>> clearAllCaches() {
        log.info("🗑 Clearing all caches");

        int clearedCount = 0;
        for (String cacheName : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                clearedCount++;
            }
        }

        log.info("✅ Cleared {} caches", clearedCount);
        return ResponseEntity.ok(ApiResponse.success("All caches cleared",
                "Cleared " + clearedCount + " caches"));
    }

    /**
     * Evict specific cache entries (for locations, routes, etc.)
     * POST /admin/cache/evict
     */
    @PostMapping("/evict")
    public ResponseEntity<ApiResponse<String>> evictCacheEntries(
            @RequestParam String cacheName,
            @RequestParam(required = false) String key) {

        log.info("🗑 Evicting from cache: {}, key: {}", cacheName, key);

        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cache not found: " + cacheName, "CACHE_NOT_FOUND"));
        }

        if (key != null && !key.isEmpty()) {
            cache.evict(key);
            log.info("✅ Evicted key '{}' from cache '{}'", key, cacheName);
            return ResponseEntity.ok(ApiResponse.success("Cache entry evicted",
                    "Evicted key '" + key + "' from cache '" + cacheName + "'"));
        } else {
            cache.clear();
            log.info("✅ Cleared entire cache '{}'", cacheName);
            return ResponseEntity.ok(ApiResponse.success("Cache cleared",
                    "Cleared entire cache '" + cacheName + "'"));
        }
    }

    /**
     * Warm up caches by pre-loading frequently accessed data
     * POST /admin/cache/warmup
     */
    @PostMapping("/warmup")
    public ResponseEntity<ApiResponse<String>> warmupCaches() {
        log.info("🔥 Warming up caches...");

        // Note: This is a placeholder. Actual warmup would call the cached services
        // to populate the caches. For example:
        // locationService.getAllLocations(); // This populates CACHE_LOCATIONS_ALL
        // vehicleTypeService.getAllVehicleTypesForSelection(); // This populates
        // CACHE_VEHICLE_TYPES_ALL
        // routeService.getAllRoutesForSelection(); // This populates
        // CACHE_ROUTE_SELECTIONS

        return ResponseEntity.ok(ApiResponse.success("Cache warmup initiated",
                "Caches will be populated on first access"));
    }
}
