package com.mindfultalk.growthlab.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@Component
public class SwaggerAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Check if request is for Swagger UI or API docs
        if (isSwaggerRequest(requestURI)) {
            HttpSession session = request.getSession(false);

            // Check if user is authenticated as SuperAdmin
            if (session == null ||
                session.getAttribute("superAdminAuthenticated") == null ||
                !(Boolean) session.getAttribute("superAdminAuthenticated")) {

                // Redirect to login page
                response.sendRedirect("/api/v1/swagger/login");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSwaggerRequest(String requestURI) {
        return requestURI.startsWith("/swagger-ui") ||
               requestURI.startsWith("/v3/api-docs") ||
               requestURI.equals("/swagger-ui.html") ||
               requestURI.startsWith("/webjars/swagger-ui");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Don't filter swagger auth endpoints and static resources
        return path.startsWith("/api/v1/swagger/login") ||
               path.startsWith("/api/v1/swagger/authenticate") ||
               path.startsWith("/api/v1/swagger/logout") ||
               path.startsWith("/api/v1/swagger/unauthorized") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/static/");
    }
}