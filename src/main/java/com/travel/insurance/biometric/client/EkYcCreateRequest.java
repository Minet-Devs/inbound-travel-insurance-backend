package com.travel.insurance.biometric.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EkYcCreateRequest(
        @JsonProperty("notification_callback_url") String notificationCallbackUrl,
        @JsonProperty("reason") String reason,
        @JsonProperty("relying_party_agent_id_number") String relyingPartyAgentIdNumber,
        @JsonProperty("relying_party_agent_id_type") String relyingPartyAgentIdType,
        @JsonProperty("subject_id_number") String subjectIdNumber,
        @JsonProperty("subject_id_type") String subjectIdType,
        @JsonProperty("relying_party_request_id") String relyingPartyRequestId,
        @JsonProperty("expires_in_seconds") Integer expiresInSeconds,
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("total_attempts") Integer totalAttempts,
        @JsonProperty("request_mode") String requestMode,
        @JsonProperty("poor_quality_result_attempts") Integer poorQualityResultAttempts,
        @JsonProperty("location_name") String locationName,
        @JsonProperty("workstation_id") String workstationId,
        @JsonProperty("device_id") String deviceId,
        @JsonProperty("device_name") String deviceName
) {
}
