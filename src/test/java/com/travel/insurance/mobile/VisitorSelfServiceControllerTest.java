package com.travel.insurance.mobile;

import com.travel.insurance.preauthorization.upload.PreauthorizationUploadService;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.claim.ClaimController;
import com.travel.insurance.claim.ClaimExcelWriter;
import com.travel.insurance.claim.ClaimService;
import com.travel.insurance.common.util.AuthenticatedUser;
import com.travel.insurance.config.SecurityConfig;
import com.travel.insurance.preauthorization.PreauthorizationController;
import com.travel.insurance.preauthorization.PreauthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Visitor self-service endpoints plus the URL rules that keep visitor tokens off the staff-wide claim and
 * pre-authorization endpoints. Imports {@link SecurityConfig} so those rules are the real ones.
 */
@WebMvcTest({VisitorSelfServiceController.class, ClaimController.class, PreauthorizationController.class})
@Import(SecurityConfig.class)
class VisitorSelfServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaimService claimService;

    @MockBean
    private ClaimExcelWriter claimExcelWriter;

    @MockBean
    private PreauthorizationUploadService preauthorizationUploadService;

    @MockBean
    private PreauthorizationService preauthorizationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID visitorId = UUID.randomUUID();

    /** Mirrors JwtAuthenticationFilter for a visitor token: principal userId = sub = visitor id. */
    private RequestPostProcessor visitor() {
        AuthenticatedUser principal = new AuthenticatedUser(visitorId, null, "VISITOR");
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_VISITOR"))));
    }

    @Test
    void visitorListsOwnClaimsNewestFirst() throws Exception {
        when(claimService.listByVisitor(eq(visitorId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/mobile/me/claims").with(visitor()))
                .andExpect(status().isOk());

        verify(claimService).listByVisitor(eq(visitorId), argThat((Pageable p) -> {
            Sort.Order order = p.getSort().getOrderFor("createdDate");
            return order != null && order.isDescending();
        }));
    }

    @Test
    void visitorListsOwnPreauthorizations() throws Exception {
        when(preauthorizationService.listByVisitor(eq(visitorId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/mobile/me/preauthorizations").with(visitor()))
                .andExpect(status().isOk());

        verify(preauthorizationService).listByVisitor(eq(visitorId), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void staffCannotUseVisitorEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/me/claims")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/mobile/me/preauthorizations")).andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotUseVisitorEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/me/claims")).andExpect(status().isUnauthorized());
    }

    @Test
    void visitorCannotListAllClaimsOrPreauthorizations() throws Exception {
        mockMvc.perform(get("/api/v1/claims").with(visitor())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/claims/{id}", UUID.randomUUID()).with(visitor()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/claims/{id}/documents", UUID.randomUUID()).with(visitor()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/preauthorizations").with(visitor())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/preauthorizations/{id}", UUID.randomUUID()).with(visitor()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "INSURER_USER")
    void staffCanStillListClaims() throws Exception {
        when(claimService.list(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/claims")).andExpect(status().isOk());
    }
}
