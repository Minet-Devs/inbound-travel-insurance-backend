# Mandatory Inbound Travel Health Insurance — Implementation Summary

Summarized for engineering purposes from the Ministry of Health's **APPROVED Administrative
Framework for Implementation of the Mandatory Inbound Travel Health Insurance Program**
(Nov 2025) — see `APPROVED_ADMINISTRATIVE FRAMEWORK FOR IMPLEMENTATION OF THE MANDATORY
INBOUND TRAVEL HEALTH INSURANCE PROGRAM.pdf`.

Legal basis: Section 26(6) of the Social Health Insurance Act No. 16 of 2023 — every
non-Kenyan intending to enter and remain in Kenya for less than 12 months must hold a
travel health insurance cover designated by the Cabinet Secretary for Health. Section
26(7) empowers the Cabinet Secretary to put in place the regulatory and administrative
measures this framework describes.

## Program model

Kenya uses the **Designated** approach: the government designates a specific insurer,
product, and IT platform rather than letting travelers source cover from their country
of origin. This is what makes the cover verifiable at the point of entry and lets the
Ministry set minimum benefit standards, panel requirements, and data handling rules —
all of the requirements below flow from that choice.

## Policy period

The framework defines three cover periods (§6.1), not a Gold/Silver/Bronze tier system:

| Policy type | Duration |
|---|---|
| Single entry | Up to 30 days from date of entry |
| Single entry | 30–60 days from date of entry |
| IPMI (International Private Medical Insurance) | 60 days up to 12 months from date of entry |

This is the structure policy/cover-period logic needs to model. Today `Policy` only has
free-form `coverStartDate`/`coverEndDate` with no notion of these three period types —
see [Gaps](#whats-implemented-vs-what-the-framework-requires) below.

## Policy benefits & limits

Per §6.2/§6.3, the cover must include at least the following insured events, with a
cumulative benefit limit of **no less than USD 50,000**:

| Benefit | Scope | Minimum limit (USD) |
|---|---|---|
| Personal accident (death / permanent total disability) | Medical personnel, lab tests/imaging, ambulatory costs | — |
| Emergency medical expenses | Hospitalization incl. ICU, surgeon's fees, out-patient | 20,000 |
| Emergency medical evacuation | Site of event → nearest hospital, per doctor approval | 25,000 |
| Repatriation of mortal remains | Transport of body to country-of-origin airport | 5,000 |
| Hospital benefits (in/out-patient) | Incl. mental illness, pandemics/epidemics, medical transport | 1,000 (mental illness) |
| Prescription medicines | Prescribed (not OTC) medication for an insured event | 300 |

This is a **fixed minimum schedule mandated by regulation**, not a suggestion — unlike
today's generic `Benefit.name` (free text) + `limitAmount`, which has no fixed catalog
or minimums enforced anywhere in code.

**Claims & remittance**: the framework doesn't detail claims workflow beyond the benefit
schedule above. Existing understanding still holds: providers perform member KYC and
verification, apply benefits against the policy, submit billing, and billing is
remitted to the insurer for provider payment — this maps to the `preauthorization` and
`claim` packages already in the codebase.

## KYC / onboarding data requirements

Per §8.1, the government's e-portal ("Kenya Cares") must collect at minimum:

- Full name (as per passport)
- Gender
- Date of birth
- Address
- State and country of origin
- Mobile and email contacts
- Passport bio-data page (upload)
- Face photo (upload)
- Reason for travel
- Any underlying condition/ailment with prescribed medicines not easily accessible
- Duration of travel

Current `Visitor` entity covers: `fullName`, `passportNumber`, `dateOfBirth`, `gender`,
`nationality` (maps to country of origin), `email`, `phoneNumber`, `dateIn`/`dateOut`,
`maritalStatus`, next-of-kin name/phone. **Not yet modeled**: address, face photo
upload, reason for travel, underlying conditions/prescribed medicines. See
[Gaps](#whats-implemented-vs-what-the-framework-requires).

## IT / security / infrastructure requirements

Per §5.3, the platform is required to meet:

- PCI-DSS compliance (payment card handling)
- GDPR-equivalent compliance + Kenya Data Protection Act No. 24 of 2019
- SOC 2 security compliance
- 99.9% uptime guarantee
- Redundancy and full backup (two hosting sites, immediate failover)
- 24×7×365 support
- Capacity for at least 2 million transactions/year
- Data retention: active data 24 months, archived data 7 years
- Logging and monitoring of infrastructure and applications

## Required integrations

Per §5.2.4/§8.2, the platform must support:

- **eTA (Electronic Travel Authorization)** — API integration for cover verification at entry
- **API-PNR (Advanced Passenger Information)** — passenger data exchange
- **Immigration port-entry verification system** — API Gateway integration
- **CRM & claims management systems**
- **Payment gateway** — multi-channel: cards (Visa/Mastercard/Amex), mobile wallets
  (Apple Pay, Google Pay), WeChat/Alipay, M-PESA; must support collection at all land,
  sea, and air border points
- **Geolocation** for locating empaneled medical providers (customer-facing website)

None of these integrations exist in the codebase today.

## Data protection obligations

Insurance data here counts as **sensitive personal data** under the Data Protection Act
No. 24 of 2019 (health status, biometric data, marital status, family details, etc.),
and health data specifically is also governed by the Digital Health Act No. 15 of 2023.
Practical obligations for this system:

- Lawful, fair, and transparent processing; data collected must be accurate and for a
  specific, legitimate purpose.
- No cross-border transfer of sensitive personal data without proof of adequate
  safeguards or explicit data-subject consent (relevant if data is stored outside Kenya
  or shared with international reinsurers/TPAs).
- Default technical safeguards proportionate to the data collected, its processing
  extent, storage period, and foreseeable risk — reviewed and updated over time.
- As health data controller/processor, the insurer must notify the insured of Digital
  Health Act compliance requirements and obtain consent.

Directly relevant to the `visitor` and `biometric` packages, and to any future
cross-border data sharing with reinsurers or third-party administrators.

## What's implemented vs. what the framework requires

| Framework requirement | Codebase today |
|---|---|
| 3 policy period types (single-entry ≤30d, 30–60d, IPMI ≤12mo) | `Policy` has free-form `coverStartDate`/`coverEndDate`, no period-type concept |
| Fixed benefit catalog with mandated minimum limits | `Benefit` is generic free-text `name` + `limitAmount`, no catalog or minimums enforced |
| Full KYC field set incl. address, face photo, reason for travel, underlying conditions | `Visitor` covers identity/contact/dates/marital status/next-of-kin; missing address, photo, travel reason, medical conditions |
| eTA / API-PNR / immigration / payment gateway integrations | None implemented |
| Biometric/eKYC verification | Implemented — `biometric` package (`BiometricVerification`, eKYC/eCitizen integration) |
| Claims & preauthorization workflow | Implemented — `preauthorization` and `claim` packages |
| Data retention (24mo active / 7yr archived), 99.9% uptime, PCI-DSS/SOC2 | Not addressed at the application level; infrastructure/ops concern |
