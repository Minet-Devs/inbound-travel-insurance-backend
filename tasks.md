# Tasks — Regulatory Domain Model Updates

Derived from the `backend-architecture.md` changes made to align `Policy`,
`Benefit`, and `Visitor` with the Ministry of Health's Mandatory Inbound
Travel Health Insurance framework (see `travel-insurance.md`). Each task
follows the repo's [Definition of done](CLAUDE.md#definition-of-done): tests
in the matching style, run before marking done, and `backend-architecture.md`
kept in sync (already updated as the basis for this list — revisit it if an
implementation detail below turns out to need a different shape).

## 1. Policy: cover-period types — done

- [x] Add `PolicyType` enum (`SINGLE_ENTRY_UP_TO_30_DAYS`,
      `SINGLE_ENTRY_31_TO_60_DAYS`, `IPMI_61_DAYS_TO_12_MONTHS`) in the
      `policy` package, string-mapped like `PolicyStatus`.
- [x] Add `policyType` column to `Policy` (Flyway migration,
      `V012__add_policy_type_to_policies.sql`), non-nullable.
- [x] Add `PolicyRequest.policyType` (required, `@NotNull`) and surface it on
      `PolicyResponse` / `PolicyDetailResponse`.
- [x] `@WebMvcTest` cases in `PolicyControllerTest` for create with a
      `policyType` and the 400 when `policyType` is missing.
- **Revised**: `Policy.coverStartDate`/`coverEndDate` were removed entirely
      (`V015__drop_policy_cover_dates.sql`) — a policy covers many visitors,
      each with their own travel dates, so a fixed range didn't belong at the
      policy level. The day-range validation against `policyType` moved to
      `VisitorServiceImpl` (`dateIn`/`dateOut`), see task 3's follow-up notes.
      Verified the full migration chain (V001→V015) and the original
      duplicate-benefit failure scenario against a real Postgres 16
      container, not just `mvn test` (Flyway is disabled in the test
      profile).

## 2. Benefit: fixed catalog with mandated minimums — done

- [x] Add `BenefitType` enum in the `benefit` package: `PERSONAL_ACCIDENT`,
      `EMERGENCY_MEDICAL_EXPENSES`, `EMERGENCY_MEDICAL_EVACUATION`,
      `REPATRIATION_OF_MORTAL_REMAINS`, `HOSPITAL_BENEFITS`,
      `PRESCRIPTION_MEDICINES`. Each constant carries its mandated minimum
      `limitAmount` (USD 20,000 / 25,000 / 5,000 / 1,000 / 300); personal
      accident has no fixed minimum per the framework, so it's `ZERO`
      (`BenefitType.getMinimumLimit()`), and the mandated cumulative floor
      lives on `BenefitType.MANDATED_CUMULATIVE_MINIMUM` (50,000).
- [x] Replaced `Benefit.name` (free text) with `benefitType: BenefitType`
      (`V013__replace_benefit_name_with_benefit_type.sql`: drops `name`,
      adds `benefit_type` defaulted to `PERSONAL_ACCIDENT` for existing rows,
      moves the unique index to `(policy_id, benefit_type)`).
- [x] Updated `BenefitRequest`/`BenefitResponse` to use `benefitType` instead
      of `name`; updated `BenefitMapper` and `BenefitRepository`
      (`existsByPolicyIdAndBenefitType[AndIdNot]`).
- [x] `BenefitServiceImpl` rejects a `limitAmount` below the `BenefitType`'s
      mandated minimum on create/update (`IllegalArgumentException` → 400).
- [x] Added `PolicyActivatingEvent` (published by `PolicyServiceImpl` via
      Spring's `ApplicationEventPublisher`, before the Rabbit
      `policy.activated` event) and `PolicyActivationGateListener` in the
      `benefit` package, which enforces that all six `BenefitType`s are
      present with a combined limit ≥ USD 50,000 before a policy may go
      `ACTIVE` — thrown as `IllegalStateException` (→ 409), which rolls back
      the transactional create/update. Kept `benefit → policy` as the only
      compile-time dependency edge (matching the rest of the codebase) by
      using an event instead of injecting `BenefitService` into
      `PolicyServiceImpl`.
- [x] Updated `VisitorBenefitResponse`/`VisitorBenefitMapper` to expose
      `benefitType` instead of `benefitName`; `BenefitService.namesByIds`
      renamed to `typesByIds` (`Map<UUID, BenefitType>`).
- [x] Unit tests in `BenefitServiceImplTest` for minimum-limit rejection
      (create + update) and duplicate `benefitType` handling (batch and
      cross-request).
- [x] New `PolicyActivationGateListenerTest` covering: full catalog at
      mandated minimums (allowed), a missing type (rejected), cumulative
      limit below 50,000 despite full coverage (rejected), and no benefits
      at all (rejected).
- [x] `PolicyServiceImplTest` additions: activation event published only on
      DRAFT→ACTIVE, not published on DRAFT save, and a rejected gate rolls
      back (no Rabbit publish). Updated `PolicyControllerTest`,
      `VisitorControllerTest`, `VisitorBenefitServiceImplTest`,
      `VisitorCreatedListenerTest` for the `benefitType` rename. Full suite
      green (`mvn test`: 100 tests, 0 failures).
- Not done: no `BenefitControllerTest`/`VisitorBenefitControllerTest` were
  added — neither existed before this change, and the endpoints themselves
  didn't change shape beyond the field rename already covered by the service
  tests above and by `PolicyControllerTest`'s embedded-benefit assertions.

## 3. Visitor: extended KYC fields — done

- [x] Added columns to `Visitor` (`V014__visitor_kyc_fields.sql`): `address`
      (required), `reasonForTravel` (required), `facePhotoUrl` (required —
      stores a URL, not the binary), `underlyingConditions` (nullable, free
      text covering conditions and prescribed medicines not easily
      accessible). Existing rows default the three required fields to `''`.
- [x] Added the same fields to `VisitorRequest` (`@NotBlank` on the three
      required ones, plain nullable `String` for `underlyingConditions`) and
      to `VisitorResponse`/`VisitorDetailResponse`; updated `VisitorMapper`.
- [x] Face-photo upload path: went with "accept a pre-uploaded URL in
      `VisitorRequest`" rather than a dedicated upload endpoint — building
      multipart upload + storage (local disk vs. object storage) is a
      separate infra decision with its own config/deployment surface, out of
      scope for this domain-model task. `facePhotoUrl` is a plain required
      string; whatever uploads the photo and returns a URL first is a
      follow-up, not blocking this change.
- [x] `VisitorServiceImplTest` updated (constructor call sites for the new
      `VisitorRequest` fields) — no new service-layer validation logic was
      needed since the required/optional split is handled by bean validation
      (`@NotBlank`), consistent with how the rest of `VisitorRequest`'s
      fields are validated.
- [x] `@WebMvcTest` additions in `VisitorControllerTest`: `createReturns
      CreatedWithKycFields` (asserts `address`/`reasonForTravel`/
      `facePhotoUrl` round-trip) and `createRejectsMissingRequiredKycFields`
      (blank `address`/`reasonForTravel`/`facePhotoUrl` → 400 "Validation
      failed"). Full suite green (`mvn test`: 102 tests, 0 failures).
- **Follow-up** (after task 1 was revised — see its notes): the
      `PolicyType` day-range validation moved here from `PolicyServiceImpl`,
      since `dateIn`/`dateOut` is the actual per-traveler cover period.
      `VisitorServiceImpl.create`/`update` fetch the visitor's `Policy` and
      reject (`IllegalArgumentException` → 400) when `dateOut` precedes
      `dateIn`, or when the day span falls outside the policy's `PolicyType`
      range. Covered in `VisitorServiceImplTest` (boundary + rejection cases
      for all three `PolicyType`s, plus the reversed-dates case).

## 4. Documentation

- [x] `backend-architecture.md` updated to describe `PolicyType`,
      `BenefitType` + mandated minimums, and the extended `Visitor` KYC
      fields — written up front and verified against the actual
      implementation as each of sections 1–3 landed; field names and
      nullability match what shipped, no further doc drift to reconcile.

## Out of scope (not tracked here)

External integrations (eTA, API-PNR, immigration port-entry, payment
gateway, geolocation) and infra/ops items (PCI-DSS, SOC 2, 99.9% uptime,
data retention policy) from `travel-insurance.md`'s gap table are
deliberately excluded — they need their own design pass before becoming
engineering tasks.
