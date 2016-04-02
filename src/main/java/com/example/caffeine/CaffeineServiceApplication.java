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

import java.nio.charset.StandardCharsets;
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
        web.ignoring().antMatchers("/caffeine/**");
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

    LoadingCache<String, String> credentials = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(3, TimeUnit.DAYS)
            .build(serviceId -> {
                List<Map<String, Object>> credential = jdbc.queryForList("SELECT username, password FROM credentials WHERE service_id = ?", serviceId);
                return credential.size() == 0 ? null : Base64.getEncoder()
                        .encodeToString((credential.get(0).get("username") + ":" + credential.get(0).get("password")).getBytes(StandardCharsets.UTF_8));
            });
    ConcurrentMap<String, Cache<String, String>> caches = new ConcurrentHashMap<>();

    @RequestMapping(path = "credentials", method = RequestMethod.POST)
    ResponseEntity createCredentials(@RequestParam("service_id") String serviceId,
                                     @RequestParam("expire_second") int expireSecond,
                                     @RequestParam("maximum_size") int maximumSize) {
        String username = UUID.randomUUID().toString();
        String password = UUID.randomUUID().toString();
        return tx.execute(s -> {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM credentials WHERE service_id = ?", Integer.class, serviceId) > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            jdbc.update("INSERT INTO credentials(service_id, username, password) VALUES(?, ?, ?)", serviceId, username, password);
            jdbc.update("INSERT INTO plan(service_id, expire_second, maximum_size) VALUES(?, ?, ?)", serviceId, expireSecond, maximumSize);

            return ResponseEntity.ok(new HashMap<String, String>() {
                {
                    put("username", username);
                    put("password", password);
                }
            });
        });
    }

    @RequestMapping(path = "credentials/{service_id}", method = RequestMethod.DELETE)
    ResponseEntity deleteCredentials(@PathVariable("service_id") String serviceId) {
        return tx.execute(s -> {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM credentials WHERE service_id = ?", Integer.class, serviceId) == 0) {
                return ResponseEntity.notFound().build();
            }

            credentials.invalidate(serviceId);
            caches.remove(serviceId);

            jdbc.update("DELETE FROM plan WHERE service_id = ?", serviceId);
            jdbc.update("DELETE FROM credentials WHERE service_id = ?", serviceId);

            return ResponseEntity.noContent().build();
        });
    }


    @RequestMapping(path = "caffeine/{service_id}/{key}", method = RequestMethod.GET)
    ResponseEntity get(@PathVariable("service_id") String serviceId,
                       @PathVariable("key") String key,
                       @RequestHeader("Authorization") String authorization) {
        if (!authorization.replace("Basic ", "").equals(credentials.get(serviceId))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String value = findCacheByServiceId(serviceId).getIfPresent(key);
        return value == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(value);
    }

    @RequestMapping(path = "caffeine/{service_id}/{key}", method = RequestMethod.PUT)
    ResponseEntity put(@PathVariable("service_id") String serviceId,
                       @PathVariable("key") String key,
                       @RequestBody String body,
                       @RequestHeader("Authorization") String authorization) {
        if (!authorization.replace("Basic ", "").equals(credentials.get(serviceId))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        findCacheByServiceId(serviceId).put(key, body);
        return ResponseEntity.ok().build();
    }

    Cache<String, String> findCacheByServiceId(String serviceId) {
        return caches.computeIfAbsent(serviceId, id -> {
            Map<String, Object> plan = jdbc.queryForMap("SELECT expire_second, maximum_size FROM plan WHERE service_id = ?", id);
            return Caffeine.newBuilder()
                    .expireAfterAccess((Integer) plan.get("expire_second"), TimeUnit.SECONDS)
                    .maximumSize((Integer) plan.get("maximum_size"))
                    .build();
        });
    }

}
