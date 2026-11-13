package com.example.redisdemo.controller;

import org.springframework.data.geo.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Redis GEOSPATIAL
 * ----------------
 * Stores longitude/latitude points and supports radius / bounding-box queries.
 * Great for "find nearest stores/drivers". Commands: GEOADD, GEODIST, GEOHASH,
 * GEOPOS, GEORADIUS, GEOSEARCH.
 */
@RestController
@RequestMapping("/api/redis/geo")
public class RedisGeoController {

    private final GeoOperations<String, Object> ops;

    public RedisGeoController(RedisTemplate<String, Object> template) {
        this.ops = template.opsForGeo();
    }

    @PostMapping("/{key}")
    public Long geoadd(@PathVariable String key, @RequestParam double lon,
                       @RequestParam double lat, @RequestBody Object member) {
        return ops.add(key, new Point(lon, lat), member);            // GEOADD
    }

    @GetMapping("/{key}/pos/{member}")
    public List<Point> geopos(@PathVariable String key, @PathVariable Object member) {
        return ops.position(key, member);                            // GEOPOS
    }

    @GetMapping("/{key}/dist")
    public Distance geodist(@PathVariable String key, @RequestParam Object m1,
                            @RequestParam Object m2) {
        return ops.distance(key, m1, m2, Metrics.KILOMETERS);        // GEODIST
    }

    @GetMapping("/{key}/radius")
    public Object radius(@PathVariable String key,
                        @RequestParam double lon, @RequestParam double lat,
                        @RequestParam(defaultValue = "10") double km) {
        Circle circle = new Circle(new Point(lon, lat), new Distance(km, Metrics.KILOMETERS));
        return ops.radius(key, circle);                             // GEORADIUS
    }

    @GetMapping("/{key}/hash/{member}")
    public List<String> geohash(@PathVariable String key, @PathVariable Object member) {
        return ops.hash(key, member);                               // GEOHASH
    }
}
