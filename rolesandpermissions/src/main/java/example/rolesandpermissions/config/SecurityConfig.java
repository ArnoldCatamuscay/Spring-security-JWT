package example.rolesandpermissions.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.util.Arrays;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String[] WHITE_LIST_URL = {"/auth/**"};
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final LogoutHandler logoutHandler;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
			.cors(Customizer.withDefaults()) // by default uses a Bean by the name of corsConfigurationSource
            .csrf((csrf) -> csrf.disable()) //Cross-Site Request Forgery, con formularios no deshabilitar
            .authorizeHttpRequests(req -> {
                req
                    .requestMatchers(WHITE_LIST_URL).permitAll() // los endpoints públicos
                    .requestMatchers("/admin/**").hasRole("ADMIN") /* hasAnyRole("", "") */
                    .requestMatchers(GET, "/admin/**").hasAnyAuthority("admin:read"/*, "user:read"*/) //un usuario normal solo podra acceder a GET
                    .requestMatchers(POST, "/admin/**").hasAnyAuthority("admin:create")
                    .requestMatchers(PUT, "/admin/**").hasAnyAuthority("admin:update")
                    .requestMatchers(DELETE, "/admin/**").hasAnyAuthority("admin:delete")
                    .anyRequest().authenticated(); // cualquier otro endpoint debe estar autenticado el usuario
            })
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS)) // ALWAYS. IF_REQUIRED, NEVER, STATELESS 
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .logout(logout ->
                    logout.logoutUrl("/api/v1/auth/logout")
                            .addLogoutHandler(logoutHandler)
                            .logoutSuccessHandler((request, response, authentication) -> SecurityContextHolder.clearContext())
            );
        return http.build();
    }

    @Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
		configuration.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
