package com.travel.insurance.common.util;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, UUID organizationId, Set<String> roles) {
}
