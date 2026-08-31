# Travel Insurance — Backend Architecture

This document describes the architecture of the Inbound Travel Health Insurance backend.
It is the reference for how the codebase is organized, how the domain model fits
together, and the conventions every contribution is expected to follow. Read it
before writing your first feature.

The `Policy`, `Benefit`, and `Visitor` shapes described below reflect the
requirements of the Ministry of Health's Mandatory Inbound Travel Health
Insurance framework — see `travel-insurance.md` for the underlying regulatory
summary and the full requirement-vs-codebase gap analysis.

## Contents

- [Travel Insurance — Backend Architecture](#travel-insurance--backend-architecture)
  - [Contents](#contents)
  - [Tech Stack](#tech-stack)
  - [Project Layout (Package by Feature)](#project-layout-package-by-feature)
  - [Core Insurance Flow](#core-insurance-flow)
  - [Users, Roles \& Organizations](#users-roles--organizations)
  - [Layering Rules](#layering-rules)
  - [Base Entity, Auditing \& Soft Delete](#base-entity-auditing--soft-delete)
  - [Conventions](#conventions)
  - [REST Resources](#rest-resources)
  - [Messaging (RabbitMQ)](#messaging-rabbitmq)
  - [Notifications (Policy Document Email)](#notifications-policy-document-email)
  - [OTP (Point-of-Service Verification)](#otp-point-of-service-verification)
  - [Premium Receipt (Singleton Levy Rates)](#premium-receipt-singleton-levy-rates)
  - [Member Statement Report](#member-statement-report)
  - [API Documentation (Swagger)](#api-documentation-swagger)
  - [Security](#security)
  - [Database Connection Pool (HikariCP)](#database-connection-pool-hikaricp)
  - [Database Migrations (Flyway)](#database-migrations-flyway)
  - [Code Practices](#code-practices)
  - [Maven Dependencies](#maven-dependencies)

## Tech Stack

- Java 21
- Spring Boot 3.x (Web, Data JPA, Validation, Security)
- PostgreSQL (runtime) / H2 (tests)
- RabbitMQ (messaging)
- SMTP + Thymeleaf + openhtmltopdf (policy document email on visitor activation)
- Flyway (database migrations)
- Maven

## Project Layout (Package by Feature)

The codebase is organized by **feature**, not by technical layer. Each feature
package contains its own controller, service (interface + implementation),
repository, domain entity, mapper, and DTOs.

```
com.travel.insurance/
│
├── 📁 config/                              # Global configuration
│   ├── SecurityConfig.java                 # Filter chain, route rules, PasswordEncoder
│   ├── JpaAuditingConfig.java              # @EnableJpaAuditing + AuditorAware
│   ├── OpenApiConfig.java                  # Swagger/OpenAPI metadata
│   ├── RabbitConfig.java                   # Exchanges, queues, bindings
│   ├── MailProperties.java                 # app.mail.* (from address, emergency-assistance contact)
│   └── UssdProperties.java                 # ussd.feedback.* (default-scheme-name, email.to)
│
├── 📁 common/                              # Shared, feature-agnostic code
│   ├── 📁 domain/
│   │   └── BaseEntity.java                 # @MappedSuperclass: ID, audit + soft-delete fields
│   ├── 📁 exception/
│   │   ├── GlobalExceptionHandler.java     # @RestControllerAdvice (ExchangeRateUnavailable → 503)
│   │   ├── ResourceNotFoundException.java
│   │   └── ApiError.java                   # Standard error response body
│   ├── 📁 service/
│   │   └── CurrencyConversionService.java  # KES→USD rate via ExchangeRate-API (RestClient,
│   │                                       # @Cacheable("fxRates"), 24h TTL)
│   ├── 📁 messaging/
│   │   └── EventPublisher.java             # Thin wrapper over RabbitTemplate
│   ├── 📁 email/
│   │   └── EmailService.java               # Thin wrapper over JavaMailSender
│   └── 📁 util/
│
├── 📁 notification/                        # Feature: Visitor-facing notifications
│   ├── VisitorActivatedNotificationListener.java  # @TransactionalEventListener(AFTER_COMMIT)
│   │                                       # on VisitorStatusChangedEvent; composes
│   │                                       # Visitor+Policy+VisitorBenefit+Insurer data
│   ├── PolicyDocumentRenderer.java         # Thymeleaf → HTML → PDF (openhtmltopdf)
│   └── PolicyDocumentData.java             # Internal template data holder (not a DTO)
│
├── 📁 auth/                                # Feature: Authentication
│   ├── AuthController.java                 # /login, /refresh
│   ├── AuthService.java                    # Service interface
│   ├── AuthServiceImpl.java                # Credential checks, token issuing
│   ├── JwtTokenProvider.java               # Token creation and validation
│   └── 📁 dto/
│       ├── LoginRequest.java
│       └── TokenResponse.java
│
├── 📁 user/                                # Feature: User Management
│   ├── UserController.java                 # Web layer (@RestController)
│   ├── UserService.java                    # Service interface (business contract)
│   ├── UserServiceImpl.java                # @Service implementation
│   ├── UserRepository.java                 # Data access (extends JpaRepository)
│   ├── User.java                           # Domain entity (@Entity)
│   ├── Role.java                           # Enum: ADMIN, INSURER_USER,
│   │                                       #       PROVIDER_USER
│   ├── UserMapper.java                     # Entity ⇄ DTO mapping
│   └── 📁 dto/
│       ├── UserRequest.java
│       └── UserResponse.java
│
├── 📁 insurer/                             # Feature: Insurer Management
│   ├── InsurerController.java
│   ├── InsurerService.java                 # Interface
│   ├── InsurerServiceImpl.java
│   ├── InsurerRepository.java
│   ├── Insurer.java                        # ...organizationId (nullable, → Organization)
│   ├── InsurerMapper.java
│   ├── InsurerCreatedEvent.java             # published on create; consumed below
│   ├── InsurerCreatedListener.java          # @EventListener — provisions a starter
│   │                                        # Policy via PolicyService.create
│   └── 📁 dto/
│       ├── InsurerRequest.java
│       ├── InsurerResponse.java
│       └── InsurerDetailResponse.java       # InsurerResponse + embedded policies
│                                             # (GET /api/v1/insurers/detailed only)
│
├── 📁 serviceprovider/                     # Feature: Service Provider Management
│   ├── ServiceProviderController.java
│   ├── ServiceProviderService.java         # Interface
│   ├── ServiceProviderServiceImpl.java
│   ├── ServiceProviderRepository.java
│   ├── ServiceProvider.java                # name (unique), contactEmail, contactPhone, address,
│   │                                        # organizationId (nullable, → Organization)
│   ├── ServiceProviderMapper.java
│   └── 📁 dto/
│       ├── ServiceProviderRequest.java
│       └── ServiceProviderResponse.java
│
├── 📁 policy/                              # Feature: Policy Management
│   ├── PolicyController.java
│   ├── PolicyService.java                  # Interface
│   ├── PolicyServiceImpl.java
│   ├── PolicyRepository.java
│   ├── Policy.java                         # insurerId
│   ├── PolicyStatus.java                   # Enum: DRAFT, ACTIVE, EXPIRED, CANCELLED
│   ├── PolicyMapper.java
│   └── 📁 dto/
│       ├── PolicyRequest.java
│       ├── PolicyResponse.java
│       └── PolicyDetailResponse.java       # PolicyResponse + embedded global benefit catalog
│
├── 📁 benefit/                             # Feature: Benefit Catalog (global)
│   ├── BenefitController.java
│   ├── BenefitService.java                 # Interface
│   ├── BenefitServiceImpl.java
│   ├── BenefitRepository.java
│   ├── Benefit.java                        # benefitName, limitAmount (no policy link)
│   ├── BenefitMapper.java
│   └── 📁 dto/
│       ├── BenefitRequest.java
│       └── BenefitResponse.java
│
├── 📁 visitor/                             # Feature: Visitor (insured traveler) Management
│   ├── VisitorController.java
│   ├── VisitorService.java                 # Interface
│   ├── VisitorServiceImpl.java
│   ├── VisitorRepository.java
│   ├── Visitor.java                        # policyId + passport-based KYC attributes,
│   │                                       # incl. address, facePhotoUrl, reasonForTravel,
│   │                                       # underlyingConditions; plus nullable
│   │                                       # paymentReference, etaReference, portOfEntry,
│   │                                       # entryTimestamp, exitTimestamp
│   ├── VisitorCreatedEvent.java            # In-process event on visitor creation;
│   │                                       # consumed by visitorbenefit to seed benefits
│   ├── Gender.java                         # Enum: MALE, FEMALE, OTHER
│   ├── MaritalStatus.java                  # Enum: SINGLE, MARRIED, DIVORCED, WIDOWED
│   ├── VisitorMapper.java
│   └── 📁 dto/
│       ├── VisitorRequest.java
│       ├── VisitorResponse.java
│       ├── VisitorDetailResponse.java      # KYC + assigned visitor benefits
│       └── VisitorEntryExitUpdate.java     # exactly one of entryTimestamp /
│                                           # exitTimestamp, used by the
│                                           # by-passport entry-exit PATCH
│
├── 📁 visitorbenefit/                      # Feature: Benefits assigned to a visitor
│   ├── VisitorBenefitController.java
│   ├── VisitorBenefitService.java          # Interface
│   ├── VisitorBenefitServiceImpl.java
│   ├── VisitorBenefitRepository.java
│   ├── VisitorBenefit.java                 # visitorId, benefitId, limitAmount
│   ├── VisitorCreatedListener.java         # seeds visitor benefits from the
│   │                                       # catalog on VisitorCreatedEvent
│   ├── VisitorBenefitMapper.java
│   └── 📁 dto/
│       ├── VisitorBenefitRequest.java
│       └── VisitorBenefitResponse.java
│
├── 📁 preauthorization/                    # Feature: Pre-authorization Requests
│   ├── PreauthorizationController.java     # /api/v1/preauthorizations — the only preauth
│   │                                       # endpoints; enhancement/items have no controller
│   │                                       # of their own, folded into create/get here
│   ├── PreauthorizationService.java        # Interface
│   ├── PreauthorizationServiceImpl.java
│   ├── PreauthorizationRepository.java
│   ├── PreauthorizationEnhancementRepository.java
│   ├── PreauthorizationItemRepository.java
│   ├── Preauthorization.java               # policyId, insurerId (denormalized from Policy),
│   │                                       # visitorId, icd11CodeId, benefitId,
│   │                                       # serviceProviderId, requestedAmount,
│   │                                       # approvedAmount — the raw ask, decided via
│   │                                       # PENDING/APPROVED/PARTIALLY_APPROVED/REJECTED
│   ├── PreauthorizationEnhancement.java    # preauthorizationId (plain UUID column, no JPA
│   │                                       # relation — same convention as VisitorBenefit),
│   │                                       # medicalServiceId, requestedAmount. Exactly one
│   │                                       # per Preauthorization (unique constraint). No
│   │                                       # status of its own — it's the itemized billing
│   │                                       # detail attached to the preauth, not something
│   │                                       # decided independently
│   ├── PreauthorizationItem.java           # enhancementId (plain UUID column, no JPA
│   │                                       # relation), description, quantity, unitPrice,
│   │                                       # amount, serviceDate
│   ├── PreauthorizationStatus.java         # Enum: PENDING, APPROVED, PARTIALLY_APPROVED,
│   │                                       #       REJECTED, EXPIRED
│   ├── PreauthorizationMapper.java
│   └── 📁 dto/
│       ├── PreauthorizationRequest.java    # embeds medicalServiceId + preauthorizationItems
│       ├── PreauthorizationItemRequest.java
│       ├── PreauthorizationItemResponse.java
│       ├── PreauthorizationDecisionRequest.java   # Approve/reject with amount and reason
│       └── PreauthorizationResponse.java
│
├── 📁 claim/                               # Feature: Claims Processing
│   ├── ClaimController.java
│   ├── ClaimService.java                   # Interface
│   ├── ClaimServiceImpl.java
│   ├── ClaimRepository.java
│   ├── Claim.java                          # policyId, benefitId, serviceProviderId,
│   │                                       # preauthorizationId, visitorId (all nullable
│   │                                       # except policy/benefit), insurerId (derived
│   │                                       # from the policy), claimedAmount (raw KES),
│   │                                       # currency/baseCurrency, claimedAmountBase
│   │                                       # (USD), exchangeRate + fxRateDate (snapshot
│   │                                       # at save), approvedAmount, prescription,
│   │                                       # documentIds (UUID element collection); the
│   │                                       # claim_diagnoses / claim_procedures /
│   │                                       # claim_invoices tables persist IDs only
│   ├── ClaimStatus.java                    # Enum: OPEN, SUBMITTED, UNDER_REVIEW,
│   │                                       #       APPROVED, PARTIALLY_APPROVED,
│   │                                       #       REJECTED, PAID
│   ├── ClaimMapper.java
│   └── 📁 dto/
│       ├── ClaimRequest.java               # insurerId NOT accepted — derived server-side
│       ├── ClaimDecisionRequest.java
│       └── ClaimResponse.java              # embeds full visitor/insurer/invoices/
│                                           # diagnoses (ICD-11)/procedures objects
│
├── 📁 invoice/                             # Feature: claim supporting invoices
│   ├── InvoiceController.java              # /api/v1/invoices
│   ├── InvoiceService.java                 # Interface
│   ├── InvoiceServiceImpl.java
│   ├── InvoiceRepository.java
│   ├── Invoice.java                        # claimId (ID-only), invoiceNumber,
│   │                                       # issueDate, currency, totalAmount
│   │                                       # (raw KES) + exchangeRate, baseCurrency,
│   │                                       # baseTotalAmount (USD), fxRateDate snapshot
│   ├── InvoiceItem.java                    # medicalServiceId (ID-only), description, quantity,
│   │                                       # unitPrice, amount (raw KES) + baseUnitPrice,
│   │                                       # baseAmount (USD),
│   │                                       # serviceDate (owned by the invoice)
│   ├── InvoiceMapper.java
│   └── 📁 dto/
│       ├── InvoiceRequest.java
│       ├── InvoiceResponse.java
│       ├── InvoiceItemRequest.java
│       └── InvoiceItemResponse.java        # medicalServiceName resolved via
│                                           # MedicalServiceService
│
├── 📁 icd11/                               # Feature: ICD-11 diagnosis code catalog
│   ├── Icd11CodeController.java
│   ├── Icd11CodeService.java               # Interface
│   ├── Icd11CodeServiceImpl.java
│   ├── Icd11CodeRepository.java
│   ├── Icd11Code.java                      # code (unique), title
│   ├── Icd11ExcelParser.java               # parses uploaded .xlsx → code/title rows
│   ├── Icd11CodeMapper.java
│   └── 📁 dto/
│       ├── Icd11CodeResponse.java
│       └── Icd11ImportResult.java          # totalRows, inserted, updated, skipped
│
├── 📁 department/                          # Feature: Department catalog
│   ├── DepartmentController.java
│   ├── DepartmentService.java              # Interface
│   ├── DepartmentServiceImpl.java
│   ├── DepartmentRepository.java
│   ├── Department.java                     # name (unique) — nothing else
│   ├── DepartmentMapper.java
│   └── 📁 dto/
│       ├── DepartmentRequest.java
│       └── DepartmentResponse.java
│
├── 📁 organization/                        # Feature: Organization directory (name, type, email, phone, address, city)
│   ├── OrganizationController.java
│   ├── OrganizationService.java            # Interface
│   ├── OrganizationServiceImpl.java
│   ├── OrganizationRepository.java
│   ├── Organization.java                   # name (unique), organizationType, email, phoneNumber, address,
│   │                                        # city, logoUrl, policyToken, notificationEmail,
│   │                                        # notificationEmailPassword (encrypted), host, port,
│   │                                        # esignature — all optional
│   ├── OrganizationType.java                # enum: ADMIN, INSURER, SERVICE_PROVIDER
│   ├── OrganizationMapper.java
│   ├── OrganizationCreatedEvent.java         # published on create; consumed below
│   ├── OrganizationCreatedListener.java      # @EventListener — provisions the matching
│   │                                         # Insurer/ServiceProvider for organizationType
│   │                                         # INSURER/SERVICE_PROVIDER (ADMIN is a no-op)
│   ├── OrganizationUpdatedEvent.java         # published on update; consumed below
│   ├── OrganizationUpdatedListener.java      # @EventListener — mirrors the update into the
│   │                                         # matching Insurer/ServiceProvider (found via
│   │                                         # organizationId), if one exists; no-op otherwise
│   └── 📁 dto/
│       ├── OrganizationRequest.java
│       └── OrganizationResponse.java
│
├── 📁 medicalservice/                      # Feature: Service catalog (belongs to a department)
│   ├── MedicalServiceController.java
│   ├── MedicalServiceService.java          # Interface
│   ├── MedicalServiceServiceImpl.java
│   ├── MedicalServiceRepository.java
│   ├── MedicalService.java                 # name, departmentId (unique per department)
│   ├── MedicalServiceExcelParser.java      # parses uploaded .xlsx → service/department rows
│   ├── MedicalServiceMapper.java
│   └── 📁 dto/
│       ├── MedicalServiceRequest.java
│       ├── MedicalServiceResponse.java
│       └── MedicalServiceImportResult.java  # totalRows, departmentsCreated,
│                                            # servicesInserted, servicesSkipped
│
├── 📁 report/                               # Feature: Claim receipts & provider reports
│   ├── ReportController.java                # /api/v1/reports — claim receipt + provider report
│   ├── ReportService.java                   # Interface
│   ├── ReportServiceImpl.java               # PDF (Thymeleaf + openhtmltopdf), Excel (POI),
│   │                                       # JSON paginated provider report
│   └── 📁 dto/
│       ├── ClaimReceiptResponse.java        # Full claim breakdown for receipt PDF
│       ├── ClaimInvoiceGroup.java           # Invoice with nested line items
│       ├── ClaimLineItem.java               # Service name, department, qty, price, amount
│       ├── ProviderClaimReportRow.java      # Per-row for provider report
│       ├── ProviderClaimReportSummary.java  # Aggregate totals + status counts
│       └── ProviderClaimReportResponse.java # Wraps summary + paginated rows
│
├── 📁 memberstatement/                     # Feature: Member Statement Report
│   ├── MemberStatementController.java      # /api/v1/member-statements
│   ├── MemberStatementService.java         # Interface
│   ├── MemberStatementServiceImpl.java     # Fan-in: VisitorService, VisitorBenefitService,
│   │                                       # PolicyService, ClaimService, BenefitService,
│   │                                       # ServiceProviderService
│   ├── MemberStatementExportType.java      # Enum: PDF, EXCEL
│   ├── MemberStatementExcelWriter.java     # POI XLSX builder (same plain-cells convention
│   │                                       # as ProcedureUploadWorkbooks)
│   ├── MemberStatementPdfRenderer.java     # Thymeleaf → openhtmltopdf, same pipeline as
│   │                                       # PolicyDocumentRenderer (templates/member-statement.html)
│   └── 📁 dto/
│       ├── MemberStatementResponse.java    # memberName/passportNumber/policyId +
│       │                                   # benefits (List<VisitorBenefitResponse>,
│       │                                   # reused as-is) + transactions
│       └── MemberStatementTransaction.java # one row per Claim (not per Invoice)
│
├── 📁 ussd/                                # Feature: USSD Gateway & Self-Service
│   ├── 📁 config/
│   │   └── RedisConfig.java                # RedisTemplate<String, UssdSession>
│   ├── 📁 controller/
│   │   └── UssdController.java             # POST /ussd/handle (form-encoded & JSON)
│   ├── 📁 domain/
│   │   ├── UssdSession.java                # Redis-backed session state model
│   │   └── ProviderPanelEntry.java         # POJO: area, town, county, providerName, address, services
│   ├── 📁 dto/
│   │   ├── UssdRequest.java                # Gateway request payload
│   │   └── UssdResponse.java               # CON / END response text
│   ├── 📁 service/
│   │   ├── UssdService.java                # Interface
│   │   ├── UssdServiceImpl.java            # State machine (Find Hospital, Feedback)
│   │   ├── ProviderPanelLoader.java        # Parses provider-panel.xlsx on startup (Apache POI)
│   │   └── ProviderPanelService.java       # In-memory search by county/town
│   └── 📁 utils/
│       └── UssdSessionManager.java         # Redis session TTL (180s) & input tracker
│
├── 📁 premiumreceipt/                      # Feature: Premium Receipt (singleton levy rates)
│   ├── PremiumReceiptController.java
│   ├── PremiumReceiptService.java          # Interface
│   ├── PremiumReceiptServiceImpl.java
│   ├── PremiumReceiptRepository.java
│   ├── PremiumReceipt.java                 # totalPremium, pcfLevy, insurancePremiumLevy,
│   │                                        # stampDuty, trainingLevy — one fixed row, no create/delete
│   ├── PremiumReceiptMapper.java
│   └── 📁 dto/
│       ├── PremiumReceiptPatchRequest.java
│       └── PremiumReceiptResponse.java
│
└── TravelInsuranceApplication.java         # @SpringBootApplication entry point
```

## Core Insurance Flow

```
Benefit                                   (a global catalog of named benefits with limits,
                                           not scoped to any policy)

Policy
   │
   ├──1:N── Visitor ──1:N── VisitorBenefit  (the insured travelers and the benefits
   │            (KYC record;   assigned to them; each row references a global
   │             holds policyId) Benefit and snapshots its own limitAmount)
   │
   ├──1:N── Preauthorization ──0:1── Claim
   │            (provider asks for approval  (a claim may reference the
   │             before rendering a service)  pre-authorization that authorized it)
   ├──1:N── Claim ──1:N── Invoice           (supporting invoices; each invoice
   │            (claims may also arrive     references a MedicalService by ID and
   │             without a pre-auth, e.g.    owns its invoice_items)
   │             reimbursement of out-of-
   │             pocket costs)
   │
   └── MedicalService ──0:1── Invoice       (ID-only; the service name is resolved
                                            into InvoiceResponse via the
                                            MedicalServiceService — no JPA relation)
```

- A **Policy** is the insurance contract. It references a single backing
  insurer (`insurerId`) and carries a status, but no cover dates of its own —
  one policy covers many visitors, each entering and leaving on their own
  schedule, so a fixed date range doesn't belong at the policy level. Every
  policy allows a single cover-period range (1 day up to 12 months), enforced
  per visitor instead (see below). A policy holds no treatment-level detail.
  `GET /api/v1/policies/{id}` and the paged
  `GET /api/v1/policies` return `PolicyDetailResponse` rows that embed the
  benefit catalog under `benefits`; since benefits are global (see below),
  every policy carries the whole catalog. `PolicyController` fetches it once
  via `BenefitService.listAll()` and attaches it to each policy. Create/update
  return plain `PolicyResponse` rows without benefits.
- Since a policy backs exactly one insurer (`Policy.insurerId`), `InsurerResponse`
  (returned by `POST/GET/PUT /api/v1/insurers` and the paged `GET
  /api/v1/insurers`) carries a `policyId` field alongside the insurer's own
  fields — resolved via `PolicyService.findPolicyIdByInsurerId`
  (`PolicyRepository.findFirstByInsurerId`). `InsurerServiceImpl` depends on
  `PolicyService` for this, injected with `@Lazy` to avoid a circular bean
  dependency with `PolicyServiceImpl`'s existing dependency on `InsurerService`
  (`InsurerService.exists()` during policy validation).
- `GET /api/v1/insurers/detailed` returns every insurer as
  `InsurerDetailResponse` (`InsurerResponse` + `policies`), additionally
  embedding the full list of policies backed by that insurer via
  `PolicyService.listByInsurerId` (`PolicyRepository.findAllByInsurerId`,
  unpaged) — in practice at most one, since the relationship is one-to-one,
  but the method stays list-shaped for forward compatibility. This is
  deliberately an **additional** endpoint, not a change to the existing `GET
  /api/v1/insurers`/`GET /api/v1/insurers/{id}` shape — unlike
  `PolicyDetailResponse` and `VisitorDetailResponse`, which replaced their
  plain counterparts on the existing routes. `InsurerController` composes the
  detailed response itself from `InsurerService` + `PolicyService`
  (controller-level fan-in, same as `VisitorController.toDetail()`).
- `Insurer` carries outbound-email/e-signature settings used for policy
  document notifications: `notificationEmail`, `notificationEmailPassword`,
  `host`, `port`, `esignature`. All are accepted on the `InsurerRequest`
  (create/update). `notificationEmailPassword` is a credential and is
  encrypted at rest via `EncryptedStringConverter` (same pattern as
  `Visitor`'s PII columns — see "Security") and is deliberately **omitted**
  from `InsurerResponse`/`InsurerDetailResponse`; the other four fields are
  returned as plain values.
- These outbound-email settings are consumed by
  `VisitorActivatedNotificationListener` when it emails a visitor's policy
  certificate. It loads the backing `Insurer` as an **entity** via
  `InsurerService.getEntityById(UUID id)` (mirroring `PolicyService`/
  `VisitorService`'s existing `getEntityById`) rather than `getById`'s DTO,
  since `notificationEmailPassword` is intentionally never exposed on
  `InsurerResponse` and the decrypted value is only obtainable by loading the
  entity (decryption happens transparently via `EncryptedStringConverter` on
  load). If the insurer's `host`, `port`, `notificationEmail`, and
  `notificationEmailPassword` are **all** non-blank, the certificate is sent
  through a `JavaMailSender` built from those credentials
  (`common.email.SmtpCredentials` / `SmtpSenderFactory` /
  `DefaultSmtpSenderFactory`, wired into `EmailService` alongside the
  app-wide autoconfigured `JavaMailSender`), with `from` set to the
  insurer's `notificationEmail`. Otherwise it falls back entirely to the
  global `spring.mail.*` sender and `app.mail.from` — the `from` address and
  the SMTP relay are always resolved together, never mixed, to avoid
  SPF/relay rejection. `EmailService` stays domain-agnostic: the override is
  passed in as a generic `SmtpCredentials` record, and `EmailServiceTest`
  exercises the override path entirely against a mocked `SmtpSenderFactory`
  — no test ever opens a real SMTP connection. `Insurer.esignature`, when
  set, is normalized the same way as `logoUrl` (`LogoUrlNormalizer`, e.g.
  for Dropbox share links) and rendered as an image in the certificate's
  signature block (`policy-certificate.html`'s `.sign` table), replacing the
  default "No wet signature required" copy with "Digitally signed by the
  underwriter" so the certificate reads as genuinely signed when an insurer
  has provided one.
- **Benefit** is a standalone **global catalog** entry: a `benefitName` (free
  text) and a `limitAmount` (limit of cover). It is no longer scoped to a
  policy — there is no `policyId` or fixed `BenefitType` enum. The catalog is
  managed directly through full CRUD (`POST/GET/PUT/DELETE /api/v1/benefits`);
  names are not required to be unique. Consumption is not tracked against the
  limit. Because there is no policy link, a policy's `benefits` in
  `PolicyDetailResponse` is simply the entire catalog. Other features
  reference a benefit by ID only: `VisitorBenefit`, `Preauthorization` and
  `Claim` validate that the referenced benefit exists (via
  `BenefitService.getEntityById`), but no longer that it belongs to a
  particular policy.
- **ICD-11 Code** is a reference catalog of diagnosis codes, each a unique
  `code` and a `title`. It is bulk-loaded by an admin uploading an `.xlsx`
  workbook to `POST /api/v1/icd11-codes/import` (multipart; `Icd11ExcelParser`
  locates the `code`/`title` header columns case-insensitively). The import
  upserts by `code` — existing codes are updated, new ones inserted, blank rows
  skipped — and returns an `Icd11ImportResult` count summary, so re-uploading
  the same file is idempotent. Lookups use `GET /api/v1/icd11-codes?query=…`
  (matches code or title, paged), `GET /api/v1/icd11-codes/search?title=…`
  (title-only substring match, paged — the diagnosis picker use case),
  `GET /api/v1/icd11-codes/{code}` and `GET /api/v1/icd11-codes/by-id/{id}`
  (resolves a claim's `diagnosisIds` back to code/title for display). Import is restricted to `ADMIN`; the read
  endpoints are open to any authenticated user.
- A **Department** is a plain name-only catalog entry (e.g. `PHARMACY`,
  `LABORATORY`) — nothing beyond the `BaseEntity` fields and a unique `name`.
  A **MedicalService** belongs to exactly one department, referenced by
  `departmentId` (ID-only, same convention as every other cross-feature
  reference — no JPA relation), and its `name` is unique per department rather
  than globally, so two departments may each have an identically-named
  service. `GET /api/v1/departments/{id}` never embeds that department's
  services — callers fetch them separately via
  `GET /api/v1/medical-services?departmentId=…` (paged) or
  `GET /api/v1/medical-services/by-department/{departmentId}` (unpaged),
  keeping department reads cheap regardless of catalog size.
  `MedicalServiceResponse` additionally carries the owning department's
  `departmentName` (resolved through `DepartmentService.namesByIds`; `null` if
  the department has since been deleted), the same "resolve the display name,
  don't nest the entity" shape already used by `VisitorBenefitResponse`.
  Both catalogs are bulk-loaded from the master list: an admin uploads a
  two-column (`service`/`department`) `.xlsx` workbook to
  `POST /api/v1/medical-services/import` (multipart;
  `MedicalServiceExcelParser` locates the header columns case-insensitively,
  mirroring `Icd11ExcelParser`). For each row, `MedicalServiceServiceImpl`
  resolves the department by exact name — creating it via
  `DepartmentService.findOrCreateByName` if it doesn't exist yet, caching the
  lookup within the run so a department referenced by hundreds of rows is
  only resolved once — then upserts the service by (`name`, `departmentId`);
  rows with a blank service or department name, or a service already present
  in that department, are skipped. The returned `MedicalServiceImportResult`
  reports `totalRows`/`departmentsCreated`/`servicesInserted`/`servicesSkipped`,
  so re-uploading the same file is idempotent (a second run reports zero
  inserted/created). Writes (create/update/delete/import) on both
  `/api/v1/departments` and `/api/v1/medical-services` are restricted to
  `ADMIN`; reads are open to any authenticated user.
- An **Organization** is a standalone directory entry — `name` (unique),
  `organizationType` (enum: `ADMIN`, `INSURER`, `SERVICE_PROVIDER`), `email`,
  `phoneNumber`, `address`, `city` — with plain CRUD
  (`/api/v1/organizations`), following the same shape as Department. It is
  unrelated to the `organizationId` column described in
  [Users, Roles & Organizations](#users-roles--organizations), which points at
  an `Insurer` or `ServiceProvider` row rather than this entity. Writes are
  restricted to `ADMIN`; reads are open to any authenticated user.
  `GET /api/v1/organizations?organizationType=…` (paged) filters the list by
  type; the parameter is optional and omitting it returns all organizations.
  It also carries the same optional `logoUrl` (normalized via
  `LogoUrlNormalizer`, same as `Insurer`), an optional `policyToken` (`Long`,
  same meaning as `Insurer.policyToken` — see
  [Policy Tokenization (Quota Management)](#policy-tokenization-quota-management)), and
  outbound-email/e-signature settings as `Insurer` — `notificationEmail`,
  `notificationEmailPassword` (encrypted at rest via
  `EncryptedStringConverter`, never returned in `OrganizationResponse`),
  `host`, `port`, `esignature`. For an `INSURER`-type organization, all of
  these (including `logoUrl` and `policyToken`) are carried across to the
  provisioned `Insurer` by `OrganizationCreatedListener` below;
  `ServiceProvider` has no equivalent fields, so `SERVICE_PROVIDER`
  organizations don't carry any of this.
- A **Visitor** is an insured traveler behind a policy. It carries a
  `policyId` (ID-only reference — one policy may cover many visitors) and a
  denormalized `insurerId` (non-nullable `UUID`, mirroring `Claim.insurerId`),
  resolved from `policy.getInsurerId()` on the already-fetched `Policy` in
  `VisitorServiceImpl.create`/`update` — not client-supplied (absent from
  `VisitorRequest`) and re-resolved fresh on every update in case the
  visitor's `policyId` changes to a policy under a different insurer. This
  denormalization lets visitors be scoped/queried by insurer without joining
  through `policies`, the same rationale as `Claim.insurerId`. Plus the
  passport-based basic KYC attributes captured at onboarding: full name,
  passport number (unique), date of birth, gender, nationality, address,
  email, phone number, date in / date out of the country, marital status,
  reason for travel, underlying condition/prescribed-medicine notes
  (`underlyingConditions`, nullable), a face photo upload (`facePhotoUrl`),
  and next of kin (name + phone) — aligned with the e-portal ("Kenya Cares")
  onboarding data set required by the framework. `Gender` and `MaritalStatus`
  are string-mapped enums. `dateIn`/`dateOut` is where the mandated cover
  period actually gets enforced: `VisitorServiceImpl` fetches the visitor's
  policy and rejects a create/update where `dateOut` is before `dateIn`, or
  where the day span between them falls outside the allowed 1-to-365-day
  range — `IllegalArgumentException` (→ 400) either way. `Visitor.getPolicyExpiryDate()`
  is a derived, non-persisted (`@Transient`) property — always `dateIn` plus
  365 days, computed on read rather than stored, so it can never drift from
  `dateIn`. It's exposed on `VisitorResponse` and threaded through
  `PolicyDocumentData` so the certificate's "Cover Period" shows `dateIn` to
  `policyExpiryDate` (not `dateOut`, which remains the visitor's own declared
  travel end date, used for the 1-to-365-day validation above and reporting —
  the two dates serve different purposes and are allowed to differ). It also carries a
  set of nullable border/payment-tracking attributes populated after
  onboarding: `paymentReference`, `etaReference` (eTA/authorization
  reference), `portOfEntry`, and `entryTimestamp`/`exitTimestamp` (actual
  border-crossing times, distinct from the declared `dateIn`/`dateOut`
  cover dates). `entryTimestamp`/`exitTimestamp` are set independently via
  `PATCH /api/v1/visitors/by-passport/entry-exit?passportNumber=…`, taking
  a `VisitorEntryExitUpdate` body with exactly one of `entryTimestamp` or
  `exitTimestamp` populated — passing both, or neither, is rejected with
  `IllegalArgumentException` (→ 400), since entry and exit are recorded as
  separate border events. This endpoint is separate from the general
  `update`/`create` flow since these values are typically recorded later,
  by a border-control integration rather than at KYC onboarding.
  `GET /api/v1/visitors` (the paged list) takes an optional `insurerId` query
  param — omitted, it returns all visitors (`VisitorRepository.findAll`);
  provided, it filters to that insurer's visitors
  (`VisitorRepository.findByInsurerId`) using the denormalized `insurerId`
  column directly, the same optional-filter shape as
  `GET /api/v1/organizations?organizationType=…`.
  `GET /api/v1/visitors/{id}` and
  `GET /api/v1/visitors/by-passport?passportNumber=…` return a
  `VisitorDetailResponse` that embeds the visitor's assigned benefits
  (`visitorBenefits`), and `GET /api/v1/visitors/by-policy?policyId=…`
  returns a list of them (one per visitor on the policy) — composed in
  `VisitorController` from `VisitorBenefitService`, which keeps service
  dependencies acyclic. The paged list and create/update endpoints return
  plain `VisitorResponse` rows without benefits.
- A visitor carries a `VisitorStatus` with guarded transitions
  (`canTransitionTo`). A newly created visitor defaults to `ACTIVE`. It is updated via
  `PATCH /api/v1/visitors/{id}/status` or
  `PATCH /api/v1/visitors/by-passport/status?passportNumber=…`, both taking a
  `VisitorStatusUpdate` body; an allowed transition publishes a
  `VisitorStatusChangedEvent`, an invalid one is rejected with `409 Conflict`.
- Visitors are auto-assigned the full benefit catalog on creation:
  `VisitorServiceImpl` publishes an in-process `VisitorCreatedEvent`, which
  `visitorbenefit.VisitorCreatedListener` consumes to create one
  `VisitorBenefit` per global `Benefit` (each snapshotting the catalog
  `limitAmount` and taking the visitor's current status, `ACTIVE` by default).
  The listener skips benefits already
  assigned to the visitor, so it is idempotent. Further benefits can still be
  attached explicitly via the `VisitorBenefit` endpoints.
- A **VisitorBenefit** assigns a global catalog benefit to a visitor. It
  carries `visitorId`, `benefitId`, and its own `limitAmount` — snapshotted
  from the `Benefit` at assignment time unless an explicit limit is supplied —
  so later catalog edits do not alter benefits already assigned to a visitor.
  The referenced benefit only needs to exist (no policy-membership check). A
  visitor may hold each catalog benefit at most once (`visitorId` + `benefitId`
  unique).
  `VisitorBenefitResponse` additionally carries the catalog benefit's
  `benefitName` (resolved through `BenefitService`; `null` if the catalog
  benefit has since been deleted), plus two **transient, computed-on-read**
  fields — `utilizedAmount` and `balance` — so clients can display
  assignments and remaining cover without extra lookups. `utilizedAmount` is
  the sum of `Claim.claimedAmount` across every claim for that
  `(visitorId, benefitId)` pair, regardless of claim status; `balance` is
  `limitAmount − utilizedAmount` (it can go negative if claims exceed the
  limit). Neither value is persisted — `VisitorBenefitServiceImpl` computes
  them per request via `ClaimService.sumClaimedAmountsByVisitorAndBenefit`
  (the usual "go through the other feature's service, not its repository"
  rule), batched by visitor/benefit ID sets so listing several
  `VisitorBenefit`s costs one grouped `SUM` query, not N. This computation
  only runs where `VisitorBenefit` rows are returned; the plain paginated
  `GET /api/v1/visitors` never embeds `visitorBenefits` at all (see above),
  so it is unaffected regardless of claim volume.
- A **Preauthorization** is raised by a `PROVIDER_USER` before rendering a
  service and is decided by an `INSURER_USER` (or a admin agent). It also
  carries a denormalized `insurerId` (non-nullable `UUID`, same rationale and
  pattern as `Visitor.insurerId`/`Claim.insurerId`), resolved from
  `policy.getInsurerId()` on the `Policy` already fetched by
  `validatePolicyActive` in `PreauthorizationServiceImpl.create` — not
  client-supplied (absent from `PreauthorizationRequest`) and exposed on
  `PreauthorizationResponse`. There is no `update` endpoint for
  `Preauthorization`, so unlike `Visitor.insurerId` it is only ever resolved
  once, at create time. `GET /api/v1/preauthorizations` takes an optional
  `insurerId` query param, the same optional-filter shape as
  `GET /api/v1/visitors?insurerId=…`/`GET /api/v1/organizations?organizationType=…`
  — applied inside `PreauthorizationServiceImpl.findScoped` alongside the
  existing `PROVIDER_USER` auto-scoping (a `PROVIDER_USER` is always
  restricted to their own `serviceProviderId`; supplying `insurerId` narrows
  that further via `findAllByServiceProviderIdAndInsurerId` rather than
  bypassing the scope, while any other role filters the full table via
  `findAllByInsurerId`). Create
  requires the diagnosis (`icd11CodeId`, validated via `Icd11CodeService`),
  the patient (`visitorId`, validated via `VisitorService`, existence only —
  not checked against the request's `policyId`), the accessed hospital
  (`serviceProviderId`, validated via `ServiceProviderService`), the services
  rendered (`serviceDescription`), the utilised `benefitId`, and
  `requestedAmount`. On `decide`, the approver and decision time are not
  separate columns — they reuse `BaseEntity`'s existing `updatedBy`/`updatedDate`
  audit columns (already populated by `AuditorAware` on every save) and are
  surfaced in `PreauthorizationResponse` as `decidedBy`/`decidedAt`, `null`
  while the request is still `PENDING`. `PreauthorizationResponse` also
  resolves display names for every referenced ID — `visitorName`,
  `icd11Code`/`icd11Title`, `benefitName`,
  `serviceProviderName` — via the respective feature services, so API
  consumers never have to display a raw UUID.

  Optionally, the same create call also carries a `medicalServiceId`
  (validated via `MedicalServiceService`) and a list of
  `preauthorizationItems` (description, quantity, unitPrice, amount,
  serviceDate) — an itemized billing breakdown. Under the hood these live on
  a separate `PreauthorizationEnhancement` row (exactly one per
  `Preauthorization`, unique-constrained on `preauthorizationId`), not on
  `Preauthorization` itself — mirroring why `Invoice` is separate from
  `Claim` (a claim can carry multiple invoices, each with its own header),
  except a preauth has exactly one enhancement, created/read transparently
  through the same `POST`/`GET /api/v1/preauthorizations` endpoints —
  **there is no separate enhancement endpoint**, `PreauthorizationServiceImpl`
  creates the `Preauthorization` + `PreauthorizationEnhancement` + its items
  together in one transaction, and `enrich()` folds the enhancement's
  `medicalServiceName`/`preauthorizationItems` back into
  `PreauthorizationResponse` on every read. Neither `PreauthorizationEnhancement`
  nor `PreauthorizationItem` is a JPA relation — both carry a plain
  `preauthorizationId`/`enhancementId` UUID column and are loaded via
  repository queries (`findByPreauthorizationId` /
  `findAllByEnhancementId`), the same ID-only convention `VisitorBenefit`
  uses for `Visitor`. The enhancement has no status of its own — it isn't
  decided independently; its approval state is read off the parent
  `Preauthorization.status`. `requestedAmount` (on both `Preauthorization`
  and the enhancement) stays independent of the items — no cross-validation
  that it equals their sum (same as `Invoice.totalAmount` vs. `invoiceItems`
  today). A preauth with no enhancement (legacy rows, or simply never
  itemized) returns `medicalServiceId`/`medicalServiceName` as `null` and
  `preauthorizationItems` as an empty list.
- A **Claim** is the request for payment. It is either provider-submitted
  against an approved pre-authorization, or customer-submitted for
  reimbursement (no pre-authorization). Decisions are made by the insurer;
  `PAID` is the terminal status. Since the augmentation, a claim may also
  carry a `visitorId` (must belong to the claim's policy), a `prescription`,
  and four UUID sets — `diagnosisIds` (ICD-11 codes, required unless
  auto-populated from a preauthorization), `procedureIds` (procedure catalog,
  always required), `invoiceIds` (validated through `InvoiceService`), and
  `documentIds` (a placeholder for a future upload service; the
  `claim_documents` join table persists them but no documents feature exists
  yet). When a claim is created with a `preauthorizationId` and no
  `diagnosisIds` are supplied, the diagnosis is auto-populated from the
  preauthorization's `icd11CodeId`. `procedureIds` are always required — the
  preauthorization feature does not carry procedure references.
  Existing invoices can only be attached to a claim whose
  status is `OPEN` via `PUT /api/v1/claims/{id}/invoice` (`AttachInvoiceRequest`); attaching
  transitions the claim to `SUBMITTED` and syncs both the `claim_invoices` join table
  and the `invoices.claim_id` FK on the Invoice entity. Attaching to any other status returns
  409 Conflict (`IllegalStateException`). The claim's `insurerId` is **not** accepted
  on the request: it is derived server-side from the policy's single
  `insurerId`. `ClaimResponse` embeds the full
  `visitor`, `insurer`, `invoices`, `diagnoses` and `procedures` objects —
  `diagnoses` resolved through `Icd11CodeService`, `procedures` through
  `ProcedureService`, `invoices` read from `invoices.claim_id` FK (source of
  truth) — alongside the raw IDs, so consumers never have to display a raw
  UUID. A referenced diagnosis or
  procedure that is no longer in its catalog is omitted from the response with
  a logged warning (stale references never fail the claim read, but are now
  visible in logs for debugging).
- **Currency (base-equivalent) snapshotting.** Frontend amounts arrive in KES
  and are stored raw and unmodified. At every save the claim also persists a
  USD base equivalent of the claimed amount (`claimedAmountBase`), the
  `exchangeRate` used, `baseCurrency` (`USD`) and an `fxRateDate` timestamp —
  the rate snapshot at the time of saving, fetched once per currency pair and
  cached for 24 hours via `common/service/CurrencyConversionService` (an
  ExchangeRate-API `RestClient`, `@Cacheable("fxRates")` Caffeine cache). If
  the claim has invoices attached, `claimedAmountBase` aggregates the USD base
  totals of those invoices; otherwise it is the raw KES `claimedAmount`
  multiplied by the spot KES→USD rate (2-dp, `HALF_UP`). Rates are never
  recomputed against the live endpoint for historical rows; a failure to reach
  the API surfaces as `ExchangeRateUnavailableException` → 503.
- An **Invoice** is a supporting document for a claim, referenced by ID only.
  It is created through its own feature (`POST /api/v1/invoices`) and a claim
  attaches already-existing invoices by UUID. Each line item (`InvoiceItem`) optionally references
  a `MedicalService` by ID (`medicalServiceId`, validated through
  `MedicalServiceService` on create/update); `InvoiceItemResponse` resolves that
  ID to `medicalServiceName` (the "resolve the display name, don't nest the
  entity" shape). `Invoice` embeds its line items as a child aggregate
  (`invoice_items`, a bidirectional `@OneToMany`/`@ManyToOne` owned on the
  item side so the `invoice_id` FK is set on insert — the schema column is
  `NOT NULL`). Like claims, invoices snapshot the KES→USD rate at save time:
  the raw KES `totalAmount` and item `unitPrice`/`amount` are kept, and the
  USD base values (`baseTotalAmount`, item `baseUnitPrice`/`baseAmount`,
  `exchangeRate`, `baseCurrency`, `fxRateDate`) are derived from the same
  cached `CurrencyConversionService` on every create/update.
- Cross-feature references are **ID columns only** (the same rule as
  `User.organizationId`): the `claim` feature calls `PolicyService` and
  `BenefitService`, never their repositories, and no JPA relations cross
  package boundaries. Likewise the `invoice` feature calls
  `MedicalServiceService` (never its repository) to validate and resolve the
  medical service reference.

## Procedures (Catalogue & Excel Upload)

The `procedure` feature is a catalogue of service items (e.g. `Nebulization`,
`Lumbar Puncture`) scoped to a department. It is independent of the insurance
flow above.

- A **Procedure** carries a generated `procedureCode`, a display `name`, an
  internal `normalizedName` (never exposed), an optional `description`, a
  `departmentPublicId` (ID-only reference — **no** JPA relationship to the
  department feature), an `active` flag and a nullable `uploadBatchPublicId`
  (set on rows created by Excel import). The entity's UUID `id` is its public id.
- **Codes** come from a dedicated Postgres sequence (`procedure_code_seq`, via
  `ProcedureRepository.nextProcedureCodeValue()`), formatted `PRC-0001` by
  `ProcedureCodeGenerator`. The same generator serves manual creation and Excel
  import; codes are never derived from a count/`MAX+1`, never editable, never
  reused (gaps are fine). Uniqueness is enforced by `uq_procedures_code`.
- **Name cleaning/normalization** lives in `ProcedureNameNormalizer` and is shared
  by manual create, update, Excel validation and Excel import: trim, collapse
  whitespace, replace non-breaking spaces, strip control/format characters; the
  cleaned value is the display name and its upper-cased form the normalized name.
  Medical terminology is never altered.
- **Duplicate rule**: unique on `departmentPublicId + normalizedName`, enforced in
  the application and by a partial unique index (`where deleted = false`). An
  active match is rejected (`409`); an inactive match is rejected advising
  reactivation. Activation re-checks for an active conflict first.
- **Department validation** goes through the `department.DepartmentService`
  interface (`getEntityById(UUID)`), which throws `ResourceNotFoundException`
  (`404`) for an unknown department id — no repository reach-across. The
  Department entity has no active flag, so validation is existence-only.
- **Endpoints**: CRUD + search/filter (`GET /api/v1/procedures?search=&departmentPublicId=&active=`,
  paged/sortable), `PATCH /{id}/activate`, `PATCH /{id}/deactivate` (no hard
  delete in normal operation).

Bulk creation is a synchronous two-stage Excel flow under
`/api/v1/procedures/uploads` (`procedure.upload`):

- The **department is chosen per row, inside the file**. The template is
  `Procedure Name*` | `Department*` | `Description`. Each row's department **name**
  is resolved to a department id **case-insensitively and trimmed**, in one bulk
  query for the whole file (`DepartmentService.idsByName`). A blank department cell
  fails the row (`DEPARTMENT_REQUIRED`); an unmatched name fails the row
  (`DEPARTMENT_NOT_FOUND`) — departments are never auto-created. In-file and DB
  duplicate detection is keyed by **department + normalized name**, so the same
  name under two departments is not a duplicate.
- **Validate** (`POST /upload`, multipart `file` only) reads the whole workbook once
  (`ProcedureExcelParser`, preserving real Excel row numbers, formulas never
  evaluated), detects in-file duplicates via in-memory maps, bulk-loads existing
  matches with one query, classifies each row (`VALID` / `SKIPPED` (already
  exists) / `FAILED` (name required, too long, duplicate-in-file, inactive
  exists)), persists a `ProcedureUpload` + `ProcedureUploadRow` rows, and returns
  a summary. No procedures are created.
- **Import** (`POST /upload/{uploadPublicId}/import`) is guarded against repeat/parallel
  runs by status transitions (`RECEIVED → VALIDATING → READY_FOR_IMPORT →
  PROCESSING → COMPLETED[/_WITH_ERRORS]/FAILED`), re-checks duplicates immediately
  before saving, generates a code per new procedure, sets the upload-batch id,
  and persists in batches (`hibernate.jdbc.batch_size`). A late uniqueness race
  surfaces as a `409`.
- **Downloads**: `GET /upload/download` (cached static template bytes) and
  `GET /upload/{uploadPublicId}/errors` (failed/skipped rows only, with a Department column), both
  streamed as `.xlsx` attachments with the correct content type.
- Operational limits (`procedure.upload.*` → `ProcedureUploadProperties`): max
  file size, max rows, batch size, max name length. Background/`@Async` +
  streaming (SXSSF) processing for very large files is intentionally deferred.

## Users, Roles & Organizations

A single `User` entity serves everyone — admin staff, insurer staff, and
service provider staff. Users are distinguished by role, not by separate
entities:

- `User.role` is a single `Role` enum column (`ADMIN`, `INSURER_USER`,
  `PROVIDER_USER`) — a user has exactly one role, mapped to a single Spring
  Security authority. The JWT carries it as a single `role` claim
  (`JwtTokenProvider.CLAIM_ROLE`); `AuthenticatedUser.role` is a single
  `String`, not a set.
- Besides the standard `sub` claim (the user id — clients should read the
  user id from `sub`, not a duplicate claim), the JWT also carries
  `firstName`, `lastName`, and `email` claims
  (`JwtTokenProvider.CLAIM_FIRST_NAME`/`CLAIM_LAST_NAME`/`CLAIM_EMAIL`) so
  clients can render basic profile info without a separate
  `GET /api/v1/users/{id}` call. These are set once at token issuance
  (login/refresh) and are not re-verified per request — clients needing
  guaranteed-fresh profile data should still call the users endpoint.
- Every user, regardless of role, carries a plain `organizationId: UUID`
  pointing directly at an `organization.Organization` row (the directory
  entry described earlier) — the same id an `ADMIN` client would get back
  from `GET /api/v1/organizations`. `UserRequest.organizationId` is
  `@NotNull` and validated against `OrganizationService.getEntityById` on
  create/update (404 if it doesn't resolve). This is a plain column, **not**
  a JPA relation, so the `user` package stays decoupled from `organization`.
  `User.organizationId` never points directly at an `Insurer`/`ServiceProvider`
  row — see below for how `INSURER_USER`/`PROVIDER_USER` scoping bridges that
  gap.
- `UserResponse.organizationName` and the JWT's `organizationName` claim
  (`JwtTokenProvider.CLAIM_ORGANIZATION_NAME`, set at login/refresh) resolve
  `User.organizationId` via `OrganizationService.namesByIds` (batch id→name
  lookup, same pattern as `MedicalServiceResponse.departmentName`) — the same
  lookup for every role, since `organizationId` always means "the
  `Organization` row". `AuthServiceImpl.issueTokens` resolves the name fresh
  from the current `User` row on every login and refresh, so it is never
  stale inside a long-lived refresh token.
- Separately, `Insurer` and `ServiceProvider` each carry their own optional
  `organizationId: UUID` pointing at the same `organization.Organization`
  row — an ID-only reference, validated via `OrganizationService.getEntityById`
  on create/update (404 if the id doesn't resolve). Both `User.organizationId`
  and `Insurer`/`ServiceProvider.organizationId` point *at* the same
  `Organization` id space; they are just two different entities referencing
  the same directory entry, not a two-hop chain.
- Data scoping bridges `User.organizationId` (an `Organization` id) to the
  `Insurer`/`ServiceProvider` id that policies/claims/preauthorizations
  actually key on, via `InsurerService.findIdByOrganizationId`/
  `ServiceProviderService.findIdByOrganizationId` (each backed by
  `findFirstByOrganizationId` on the respective repository). For example,
  `PolicyServiceImpl.findScoped` resolves the current `INSURER_USER`'s
  `Insurer` id from their `organizationId` before filtering
  `findAllByInsurerId`; if no `Insurer`/`ServiceProvider` currently has that
  `organizationId`, scoping returns an empty page rather than falling back to
  unscoped access. Roles gate *which endpoints* a user can call;
  `organizationId` (via this bridge) gates *which rows* they can see.
- `UserResponse` and the JWT (`JwtTokenProvider.CLAIM_SERVICE_PROVIDER_ID`/
  `CLAIM_INSURER_ID`) also expose the bridged `serviceProviderId`/`insurerId`
  directly, via `UserService.serviceProviderId(User)`/`insurerId(User)`
  (`UserServiceImpl`, reusing the same `findIdByOrganizationId` bridge above).
  `serviceProviderId` is populated only for `PROVIDER_USER` (`null`
  otherwise); `insurerId` only for `INSURER_USER` (`null` otherwise); both are
  `null` when the role doesn't match or the bridge can't resolve an
  `Insurer`/`ServiceProvider` for the user's `organizationId`. As with
  `organizationName`, `AuthServiceImpl.issueTokens` resolves both fresh from
  the current `User` row on every login and refresh.
- The direction of provisioning runs from `Organization` outward, not the
  other way round: `OrganizationServiceImpl.create` publishes an in-process
  `OrganizationCreatedEvent` (via `ApplicationEventPublisher`, synchronously
  within the same transaction) after saving.
  `organization.OrganizationCreatedListener` reads the new row back through
  `OrganizationService.getEntityById` and switches on `organizationType`:
  `INSURER` creates a matching `Insurer` via `InsurerService.create`,
  `SERVICE_PROVIDER` creates a matching `ServiceProvider` via
  `ServiceProviderService.create` (`name`/`email`/`phoneNumber`/`address`
  copied across; insurer-only fields like `policyToken`/`host`/`esignature`
  are left `null`), and `ADMIN` is a no-op — there's no entity to create.
  Either create call is passed the originating `Organization.id` as
  `organizationId`, so the new `Insurer`/`ServiceProvider` is linked back in
  the same step (no separate "assign" call). Because the listener runs in
  the same transaction as the `Organization` create, a failure here (e.g. an
  `Insurer`/`ServiceProvider` name collision) rolls back the organization
  creation too. Creating an `Insurer`/`ServiceProvider` directly (with or
  without an `organizationId`) does **not** create an `Organization`.
- `PUT /api/v1/organizations/{id}` and `PATCH /api/v1/organizations/{id}`
  mirror edits the same way: after saving, `OrganizationServiceImpl.update`/
  `.patch` both publish the same `OrganizationUpdatedEvent`, and
  `organization.OrganizationUpdatedListener` looks up the matching
  `Insurer`/`ServiceProvider` via `findIdByOrganizationId` and, if one is
  found, calls its `update` with the fresh `Organization` fields read back
  through `OrganizationService.getEntityById` (same field mapping as the
  create-time listener) — for PATCH this is the already-merged entity, so a
  partial patch still produces a full, correct `Insurer`/`ServiceProvider`
  update. If no matching entity exists yet (e.g. it predates this linkage,
  or `organizationType` is `ADMIN`), the listener is a no-op — it never
  creates one. Directly updating an `Insurer`/`ServiceProvider` does **not**
  update the `Organization` it's linked to; propagation only runs
  `Organization` → `Insurer`/`ServiceProvider`.
- Provisioning continues one hop further: `InsurerServiceImpl.create`
  publishes an in-process `InsurerCreatedEvent` (via `ApplicationEventPublisher`,
  synchronously within the same transaction) after saving.
  `insurer.InsurerCreatedListener` responds by creating a starter `Policy`
  for that insurer via `PolicyService.create` (`status` left `null`, so
  `PolicyMapper` defaults it to `ACTIVE`) — every `Insurer`, whether created
  directly or provisioned from an `Organization`, ends up with a `Policy` to
  back visitors against without a separate manual step. As with the
  `Organization` → `Insurer`/`ServiceProvider` step, this runs in the same
  transaction as the `Insurer` create, so a failure here rolls back the
  insurer creation too.
- The `auth` feature owns login and JWT concerns and depends on `user`
  (service → service); `config/SecurityConfig` wires the JWT filter and
  role-based route rules.

## Layering Rules

- **Controller → Service → Repository**. Never skip a layer or call backwards.
- Every service is split into an **interface** (`XxxService`) and an
  **implementation** (`XxxServiceImpl`, annotated `@Service`). Controllers and
  other features depend only on the interface; the implementation is an
  injection-time detail. All business logic lives in the implementation.
- Controllers accept and return **DTOs only** — entities never cross the web
  boundary.
- Mappers convert between entities and DTOs so persistence details stay inside
  the feature.
- Cross-feature calls go **service → service** (for example,
  `ServiceProviderServiceImpl` may use `InsurerService`, never
  `InsurerRepository`).
- `common/` and `config/` must not depend on any feature package.

## Base Entity, Auditing & Soft Delete

Every entity extends `common/domain/BaseEntity` (`@MappedSuperclass`):

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | Primary key, generated |
| `deleted` | `boolean` | Soft-delete flag, defaults to `false` |
| `createdDate` | `Instant` | `@CreatedDate` |
| `updatedDate` | `Instant` | `@LastModifiedDate` |
| `deletedDate` | `Instant` | Set when `deleted` flips to `true` |
| `createdBy` | `UUID` | `@CreatedBy` — public ID of the acting user |
| `updatedBy` | `UUID` | `@LastModifiedBy` — public ID of the acting user |

- Auditing is driven by `JpaAuditingConfig`, whose `AuditorAware<UUID>` reads
  the current user's public ID from the Spring Security context.
- **Soft delete**: repositories never hard-delete. Entities are annotated with
  `@SQLDelete(sql = "... set deleted = true, deleted_date = now() ...")` and
  `@SQLRestriction("deleted = false")`, so deleted rows drop out of queries
  automatically.

## Conventions

- Package names are lowercase and singular (`user`, `insurer`,
  `serviceprovider`).
- REST base paths are plural kebab-case (`/api/v1/users`,
  `/api/v1/service-providers`).
- Request DTOs are validated with `jakarta.validation` (`@Valid` in
  controllers).
- All errors are normalized to `ApiError` by `GlobalExceptionHandler`, always
  serialized as `application/json` regardless of the request's negotiated content
  type (so error bodies never fail on non-JSON `Accept`/path extensions).
  `IllegalStateException` → 409, `ResourceNotFoundException` → 404,
  `IllegalArgumentException` → 400, `MethodArgumentTypeMismatchException`
  (a `@RequestParam`/`@PathVariable` that fails to convert — e.g. a
  non-ISO date, an unparsable UUID, an unknown enum constant) → 400. Without
  the last of these, a malformed query param falls through to the catch-all
  `Exception` handler and reports a misleading 500 instead of a 400.
- `PATCH` endpoints are partial updates: every field on the patch DTO is
  optional, and only fields present (non-null) in the request are applied —
  unlike `PUT`, which requires the full resource and replaces every field.
  (`OrganizationController.patch`/`OrganizationPatchRequest` is the reference
  implementation; other `PATCH` endpoints in this codebase, e.g. `Visitor`'s
  `/status` and `/entry-exit`, are narrower single-purpose actions rather than
  general partial updates.)
- Database schema changes ship as Flyway migrations
  (`src/main/resources/db/migration/`); Hibernate `ddl-auto` is never used to
  manage the schema. See [Database Migrations (Flyway)](#database-migrations-flyway)
  for the file naming convention.

## REST Resources

| Feature           | Base Path                     | Entity Table        |
|-------------------|-------------------------------|---------------------|
| User              | `/api/v1/users`               | `users`             |
| Insurer           | `/api/v1/insurers`            | `insurers`          |
| Service Provider  | `/api/v1/service-providers`   | `service_providers` |
| Policy            | `/api/v1/policies`            | `policies`          |
| Benefit           | `/api/v1/benefits`            | `benefits`          |
| Visitor           | `/api/v1/visitors`            | `visitors`          |
| Visitor Benefit   | `/api/v1/visitor-benefits`    | `visitor_benefits`  |
| Pre-authorization | `/api/v1/preauthorizations`   | `preauthorizations` |
| Claim             | `/api/v1/claims`              | `claims`            |
| Invoice           | `/api/v1/invoices`            | `invoices`          |
| ICD-11 Code       | `/api/v1/icd11-codes`         | `icd11_codes`       |
| Procedure         | `/api/v1/procedures`          | `procedures`        |
| Procedure Upload  | `/api/v1/procedures/uploads`  | `procedure_uploads`, `procedure_upload_rows` |
| Department        | `/api/v1/departments`         | `departments`       |
| Medical Service   | `/api/v1/medical-services`    | `medical_services`  |
| Organization      | `/api/v1/organizations`       | `organizations`     |
| OTP               | `/api/v1/otps`                | `otps`               |
| Reports           | `/api/v1/reports`             | (reads from existing tables) |
| Member Statement  | `/api/v1/member-statements`   | — (computed, see [Member Statement Report](#member-statement-report)) |

## Policy Tokenization (Quota Management)

The system enforces **per-insurer policy quotas** to prevent insurers from overselling policies.

**Concept:**
- Each `Insurer` is allocated a fixed number of policies via `policyToken` (e.g., 1000 policies for Minet Insurance)
- When a `Visitor` is created using a policy backed by an insurer, that insurer's available quota decreases by 1
- The system prevents visitor creation if the policy's backing insurer has exhausted their quota (policyToken ≤ 0)
- If a visitor is deleted, the quota is restored

**Data Model:**
- `Insurer.policyToken: Long` — available (unconsumed) policies for this insurer
- `InsurerResponse.availablePolicies: Long` — exposed in API responses (same as policyToken, defaults to 0 if null)

**Event-Driven Flow:**

1. **Visitor Creation → Policy Consumption**
   - `VisitorServiceImpl.create()` validates that the backing insurer has `policyToken > 0`
   - If validation passes, visitor is saved and `VisitorCreatedEvent` is published
   - `PolicyConsumptionListener` receives the event and decrements `policyToken` for the backing insurer
   - Example: Minet Insurance 1000 → 999 when first visitor is created

2. **Visitor Deletion → Policy Restoration**
   - `VisitorServiceImpl.delete()` soft-deletes the visitor and publishes `VisitorDeletedEvent`
   - `PolicyRestorationListener` receives the event and restores (increments) `policyToken` for the backing insurer
   - Example: Minet Insurance 999 → 1000 when that visitor is deleted

3. **Quota Exhaustion**
   - When `policyToken` reaches 0, any attempt to create a visitor using that insurer's policy fails with:
   - `IllegalStateException: "Insurer 'Minet Insurance' has no available policies left"`
   - HTTP 400 (Bad Request)

**Implementation Details:**

| Component | Responsibility |
|-----------|-----------------|
| `PolicyConsumptionListener` | Listens to `VisitorCreatedEvent`; decrements `policyToken` for the backing insurer |
| `PolicyRestorationListener` | Listens to `VisitorDeletedEvent`; restores `policyToken` for the backing insurer |
| `VisitorServiceImpl.requireInsurerWithQuota()` | Pre-creation validation; checks the backing insurer has available policies, returns it |
| `VisitorServiceImpl.delete()` | Publishes `VisitorDeletedEvent` after soft-delete |
| `InsurerResponse.availablePolicies` | Exposes quota count in API responses for admin monitoring |

**Example API Usage:**

```bash
# Create insurer with 1000 policies
POST /api/v1/insurers
{
  "name": "Minet Insurance",
  "policyToken": 1000
}

# Create policy linked to insurer
POST /api/v1/policies
{
  "insurerId": "<insurer-id>",
  "status": "ACTIVE"
}

# Create first visitor → Minet.policyToken: 1000 → 999
POST /api/v1/visitors
{ "policyId": "<policy-id>", ... }

# Create second visitor → Minet.policyToken: 999 → 998
POST /api/v1/visitors
{ "policyId": "<policy-id>", ... }

# Check available policies
GET /api/v1/insurers/<insurer-id>
# Response includes: "availablePolicies": 998

# After 1000 visitors created, next attempt fails
POST /api/v1/visitors
# 400 Bad Request: "Insurer 'Minet Insurance' has no available policies left"
```

## Messaging (RabbitMQ)

- Uses `spring-boot-starter-amqp`, with exchanges, queues, and bindings
  declared in `config/RabbitConfig.java`.
- Services publish domain events through `common/messaging/EventPublisher`
  (a thin wrapper over `RabbitTemplate`) — for example `claim.approved`,
  `preauthorization.decided`, and `policy.activated` — to drive notifications
  and downstream integrations.
- Listeners (`@RabbitListener`) live inside the feature package that consumes
  the event.

## Notifications (Policy Document Email)

When a `Visitor`'s cover becomes `ACTIVE`, the `notification` package
emails them a personalized policy certificate as a PDF attachment:

- `VisitorActivatedNotificationListener` sends the certificate on two paths,
  both gated on `ACTIVE`: `VisitorStatusChangedEvent` with `newStatus == ACTIVE`
  (a transition), and `VisitorCreatedEvent` when the newly created visitor is
  already `ACTIVE` (the default status), so visitors created active still get a
  certificate without a separate activation step. Unlike
  the sibling `visitorbenefit.VisitorStatusChangedListener` (which stays
  synchronous and in-transaction because it must mirror the status onto
  `VisitorBenefit` rows consistently), this listener uses
  `@TransactionalEventListener(phase = AFTER_COMMIT)`: sending mail over SMTP
  inside the same transaction that changed the visitor's status would risk
  rolling back a legitimate status change if the mail server is slow or
  unreachable. Any failure here is caught and logged, never propagated — a
  broken mail server must never affect the visitor status API's correctness.
  Re-activation (e.g. `ACTIVE` → `SUSPENDED` → `ACTIVE`) intentionally
  re-sends the certificate; that's treated as a new, valid activation, not a
  duplicate to guard against.
- The listener composes data via `VisitorService`, `PolicyService`,
  `VisitorBenefitService`, and `InsurerService` (the same "fan-in at a
  boundary" shape already used for `VisitorDetailResponse`), builds a
  `PolicyDocumentData` holder (internal to
  the package, not a DTO — it never crosses the web boundary), and passes it
  to `PolicyDocumentRenderer`. The underwriter logo comes from the first
  backing insurer with a non-blank `Insurer.logoUrl`; when present the template
  renders it as an `<img>`, otherwise it falls back to the dashed placeholder.
  Logo URLs are normalized by `common.util.LogoUrlNormalizer` so they return
  raw image bytes: Dropbox share links (`www.dropbox.com/...?dl=0`, which serve
  an HTML preview the PDF renderer can't read) are rewritten to the
  `dl.dropboxusercontent.com` direct-download host with `dl=1`. Normalization
  is applied both on save (`InsurerMapper`) and defensively at render time (for
  any logo stored before this was added).
- `PolicyDocumentRenderer` has no dependency on any other feature's service —
  it only knows how to render `templates/policy-certificate.html` (Thymeleaf) to
  HTML, then converts that HTML to PDF bytes via `openhtmltopdf`. The masthead
  shows only the underwriter's logo, centered — it carries no Government of
  Kenya branding (no crest, no "REPUBLIC OF KENYA" / "MINISTRY OF HEALTH" /
  "KENYA CARES" text). The
  template deliberately excludes `Visitor.underlyingConditions`: none of the
  real insurer certificates this template is modeled on embed a free-text
  medical-conditions field, and there's no reason to widen PII exposure over
  email with it (see `policy-document-analysis.md` for the full reference
  analysis).
- Each visitor's certificate carries a `Visitor.certificateSerialNumber`
  (`<INSURER-PREFIX>-<YEAR>-<6-digit sequence>`, e.g. `ACME-2026-000123`),
  minted by `CertificateSerialNumberGenerator` (mirrors
  `procedure.ProcedureCodeGenerator`'s shape: a Postgres sequence,
  `certificate_serial_seq`, `nextval()`'d via `VisitorRepository`, formatted
  with the first word of the issuing insurer's name uppercased as prefix and
  the mint-time calendar year as a label — the sequence itself is global and
  never resets). `VisitorServiceImpl` mints it once, the first time a visitor
  transitions to `ACTIVE` (either at `create()` or via `applyStatusUpdate()`),
  and persists it on `Visitor`; it's left untouched on any later
  `SUSPENDED → ACTIVE` reactivation, so one visitor keeps the same serial for
  the life of their cover even though the certificate email itself is
  re-sent. It's rendered in the certificate's `.meta` strip and referenced by
  the verification copy ("Verify this certificate at kenyacares.go.ke/verify
  using the Certificate Serial Number above").
- The activation email carries up to three attachments: the personalized
  `policy-certificate-<passportNumber>.pdf` (rendered per visitor), a
  `premium-receipt-<passportNumber>.pdf` (rendered per visitor, see below),
  and the static policy wording `templates/Policy_Document_July_2026.pdf`,
  loaded once from the classpath and cached. If the bundled document can't be
  read it is logged and skipped so the certificate still goes out.
- The premium receipt is rendered the same way as the certificate —
  `PolicyDocumentRenderer.renderPremiumReceiptPdf` processes
  `templates/premium-receipt.html` (Thymeleaf) to HTML then to PDF via
  `openhtmltopdf` — from a `PremiumReceiptData` holder (visitor full name,
  passport number, insurer name, plus `totalPremium`, `pcfLevy`,
  `insurancePremiumLevy`, `stampDuty`, `trainingLevy` fetched via
  `PremiumReceiptService.get()`, the same singleton levy-rate config exposed
  by `GET /api/v1/premium-receipts` — see
  [Premium Receipt (Singleton Levy Rates)](#premium-receipt-singleton-levy-rates)).
  Like the rest of this listener, a failure anywhere in this path (including
  rendering) is caught by the same top-level try/catch and blocks the whole
  activation email rather than partially sending it.
- The template is styled as a narrow, monospace thermal cash-register
  receipt (perforated top/bottom edges, dashed dividers, a decorative
  barcode) rather than the boxed A4 layout used by the certificate/claim
  receipt templates, to match a reference mockup. The levy fields
  (`pcfLevy`, `insurancePremiumLevy`, `trainingLevy`, each a fraction) are
  shown as their own line items with both the rate and a computed amount
  column (`totalPremium * rate`, rounded to 3dp — `PolicyDocumentRenderer`
  computes `pcfLevyAmount`/`insurancePremiumLevyAmount`/`trainingLevyAmount`
  and passes them into the template alongside `receipt`); `stampDuty` (a
  flat amount) is shown as its own line item with no rate, and its amount
  column is a hardcoded `0.308` in the template rather than derived from
  any `PremiumReceiptData` field. The bottom
  `TOTAL PREMIUM (USD)` row simply echoes `totalPremium` unchanged — it is
  not a sum of the line items above it.
- `common/email/EmailService` is a thin, domain-agnostic wrapper over
  `JavaMailSender` (mirrors `common/messaging/EventPublisher`'s catch-and-log
  style) — it never logs the email body or PDF bytes, only the outcome. It
  accepts either a single attachment or a `List<EmailAttachment>`.
- SMTP config (`spring.mail.*`) is sourced from `SMTP_*` env vars with
  STARTTLS explicitly required (Spring Boot does not enable it by default);
  `app.mail.from` and `app.mail.emergency-assistance.{phone,email}`
  (`config/MailProperties.java`) hold the small amount of static content our
  domain model doesn't capture (a 24/7 helpline is not stored per policy —
  every reference insurer hardcodes it too).
- No "document sent" tracking column exists — a resend on re-activation is
  desired behavior, not a defect.

## OTP (Point-of-Service Verification)

The `otp` package issues six-digit email codes used to confirm a visitor's
identity at the point of service (e.g. at a hospital reception), via two
endpoints:

- `POST /api/v1/otps/send` (`SendOtpRequest`: `email`, `serviceProviderId`) —
  generates a code and emails it. Returns `202 Accepted`.
- `POST /api/v1/otps/verify` (`VerifyOtpRequest`: `email`, `serviceProviderId`,
  `otp`) — checks the code. Returns `200 OK` on success.

Design notes:
- `Otp` extends `BaseEntity` and is **deliberately standalone** — no FK to
  `Visitor`/`User`. Columns are `otp` (stored **unhashed** — the code is
  low-entropy and short-lived, so hashing adds little), `expiryTime`, `email`,
  and `serviceProviderId` (validated against `ServiceProviderService.exists`,
  but not otherwise related to the entity).
- Codes expire 10 minutes after generation; verifying a code past that returns
  a distinct `"Otp expired"` message. A wrong code or no matching record both
  return a generic `"Invalid otp"` message, so a caller can't distinguish
  "no OTP was ever sent" from "you guessed wrong" (both `409`, via
  `IllegalStateException`, same as the rest of the API's conflict cases).
- **Invalidation reuses the existing soft-delete convention** rather than a
  new status column: a successfully verified OTP, and any OTP superseded by a
  resend for the same `(email, serviceProviderId)` pair, is deleted via
  `OtpRepository.delete(...)`, which — like every other entity in this
  codebase — triggers `@SQLDelete` (`deleted = true`) instead of a hard
  delete, so `@SQLRestriction("deleted = false")` makes it invisible to
  subsequent lookups.
- Lookups (both invalidating a prior code on resend, and matching a code on
  verify) are scoped to `(email, serviceProviderId)` together, not `email`
  alone — a visitor could plausibly have a live code at two different service
  providers at once, and codes shouldn't cross between them.
- **Sending follows the `AFTER_COMMIT` event pattern** used by
  `VisitorActivatedNotificationListener`: `OtpServiceImpl.send()` persists the
  `Otp` row and publishes `OtpGeneratedEvent`; `OtpNotificationListener`
  (`@TransactionalEventListener(phase = AFTER_COMMIT)`) sends the email via
  `EmailService` afterward, so a slow/unreachable mail server can never roll
  back the OTP row. Failures are caught and logged, never propagated.
- The sending mailbox is **not** derived from the visitor/policy (the `Otp`
  entity has no path to an `Insurer`). It's resolved via
  `OrganizationService.getEntityById` from a fixed organization id
  (`OtpNotificationListener.OTP_SENDER_ORGANIZATION_ID`) — `Organization`
  carries `host`/`port`/`notificationEmail`/`notificationEmailPassword`
  directly, so this reads the mailbox straight off that entity rather than
  hopping through `Insurer` (unlike
  `VisitorActivatedNotificationListener.resolveMailSettings`, which is handed
  an `Insurer` already resolved from the visitor's policy and has no
  organization id to look up). Falls back to the global `MailProperties`
  mailbox when the organization's SMTP fields aren't fully configured, or
  when no organization matches the id (`ResourceNotFoundException` from
  `getEntityById` is caught, not propagated). The id is hardcoded in source
  rather than externalized to configuration — a known tradeoff, accepted for
  now, that ties this behavior to whichever environment's database has a
  matching organization row.
- No rate limiting or max-attempts/lockout policy exists yet (no such
  infrastructure — no Bucket4j/Resilience4j, no `@Scheduled` jobs — exists
  anywhere else in this codebase either); this was explicitly deferred rather
  than added speculatively.
- Expired rows are not purged by a scheduled job; they're simply excluded from
  matches by the expiry-time check in `OtpServiceImpl.verify` and superseded
  by later resends, and accumulate in the table over time (same tradeoff as
  every other soft-deleted entity in this codebase).

## Premium Receipt (Singleton Levy Rates)

The `premiumreceipt` package holds the levy rates applied when computing a
policy's premium: `totalPremium` (base rate, `BigDecimal`), `pcfLevy`,
`insurancePremiumLevy`, `trainingLevy` (percentages expressed as `0`–`1`
fractions), and `stampDuty` (a flat `BigDecimal` amount, not a percentage).
Two endpoints, both under `/api/v1/premium-receipts` (no `{id}` path
variable):

- `GET /api/v1/premium-receipts` — any authenticated user.
- `PATCH /api/v1/premium-receipts` — `ADMIN` only (`@PreAuthorize`), partial
  update: only non-null fields in `PremiumReceiptPatchRequest` are applied,
  matching the `Organization` PATCH convention.

Design notes:
- **Exactly one row ever exists, and there is no `POST`/`DELETE` endpoint.**
  A Flyway migration (`V202608302028__create_premium_receipts.sql`) creates
  the table together with a unique expression index
  (`CREATE UNIQUE INDEX one_row_only ON premium_receipts ((true))`), so a
  second row can never be inserted even by future/manual SQL. A second
  migration (`V202608302029__seed_premium_receipt.sql`) inserts the single
  row with a hardcoded UUID
  (`PremiumReceiptServiceImpl.SINGLETON_ID = 00000000-0000-0000-0000-000000000001`)
  instead of relying on `BaseEntity`'s `@UuidGenerator`. GET/PATCH always
  fetch by that fixed constant — no create-or-lookup branching needed.
- Percentage fields are validated with `@DecimalMin("0")`/`@DecimalMax("1")`;
  `totalPremium`/`stampDuty` with `@DecimalMin("0")` only, since they're
  amounts, not fractions.

## Claims Reports

The `report` feature provides claim receipts and service-provider claims
reports in PDF, Excel, and JSON formats. It has **no new tables** — all data
is assembled from existing claim, invoice, visitor, benefit, and provider
entities through their respective service interfaces.

### Claim Receipt (PDF)

A per-claim itemized receipt available as PDF or JSON. Emphasis on submitted
claims since they carry invoice information and line items.

- **Endpoints**: `GET /api/v1/reports/claims/{claimId}` (JSON),
  `GET /api/v1/reports/claims/{claimId}/pdf` (PDF download)
- **Content**: claimant details (name, passport, nationality, travel dates),
  benefit name, service provider name, diagnoses, procedures, and a
  consolidated invoices table with service name, department, quantity, unit
  price, and amount — all in KES. Claim ID (UUID) and FX conversion details
  are deliberately omitted from the receipt.
- **Rendering**: Thymeleaf template (`templates/report/claim-receipt.html`)
  to HTML, then HTML to PDF via `openhtmltopdf` — the same pipeline as the
  policy certificate. Table-based layout with absolute `pt` units for
  renderer compatibility.
- **Assembly**: `ReportServiceImpl` resolves display names through
  cross-feature services: `BenefitService.namesByIds`,
  `ServiceProviderService.getById`, `Icd11CodeService.getById`,
  `ProcedureService.getById`, `MedicalServiceService.getById` (for service
  name and department name). Missing references are silently skipped or
  shown as "Unknown".

### Service Provider Claims Report

A summary report of all claims for a given service provider, available as
paginated JSON (for UI tables), PDF, or Excel.

- **Endpoints**:
  - `GET /api/v1/reports/service-providers/{providerId}/claims` —
    paginated JSON with optional `status`, `dateFrom`, `dateTo` query params
  - `GET /api/v1/reports/service-providers/{providerId}/claims/pdf` —
    full report as PDF
  - `GET /api/v1/reports/service-providers/{providerId}/claims/excel` —
    full report as XLSX (single Claims sheet)
- **Summary**: total claims, total claimed amount (KES), total approved
  amount (KES). Aggregate queries use `COALESCE(:param, column)` for
  optional filters to avoid PostgreSQL type-inference errors with nullable
  JPQL parameters.
- **Claims table**: status, date, visitor name, benefit name, claimed
  amount, approved amount. No claim ID (UUID) column.
- **Excel**: Apache POI, single Claims sheet with styled headers and
  currency formatting. No separate summary sheet.
- **PDF**: A4 landscape, summary bar at top, claims table below. No
  status/count breakdown table, no footer.
- **Access**: all authenticated roles (USER, ADMIN, AGENT, PROVIDER_USER,
  INSURER_USER).

## Member Statement Report

A per-member report — benefit allocation/utilization/balance plus a
transaction listing of the member's claims — with two endpoints, both
resolving the member by `passportNumber` (mirrors the visitor lookup
convention):

- `GET /api/v1/member-statements?passportNumber=…` → `MemberStatementResponse`
  (JSON). Returns the member's full, all-time transaction history — no date
  filtering.
- `GET /api/v1/member-statements/export?passportNumber=…&fromDate=…&toDate=…&exportType=PDF|EXCEL`
  → the same statement rendered as a file download (`Content-Disposition:
  attachment`). `fromDate`/`toDate` are required and scope which
  **transaction rows** are included; `fromDate` after `toDate` is rejected
  with `400`.

Composed entirely from existing feature services (`VisitorService`,
`VisitorBenefitService`, `PolicyService`, `ClaimService`, `BenefitService`,
`ServiceProviderService`) — the feature has no entity or table of its own.

- **Benefit summary** (`MemberStatementResponse.benefits`) is exactly
  `VisitorBenefitService.listAllByVisitor(visitorId)` — the same
  `limitAmount`/`utilizedAmount`/`balance` fields described under
  [VisitorBenefit](#core-insurance-flow) — reused as-is. It always reflects
  the member's **all-time** standing: the export's `fromDate`/`toDate` never
  affects it, only the transaction list below it.
- **Transactions** (`MemberStatementResponse.transactions`) are built **one
  row per `Claim`**, sourced entirely from the claim itself — deliberately
  not from `Invoice`. `transactionDate` = `claim.createdDate` (converted to
  `LocalDate` via UTC), `amount` = `claim.claimedAmount`; `benefitName`/
  `serviceProviderName` are resolved from the claim's `benefitId`/
  `serviceProviderId`. This means the transaction total always agrees with
  the benefit summary's `utilizedAmount` — both are sums of
  `claim.claimedAmount` over the same claims (see
  [VisitorBenefit](#core-insurance-flow)). Invoices were deliberately left
  out: `Invoice.claimId` and `Claim.invoiceIds` are kept in sync by
  `attachInvoice()` (which updates both the FK and the join table), and
  `ClaimResponse` reads invoices from `invoices.claim_id` (source of truth).
  The transaction list uses claim-level `claimedAmount` for simplicity and
  consistency with the benefit summary's `utilizedAmount`. On export, rows are filtered to
  `transactionDate` within `[fromDate, toDate]` inclusive.
- `ServiceProviderService.namesByIds(Collection<UUID>)` was added (mirroring
  `BenefitService.namesByIds`) so provider names resolve in one batched call
  across a member's claims rather than per-row; `ClaimService.listByVisitor(UUID)`
  was added the same way, backed by a new `ClaimRepository.findAllByVisitorId`.
- **Excel export** (`MemberStatementExcelWriter`) builds the workbook with
  Apache POI using the same plain-cells convention as
  `ProcedureUploadWorkbooks` (no styling/formulas/auto-sizing) — a header
  block (member name/number), the transaction table (or a
  "No member statement transaction data found" row when empty, matching the
  legacy export this replaces), then the benefit summary table. The legacy
  export's "INVOICE NUMBER" column is dropped — there's no invoice backing a
  row anymore (see above).
- **PDF export** (`MemberStatementPdfRenderer`) uses the same
  Thymeleaf-to-openhtmltopdf pipeline as `PolicyDocumentRenderer`
  (`templates/member-statement.html`), decoupled the same way — the renderer
  only lays out data it's handed, all fan-in happens in
  `MemberStatementServiceImpl`.
  - The masthead text "MINET KENYA INSURANCE BROKERS" is a fixed constant in
  both renderers, not resolved per-insurer — Minet is the broker operating
  this system for every member regardless of which insurer underwrites their
  policy, so this is a masthead, not a per-member field.

## USSD Provider Panel (Find Hospital)

The USSD "Find Hospital" feature lets travellers search for in-network
healthcare providers by **county** or **town/area**. Provider data is sourced
from an Excel spreadsheet (`provider-panel.xlsx`, bundled in
`src/main/resources/`) and loaded into memory at application startup — no
database table backs this feature.

**Data source:**

- `provider-panel.xlsx` contains two sheets:
  - **NAIROBI COUNTY** — providers keyed by area/neighborhood (e.g. Karen,
    Westlands, Upperhill). Columns: Area Name, Provider Name, Physical
    Address, Services. The area doubles as the town; county is always
    `NAIROBI`.
  - **upcountry** — providers keyed by town and county. Columns: Town,
    County, Provider Name, Physical Location, Services.
- Apache POI parses both sheets on startup (`ProviderPanelLoader`,
  `@PostConstruct`), producing a `List<ProviderPanelEntry>` held in memory.
  Currently ~739 entries.

**Architecture:**

```
provider-panel.xlsx (classpath)
        │
        ▼
ProviderPanelLoader        ← @PostConstruct, POI → List<ProviderPanelEntry>
        │
        ▼
ProviderPanelService       ← searchByCounty(q), searchByTown(q)
        │                   case-insensitive partial match (ILIKE-style)
        ▼
UssdServiceImpl            ← wired into handlePromptCountyName / handlePromptTownName
                            results paginated 5 per USSD screen
```

**USSD flow:**

```
Main Menu → 1. Find Hospital
  → 1. County → "Enter county name:" → user types query
    → ProviderPanelService.searchByCounty(query)
    → Paginated results (5 per screen), 9 = next page, 0 = back
  → 2. Town → "Enter town name:" → user types query
    → ProviderPanelService.searchByTown(query)
    → Same paginated display
  → 3. Border Point (Coming Soon)
  → 4. Nearest Tourist Attraction (Coming Soon)
  → 0. Main Menu
```

**Key design decisions:**

- **In-memory, not DB:** provider panel data changes infrequently; an
  in-memory list avoids a DB table and keeps the USSD response latency
  sub-millisecond after startup. To update providers, replace the Excel file
  and restart.
- **Search behaviour:** case-insensitive substring match on county/town name.
  Partial matches are intentional — a user typing "Momb" finds Mombasa.
- **Pagination:** 5 results per USSD screen (160-char limit). "9. Next page"
  cycles through results; "0. Back" returns to the hospital sub-menu.
- **No results:** returns to the sub-menu with "No providers found for
  '{query}'."

## API Documentation (Swagger)

- Uses `springdoc-openapi-starter-webmvc-ui`: the UI is served at
  `/swagger-ui.html` and the spec at `/v3/api-docs`.
- `config/OpenApiConfig.java` holds the API metadata and the JWT bearer
  security scheme, so the **Authorize** button works in the UI.
- Both endpoints are permitted in `SecurityConfig` for non-production profiles
  only.

## Security

- The JWT signing key is a strong secret of at least 256 bits, supplied via an
  environment variable or secret manager — never committed to the repository
  or `application.yml`.
- Access tokens are short-lived and paired with refresh tokens (handled by the
  `auth` feature).
- Passwords are hashed with BCrypt (`PasswordEncoder` bean in
  `SecurityConfig`).
- **Field-level encryption at rest (`common/crypto`):** sensitive PII and
  medical columns are encrypted with AES-256-GCM via JPA
  `AttributeConverter`s (`EncryptedStringConverter`,
  `EncryptedLocalDateConverter`), applied with `@Convert` on the entity
  fields listed below. Encryption is transparent below the service layer —
  entities expose plaintext getters/setters; only the stored column holds
  ciphertext (base64 of IV || ciphertext || GCM tag, fresh random IV per
  write).
  - `visitor.Visitor`: `fullName`, `passportNumber`, `dateOfBirth`,
    `nationality`, `address`, `email`, `phoneNumber`,
    `underlyingConditions`, `nextOfKinName`, `nextOfKinPhone`.
  - `biometric.BiometricVerification`: `subjectIdNumber`, `embededToken`.
  - `claim.Claim`: `description`, `prescription`, `decisionReason`.
  - `preauthorization.Preauthorization`: `serviceDescription`,
    `decisionReason`.
  - Excluded: `Visitor.facePhotoUrl` (a pointer to externally-stored image,
    not sensitive payload) and `User.email`/`phoneNumber` (auth principal,
    queried on every login — encrypting it would need its own blind index
    and touches the security-critical login path).
  - **Passport-number lookups:** randomized AES-GCM ciphertext can't be
    searched or uniquely constrained by SQL, but `Visitor.passportNumber`
    is looked up (`VisitorRepository.findByPassportNumberHash` /
    `existsByPassportNumberHash*`) and DB-unique among live rows. A second,
    deterministic column — `passportNumberHash`, an HMAC-SHA256 "blind
    index" computed by `BlindIndexService` over the trimmed/uppercased
    passport number — is stored alongside the encrypted value and used for
    all lookup/uniqueness checks instead. `VisitorServiceImpl` computes and
    sets it on every create/update.
  - **Keys:** `APP_ENCRYPTION_KEY` (AES data key) and
    `APP_ENCRYPTION_BLIND_INDEX_KEY` (HMAC key) are separate base64-encoded
    256-bit secrets supplied via env vars (`app.encryption.*` in
    `application.yml`), never committed. Key retrieval goes through
    `EncryptionKeyProvider`, an interface `EnvEncryptionKeyProvider`
    implements today — swap in a KMS/Vault-backed implementation later
    without touching entities or converters.
  - **Rotation (not yet implemented):** rotating `APP_ENCRYPTION_KEY`
    requires re-encrypting all existing ciphertext; rotating
    `APP_ENCRYPTION_BLIND_INDEX_KEY` requires rehashing every stored
    `passportNumberHash`. Both are out of scope for v1.
  - **Deploying to an environment with pre-existing plaintext data:**
    `V202608171753__visitor_encryption_columns.sql` adds
    `passport_number_hash` as nullable and un-indexed on purpose — an
    environment with existing visitor rows can't satisfy a `NOT NULL`
    constraint on a brand-new column in one step, and the new code's
    `@Convert` converters would fail decrypting old plaintext on first
    read. Roll out in two stages:
    1. Deploy with `APP_ENCRYPTION_BACKFILL_ENABLED=true` for exactly one
       run. `EncryptionBackfillRunner` (`common/crypto`) reads existing
       plaintext via raw JDBC (bypassing JPA, since the converters assume
       ciphertext), encrypts it, and computes `passport_number_hash` for
       rows still missing it. It's idempotent (columns that already
       decrypt successfully are left alone), so it's safe to re-run if
       interrupted. Set the flag back to `false` afterward.
    2. Once backfill is confirmed complete (no `NULL`
       `passport_number_hash` among live rows, no duplicate hashes), ship
       a follow-up migration adding `alter table visitors alter column
       passport_number_hash set not null` and the
       `uq_visitors_passport_number_hash` unique index (see the dropped
       `uq_visitors_passport_number` index it replaces).
    A fresh environment with no pre-existing data can skip the backfill
    step entirely — the hash is always populated at write time by
    `VisitorServiceImpl`.

## Database Connection Pool (HikariCP)

Spring Boot's default pool is HikariCP. The sizing rule of thumb is
`pool size = (2 × CPU cores) + effective disk spindles`; more connections than
this degrades throughput rather than improving it.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10       # Good default for a 4-core host with SSD storage
      minimum-idle: 10            # Keep the pool fixed-size; avoids resize churn
      connection-timeout: 30000   # ms
      max-lifetime: 1800000       # 30 min, below typical DB/firewall timeouts
```

Revisit `maximum-pool-size` only with load-test evidence, and ensure
PostgreSQL's `max_connections` comfortably exceeds
`pool size × application instances`.

## Database Migrations (Flyway)

**Naming convention (from V033 onward):**

```
V<timestamp>__<description>.sql
```

- `timestamp` — `yyyyMMddHHmm`, the moment the migration file was created.
  No sequential number prefix.
- `description` — snake_case summary of the change.

Example: `V202608131430__add_claim_status.sql`

**Problem this solves:** with plain `V<seq>__description.sql` naming, two
developers branching off the same `HEAD` would independently pick the same
next sequence number (e.g. both write `V033__...`). Flyway treats the part
before `__` as the version, so two files with an identical version number are
a hard conflict — whichever branch merges second fails to apply. A pure
12-digit timestamp is unique per file with zero coordination between
developers — nobody needs to know what number a teammate on another branch
just used — and it sorts correctly against every future migration without
ever repeating.

**Config implication:** because merge order no longer guarantees version
order (a migration created earlier can merge and reach an environment after
one created later has already been applied there), Flyway must be allowed to
apply migrations out of strict version order:

```yaml
spring:
  flyway:
    enabled: true
    out-of-order: true   # required by the timestamp-based migration naming convention
```

**Existing migrations (`V001` … `V032`) are not renamed** — this convention
applies only to new migrations going forward. Flyway compares version
numbers numerically (not by string length), so plain two/three-digit
versions like `V032` sort before any 12-digit timestamp automatically; no
renumbering or padding is needed.

## Code Practices

- **Thin controllers**: no business logic in controllers. They validate input
  (`@Valid`), delegate to the service, and map the result to a response.
- **Small, single-purpose methods**: aim for a maximum of ~20 lines; each
  method does one specific task. Extract private helpers rather than growing a
  method.
- **Unit tests** for every service (Mockito for collaborators) and mapper;
  controller slice tests with `@WebMvcTest` and `spring-security-test`;
  repository tests against H2.
- **Lombok** for boilerplate (`@Getter`, `@Builder`,
  `@RequiredArgsConstructor` for constructor injection). Avoid `@Data` on
  entities.

## Maven Dependencies

Parent: `spring-boot-starter-parent` 3.x. Explicit versions are only needed
where Spring Boot's dependency management does not provide one.

| Dependency | Scope | Purpose |
|---|---|---|
| `spring-boot-starter-web` | compile | REST controllers |
| `spring-boot-starter-data-jpa` | compile | Repositories, entities |
| `spring-boot-starter-validation` | compile | `@Valid` on request DTOs |
| `spring-boot-starter-security` | compile | Auth filter chain, role-based access |
| `spring-boot-starter-amqp` | compile | RabbitMQ messaging |
| `spring-boot-starter-mail` | compile | SMTP sending (`common/email/EmailService`) |
| `spring-boot-starter-cache` | compile | `@Cacheable` for `CurrencyConversionService` (`config/CacheConfig`) |
| `com.github.ben-manes:caffeine` | compile | In-memory cache backend (`fxRates`, 24h TTL) |
| `spring-boot-starter-thymeleaf` | compile | Policy document HTML templating |
| `io.github.openhtmltopdf:openhtmltopdf-pdfbox` | compile | HTML → PDF rendering for the emailed policy certificate |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` (2.6.x) | compile | Swagger UI + OpenAPI docs |
| `org.postgresql:postgresql` | runtime | Production database driver |
| `io.jsonwebtoken:jjwt-api` (0.12.x) | compile | JWT for `JwtTokenProvider` |
| `io.jsonwebtoken:jjwt-impl` (0.12.x) | runtime | JWT implementation |
| `io.jsonwebtoken:jjwt-jackson` (0.12.x) | runtime | JWT JSON serialization |
| `org.flywaydb:flyway-core` + `flyway-database-postgresql` | compile | Schema migrations |
| `org.projectlombok:lombok` | provided (optional) | Boilerplate reduction |
| `spring-boot-starter-test` | test | JUnit 5, Mockito, AssertJ |
| `spring-security-test` | test | `@WithMockUser`, security test support |
| `com.h2database:h2` | test | In-memory database for tests |

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.openhtmltopdf</groupId>
        <artifactId>openhtmltopdf-pdfbox</artifactId>
        <version>1.1.70</version>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.6.0</version>
    </dependency>

    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```