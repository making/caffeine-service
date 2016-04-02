package com.example.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@RestController
public class CaffeineServiceApplication extends WebSecurityConfigurerAdapter {

    public static void main(String[] args) {
        SpringApplication.run(CaffeineServiceApplication.class, args);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/caffeine/*/*");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/**").authenticated()
                .and()
                .httpBasic()
                .and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    TransactionTemplate tx;

    LoadingCache<String, LoadingCache<String, String>> credentials = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(3, TimeUnit.DAYS)
            .build(serviceId -> Caffeine.newBuilder()
                    .maximumSize(100)
                    .expireAfterAccess(3, TimeUnit.DAYS)
                    .build(username -> jdbc.queryForList("SELECT password FROM credentials WHERE service_id = ? AND username = ?",
                            String.class, serviceId, username).stream().findFirst().orElse(null)));
    ConcurrentMap<String, Cache<String, String>> caches = new ConcurrentHashMap<>();

    @RequestMapping(path = "caffeine", method = RequestMethod.POST)
    ResponseEntity createCache(@RequestParam("service_id") String serviceId,
                               @RequestParam("expire_second") int expireSecond,
                               @RequestParam("maximum_size") int maximumSize) {
        return tx.execute(s -> {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM caches WHERE service_id = ?", Integer.class, serviceId) > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            jdbc.update("INSERT INTO caches(service_id, expire_second, maximum_size) VALUES(?, ?, ?)", serviceId, expireSecond, maximumSize);
            return ResponseEntity.status(HttpStatus.CREATED).body("Created Cache");
        });
    }

    @RequestMapping(path = "caffeine/{service_id}", method = RequestMethod.DELETE)
    ResponseEntity deleteCache(@PathVariable("service_id") String serviceId) {
        return tx.execute(s -> {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM caches WHERE service_id = ?", Integer.class, serviceId) == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            credentials.invalidate(serviceId);
            caches.remove(serviceId);
            jdbc.update("DELETE FROM credentials WHERE service_id = ?", serviceId);
            jdbc.update("DELETE FROM caches WHERE service_id = ?", serviceId);
            return ResponseEntity.noContent().build();
        });
    }

    @RequestMapping(path = "credentials", method = RequestMethod.POST)
    ResponseEntity createCredentials(@RequestParam("service_id") String serviceId,
                                     @RequestParam("username") String username) {
        String password = UUID.randomUUID().toString();
        return tx.execute(s -> {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM caches WHERE service_id = ?", Integer.class, serviceId) == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            jdbc.update("INSERT INTO credentials(service_id, username, password) VALUES(?, ?, ?)", serviceId, username, password);
            return ResponseEntity.status(HttpStatus.CREATED).body(new LinkedHashMap<String, String>() {
                {
                    put("username", username);
                    put("password", password);
                }
            });
        });
    }

    @RequestMapping(path = "credentials/{service_id}/{username}", method = RequestMethod.DELETE)
    ResponseEntity deleteCredentials(@PathVariable("service_id") String serviceId,
                                     @PathVariable("username") String username) {
        return tx.execute(s -> {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM credentials WHERE service_id = ?", Integer.class, serviceId) == 0) {
                return ResponseEntity.notFound().build();
            }
            if (credentials.get(serviceId) != null) {
                credentials.get(serviceId).invalidate(username);
            }
            jdbc.update("DELETE FROM credentials WHERE service_id = ? AND username = ?", serviceId, username);
            return ResponseEntity.noContent().build();
        });
    }

    @RequestMapping(path = "caffeine/{service_id}/{key}", method = RequestMethod.GET)
    ResponseEntity getValue(@PathVariable("service_id") String serviceId,
                            @PathVariable("key") String key,
                            @RequestHeader("Authorization") String authorization) {
        if (!authorize(serviceId, authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String value = cache(serviceId).getIfPresent(key);
        return value == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(value);
    }

    @RequestMapping(path = "caffeine/{service_id}/{key}", method = RequestMethod.PUT)
    ResponseEntity putValue(@PathVariable("service_id") String serviceId,
                            @PathVariable("key") String key,
                            @RequestBody String body,
                            @RequestHeader("Authorization") String authorization) {
        if (!authorize(serviceId, authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        cache(serviceId).put(key, body);
        return ResponseEntity.status(HttpStatus.CREATED).body("Put");
    }

    @RequestMapping(path = "caffeine/{service_id}/{key}", method = RequestMethod.DELETE)
    ResponseEntity deleteValue(@PathVariable("service_id") String serviceId,
                               @PathVariable("key") String key,
                               @RequestHeader("Authorization") String authorization) {
        if (!authorize(serviceId, authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        cache(serviceId).invalidate(key);
        return ResponseEntity.noContent().build();
    }

    boolean authorize(String serviceId, String authorization) {
        String[] s = new String(Base64.getDecoder().decode(authorization.replace("Basic ", ""))).split(":");
        String username = s[0], password = s[1];
        LoadingCache<String, String> credential = credentials.get(serviceId);
        return credential != null && password.equals(credential.get(username));
    }

    Cache<String, String> cache(String serviceId) {
        return caches.computeIfAbsent(serviceId, id -> {
            Map<String, Object> plan = jdbc.queryForMap("SELECT expire_second, maximum_size FROM caches WHERE service_id = ?", id);
            return Caffeine.newBuilder()
                    .expireAfterAccess((Integer) plan.get("expire_second"), TimeUnit.SECONDS)
                    .maximumSize((Integer) plan.get("maximum_size"))
                    .build();
        });
    }
}
