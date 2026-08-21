package com.travel.insurance.common.util;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, UUID organizationId, String role) {
}
