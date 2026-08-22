package com.devvault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security configuration defining core security infrastructure beans.
 */
@Configuration
public class SecurityConfig {

    /**
     * Exposes the BCryptPasswordEncoder as a Spring bean.
     *
     * @return a PasswordEncoder instance using BCrypt hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
