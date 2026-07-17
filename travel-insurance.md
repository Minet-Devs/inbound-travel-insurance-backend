# Minet Healthcare System — Foreigner's Journey

Source: whiteboard sketches in `screenshots/onboarding-process.jpeg` and `screenshots/claims-and-remittance-process.jpeg`. Transcribed and organized below; handwriting that was ambiguous is flagged inline.

## Onboarding process

High-level flow for a foreigner entering the country and getting covered:

**Business → Scheme → Categories → Covers**

The insurance product is organized as a business scheme broken into categories, each mapping to a cover tier with an associated policy period:

| Tier   | Policy period (months) |
|--------|-------------------------|
| Gold   | 12                       |
| Silver | 6                        |
| Bronze | 3                        |

### 1. ETA (Electronic Travel Authorization) Portal

- Entry point is the **ETA Portal**, where the foreigner selects a **cover option** (segment).
- Steps: **pick one option → payments → on success**
- On successful payment, a **cover is created** using a **basic KYC** flow.

### 2. Cover fetch / KYC exchange

- The **ETA** system calls **Inbound** to **fetch covers**.
- **Inbound** returns the **selected covers together with KYC** requirements.
- KYC is **passport-based** and captures the following member details:
  - Name
  - Date of birth
  - Country
  - Email
  - Phone number
  - Date in / date out of the country
  - Gender
  - Marital status (incl. next of kin)

### 3. Cover & documents

- **Cover**: defined by policy period — 3, 6, or 12 months (Bronze/Silver/Gold respectively).
- **Documents required**: Passport, Birth Certificate.

### Design principles called out on the board

1. **API security** — authentication on all integration points.
2. **Simplified version** — keep the onboarding flow lean.
3. **Easy support with fewer systems** — centralize support/operations into one place rather than spreading across multiple systems.

## Claims process

Foreigner's journey once they need to use their cover, starting from the healthcare provider:

**Providers → KYC → Benefits → Billing**

- **Providers** initiate the flow by performing **KYC** on the member:
  - Member number
  - Member verification (via **biometrics**)
- Once verified, the member's **benefits** are checked/applied.
- The provider then submits **billing** for the services rendered.

## Remittance process

Follows on directly from billing in the claims flow:

- Billing is **sent to the insurer's system for payment** (provider payments / remittance).
- This is the final step that settles funds owed to the provider for services covered under the member's benefits.
