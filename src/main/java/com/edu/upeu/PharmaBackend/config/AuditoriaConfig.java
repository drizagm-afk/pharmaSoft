package com.edu.upeu.PharmaBackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditor")
public class AuditoriaConfig {
    @Bean
    public AuditorAware<String> auditor() {
        return () -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            String usuario = authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken)
                    ? authentication.getName() : "sistema";
            return Optional.of(usuario);
        };
    }
}
