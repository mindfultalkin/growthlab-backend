package com.mindfultalk.growthlab.controller;

//import java.util.Collection;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import com.mindfultalk.growthlab.service.*;
//
//@RestController
//@RequestMapping("/api/v1/cache")
//public class CacheController {
//  
//  private final CacheManagementService cacheManagementService;
//  
//  public CacheController(CacheManagementService cacheManagementService) {
//      this.cacheManagementService = cacheManagementService;
//  }
//  
//  @PostMapping("/clear-all")
//  public ResponseEntity<String> clearAllCaches() {
//      try {
//          cacheManagementService.clearAllSpringCaches();
//          return ResponseEntity.ok("All caches cleared successfully");
//      } catch (Exception e) {
//          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//              .body("Error clearing caches: " + e.getMessage());
//      }
//  }
//  
//  @PostMapping("/clear/{cacheName}")
//  public ResponseEntity<String> clearSpecificCache(@PathVariable String cacheName) {
//      try {
//          cacheManagementService.clearCacheByName(cacheName);
//          return ResponseEntity.ok("Cache '" + cacheName + "' cleared successfully");
//      } catch (Exception e) {
//          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//              .body("Error clearing cache: " + e.getMessage());
//      }
//  }
//  
//  @PostMapping("/clear-redis-all")
//  public ResponseEntity<String> clearAllRedisData() {
//      try {
//          cacheManagementService.clearAllRedisData();
//          return ResponseEntity.ok("All Redis data cleared successfully");
//      } catch (Exception e) {
//          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//              .body("Error clearing Redis data: " + e.getMessage());
//      }
//  }
//  
//  @GetMapping("/names")
//  public ResponseEntity<Collection<String>> getAllCacheNames() {
//      return ResponseEntity.ok(cacheManagementService.getAllCacheNames());
//  }
//}