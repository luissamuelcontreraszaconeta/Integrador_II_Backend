package com.exportrace.config;

import com.exportrace.entity.Permission;
import com.exportrace.entity.User;
import com.exportrace.entity.UserModuleAccess;
import com.exportrace.repository.UserModuleAccessRepository;
import com.exportrace.repository.UserRepository;
import com.exportrace.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserModuleAccessRepository userModuleAccessRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Optional<User> userOpt = userRepository.findByEmail(email);

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        // Enforce active status immediately
                        if (Boolean.TRUE.equals(user.getActivo())) {
                            List<GrantedAuthority> authorities = new ArrayList<>();

                            if (user.getRole() != null) {
                                String roleName = user.getRole().getNombre();
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));

                                if (user.getRole().getPermissions() != null) {
                                    for (Permission perm : user.getRole().getPermissions()) {
                                        if (Boolean.TRUE.equals(perm.getActivo())) {
                                            authorities.add(new SimpleGrantedAuthority(perm.getCodigo()));
                                        }
                                    }
                                }
                            }

                            // Add individual module access exceptions (MODULE_<CODE>)
                            List<UserModuleAccess> extraAccesses = userModuleAccessRepository.findByUserIdAndActivoTrue(user.getId());
                            for (UserModuleAccess uma : extraAccesses) {
                                if (uma.getModule() != null) {
                                    authorities.add(new SimpleGrantedAuthority("MODULE_" + uma.getModule().getCodigo()));
                                }
                            }

                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    email, null, authorities);
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
