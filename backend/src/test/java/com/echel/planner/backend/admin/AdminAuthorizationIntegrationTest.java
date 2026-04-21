package com.echel.planner.backend.admin;

import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the access-control contract on {@code /api/v1/admin/**}: anonymous
 * callers get 401, authenticated non-admin callers get 403, and {@code ADMIN}
 * callers pass the authorization check. Exercises {@link AdminUserController}
 * as a representative admin endpoint — the rule is applied at the path level in
 * {@link SecurityConfig}, so one endpoint is enough to lock the behaviour down.
 */
@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
class AdminAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private AppUserRepository appUserRepository;

    @Test
    @WithAnonymousUser
    void anonymousRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void authenticatedUserWithoutAdminRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authenticatedAdmin_isAllowed() throws Exception {
        when(adminUserService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk());
    }
}
