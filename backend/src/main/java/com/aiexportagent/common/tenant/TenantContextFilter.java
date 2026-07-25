package com.aiexportagent.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Injects a hardcoded dev tenant id into {@link TenantContext} for every
 * request. Stands in for real tenant login/auth, which does not exist yet
 * (see CLAUDE.md Sprint 1 status). Sourced from the {@code app.dev-tenant-id}
 * property, itself backed by the {@code DEV_TENANT_ID} env var.
 */
@Component
@Order(1)
public class TenantContextFilter extends OncePerRequestFilter {

    @Value("${app.dev-tenant-id}")
    private String devTenantId;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            TenantContext.set(UUID.fromString(devTenantId));
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
