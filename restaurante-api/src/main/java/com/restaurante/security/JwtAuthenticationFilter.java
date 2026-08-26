package com.restaurante.security;

import com.restaurante.domain.model.UserAccount;
import com.restaurante.domain.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserAccountRepository users;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtService jwtService, UserAccountRepository users,
                                   AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.users = users;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith("Bearer ") || authorization.length() <= "Bearer ".length()) {
            authenticationEntryPoint.commence(request, response,
                new BadCredentialsException("Invalid bearer token"));
            return;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        JwtData data = jwtService.parse(token).orElse(null);
        UserAccount user = data == null ? null : users.findById(data.userId()).orElse(null);
        if (!isValid(data, user)) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response,
                new BadCredentialsException("Invalid JWT"));
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
            user.getEmail(), null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().name())));
        authentication.setDetails(data);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean isValid(JwtData data, UserAccount user) {
        return data != null && user != null && user.isEnabled()
            && user.getEmail().equals(data.email())
            && user.getRole().getName() == data.role()
            && user.getTokenVersion() == data.tokenVersion();
    }
}
