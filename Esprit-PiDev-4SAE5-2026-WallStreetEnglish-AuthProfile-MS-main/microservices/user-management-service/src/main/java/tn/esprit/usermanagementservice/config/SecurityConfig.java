package tn.esprit.usermanagementservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/from-auth").permitAll()
                        .requestMatchers("/api/users/record-login").permitAll()
                        .requestMatchers("/api/users/record-logout").permitAll()
                        .requestMatchers("/api/users/sync-from-auth").permitAll()
                        .requestMatchers("/api/users/test").permitAll()
                        .requestMatchers("/api/users/stats/**").permitAll()
                        .requestMatchers("/api/users/recent-logins").permitAll()
                        .requestMatchers("/api/users/recent-logins-formatted").permitAll()
                        .requestMatchers("/api/users/logins/today").permitAll()
                        .requestMatchers("/api/users/sessions/active-count").permitAll()
                        .requestMatchers("/api/users/logins/suspicious-count").permitAll()
                        .requestMatchers("/api/users/active-sessions").permitAll()
                        .requestMatchers("/api/users/statistics").permitAll()
                        .requestMatchers("/api/users/reactivate/**").permitAll()
                        .requestMatchers("/api/users/reactivate-request").permitAll()
                        .requestMatchers("/api/users/check-blocked/**").permitAll()
                        .requestMatchers("/api/users/unblock/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/ws/info").permitAll()
                        .requestMatchers("/topic/**").permitAll()
                        .requestMatchers("/app/**").permitAll()
                        .requestMatchers("/api/users/email/**").authenticated()
                        .requestMatchers("/api/users/profile/**").authenticated()
                        .requestMatchers("/api/users/force-logout/**").authenticated()
                        .requestMatchers("/api/users/{id}").authenticated()
                        .requestMatchers("/api/users").authenticated()
                        .requestMatchers("/api/users/block/**").authenticated()
                        .requestMatchers("/api/users/send-reactivation-email/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = issuerUri + "/protocol/openid-connect/certs";
        return NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .build();
    }
}