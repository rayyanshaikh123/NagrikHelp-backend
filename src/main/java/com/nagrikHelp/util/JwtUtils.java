package com.nagrikHelp.util;

import com.nagrikHelp.model.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public final class JwtUtils {
    private JwtUtils() {}

    public static String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    public static List<SimpleGrantedAuthority> authoritiesFor(Role role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
