package com.travel.insurance.mobile;

import com.travel.insurance.claim.ClaimService;
import com.travel.insurance.claim.dto.ClaimResponse;
import com.travel.insurance.common.util.SecurityUtils;
import com.travel.insurance.preauthorization.PreauthorizationService;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Mobile-app endpoints for the signed-in visitor's own records. The visitor is always taken from the
 * token ({@code sub} = visitor id, see {@code JwtTokenProvider.createVisitorAccessToken}), never from a
 * request parameter, so a visitor can only ever see their own claims and pre-authorizations.
 * {@code SecurityConfig} restricts {@code /api/v1/mobile/me/**} to {@code VISITOR}.
 */
@RestController
@RequestMapping("/api/v1/mobile/me")
@RequiredArgsConstructor
public class VisitorSelfServiceController {

    private final ClaimService claimService;
    private final PreauthorizationService preauthorizationService;

    @GetMapping("/claims")
    public ResponseEntity<Page<ClaimResponse>> myClaims(
            @PageableDefault(sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(claimService.listByVisitor(currentVisitorId(), pageable));
    }

    @GetMapping("/preauthorizations")
    public ResponseEntity<Page<PreauthorizationResponse>> myPreauthorizations(
            @PageableDefault(sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(preauthorizationService.listByVisitor(currentVisitorId(), pageable));
    }

    private UUID currentVisitorId() {
        return SecurityUtils.currentUserId()
                .orElseThrow(() -> new AccessDeniedException("No authenticated visitor"));
    }
}
