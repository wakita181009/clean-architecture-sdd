# Feature Specification: Subscription Management API — Phase 2 (Coupons, Transfers, Breaking Changes)

**Feature Branch**: `spec4`
**Created**: 2026-03-11
**Status**: Clarified
**Input**: Extends subscription system with coupon management, subscription transfer, and breaking changes to Phase 0 business rules

Phase 2 extends the subscription system with **Coupon Management**, **Subscription Transfer**, and introduces **breaking changes** to Phase 0 business rules (configurable trial/grace periods, increased pause limit, discount scope change).

## Phase 0 Breaking Changes

The following Phase 0 rules are modified in this phase:

1. **Trial period**: Configurable per plan (0-60 days, was fixed 14 days). `trialDays=0` means no trial — subscription starts Active with immediate charge. FREE tier must have `trialDays=0`.
2. **Pause limit**: Increased from 2 to 3 pauses per billing period.
3. **Grace period**: Configurable per plan (3-30 days, was fixed 7 days).
4. **Discount scope**: Discounts now apply only to plan-related charges (PLAN_CHARGE, SEAT_CHARGE, ADDON_CHARGE). USAGE_CHARGE is excluded from discount calculation.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create Coupon (UC-015, Priority: P1)

An admin creates a reusable promotional coupon with configurable discount type, validity period, tier/interval restrictions, and redemption limits. Coupon codes are unique, uppercase alphanumeric with hyphens, max 32 characters.

**Why this priority**: Coupons are the foundation for the promotional system. Must exist before redemption.

**Independent Test**: Can be tested by creating a coupon with various configurations and verifying validation and persistence.

**Acceptance Scenarios**:

1. **Given** valid inputs, **When** creating coupon code="SAVE20" type=PERCENTAGE value=20 duration=3, **Then** coupon created with active=true, currentRedemptionCount=0
2. **Given** valid inputs, **When** creating coupon code="FLAT-10" type=FIXED_AMOUNT value=USD(10.00) currency=USD, **Then** coupon created
3. **Given** existing coupon "SAVE20", **When** creating "save20" (case-insensitive), **Then** 409 error (duplicate code)
4. **Given** code="WELCOME 2025" (contains space), **When** creating, **Then** 400 error (invalid format)
5. **Given** type=PERCENTAGE currency=USD, **When** creating, **Then** 400 error (PERCENTAGE must not specify currency)
6. **Given** type=FIXED_AMOUNT currency=null, **When** creating, **Then** 400 error (FIXED_AMOUNT must specify currency)
7. **Given** validFrom=2026-12-31 validUntil=2026-01-01, **When** creating, **Then** 400 error (validFrom must be before validUntil)
8. **Given** code with 33 characters, **When** creating, **Then** 400 error (max 32 characters)
9. **Given** lowercase code "welcome-2025", **When** creating, **Then** success, stored as "WELCOME-2025"

---

### User Story 2 - Redeem Coupon (UC-016, Priority: P1)

A customer applies a coupon code to their Active or Trial subscription. The coupon's discount replaces any existing discount. Validation checks tier/interval compatibility, currency match, redemption limits, and one-use-per-customer rule.

**Why this priority**: Coupon redemption is the primary customer-facing promotional feature. Required together with creation.

**Independent Test**: Can be tested by redeeming a valid coupon on an Active subscription and verifying the discount is applied and redemption is recorded.

**Acceptance Scenarios**:

1. **Given** Active subscription, valid coupon "SAVE20" (20%, 3 months), **When** redeeming couponCode="SAVE20", **Then** discount applied, remainingCycles=3, couponId set
2. **Given** Active subscription with existing 10% discount, **When** redeeming "SAVE20" (20%), **Then** old discount replaced with 20%
3. **Given** Trial subscription, **When** redeeming coupon, **Then** success, discount ready for first invoice
4. **Given** coupon code "SAVE20", **When** redeeming as "save20" (lowercase), **Then** success, matched case-insensitively
5. **Given** coupon durationMonths=null, **When** redeeming, **Then** discount remainingCycles=null (forever)
6. **Given** Paused subscription, **When** redeeming coupon, **Then** 409 error (must be Active or Trial)
7. **Given** coupon validUntil=yesterday, **When** redeeming, **Then** 409 error (expired)
8. **Given** coupon maxRedemptions=100 with count=100, **When** redeeming, **Then** 409 error (max reached)
9. **Given** customer already used coupon "SAVE20" previously, **When** redeeming again, **Then** 409 error (already used by customer)
10. **Given** STARTER subscription, coupon applicableTiers={PROFESSIONAL}, **When** redeeming, **Then** 409 error (tier incompatibility)
11. **Given** USD subscription, FIXED_AMOUNT coupon with EUR currency, **When** redeeming, **Then** 409 error (currency mismatch)
12. **Given** coupon count=5, **When** redeeming successfully, **Then** count becomes 6

---

### User Story 3 - Deactivate Coupon (UC-017, Priority: P2)

An admin deactivates a coupon to prevent new redemptions. Existing subscriptions with the coupon's discount continue using it until the discount naturally expires.

**Why this priority**: Admin management feature that completes the coupon lifecycle.

**Independent Test**: Can be tested by deactivating a coupon and verifying it can no longer be redeemed while existing discounts remain.

**Acceptance Scenarios**:

1. **Given** active coupon, **When** deactivating, **Then** active=false
2. **Given** subscription with discount from deactivated coupon, **When** next renewal, **Then** discount still applies (deactivation only prevents new redemptions)
3. **Given** already inactive coupon, **When** deactivating, **Then** 409 error
4. **Given** non-existent couponId=999, **When** deactivating, **Then** 404 error

---

### User Story 4 - Transfer Subscription (UC-018, Priority: P2)

An admin transfers a subscription from one customer to another. The subscription must be in Active, Paused, or Trial status, the plan must be transferable, and the target customer must not have an active subscription. All subscription state (discount, add-ons, credit balance, seats) is preserved.

**Why this priority**: Transfer is important for business operations (account consolidation, ownership changes) but is a less frequent operation.

**Independent Test**: Can be tested by transferring an Active subscription to another customer and verifying ownership change and state preservation.

**Acceptance Scenarios**:

1. **Given** Active subscription owned by customer 1, customer 42 has no active sub, **When** transferring to customer 42 with reason="Consolidation", **Then** subscription.customerId=42, transfer record created
2. **Given** Paused subscription, **When** transferring, **Then** success, still Paused, owned by new customer
3. **Given** Trial subscription, **When** transferring, **Then** success, still Trial, owned by new customer
4. **Given** Active subscription with 20% discount, 2 add-ons, USD(15.00) credit, 5 seats, **When** transferring, **Then** all state preserved on transferred subscription
5. **Given** subscription transferred 1→42, then 42→99, **When** listing transfer history, **Then** 2 SubscriptionTransfer records
6. **Given** PastDue subscription, **When** transferring, **Then** 409 error (must resolve payment first)
7. **Given** plan.transferable=false, **When** transferring, **Then** 409 error (plan not transferable)
8. **Given** transfer to self (same customer), **When** transferring, **Then** 409 error
9. **Given** customer 42 has Active subscription, **When** transferring to 42, **Then** 409 error (target already has active sub)
10. **Given** customer 42 has only Canceled subscription, **When** transferring to 42, **Then** success (Canceled is not active)
11. **Given** blank reason, **When** transferring, **Then** 400 error

---

### User Story 5 - Configurable Trial Period (UC-019, Priority: P3)

Plan's trial period is now configurable (0-60 days). Plans with trialDays=0 skip trial and start Active with immediate first charge. FREE tier must have trialDays=0.

**Why this priority**: Breaking change that must be implemented but mainly affects existing Plan and UC-001 logic.

**Independent Test**: Can be tested by creating subscriptions with plans of various trialDays values.

**Acceptance Scenarios**:

1. **Given** plan trialDays=14, **When** creating subscription, **Then** Trial status, trialEnd=now+14d (backwards compatible)
2. **Given** plan trialDays=0 with successful payment, **When** creating subscription, **Then** Active immediately, first invoice generated and paid
3. **Given** plan trialDays=0 with gateway decline, **When** creating subscription, **Then** 502 error, subscription NOT created
4. **Given** plan trialDays=30, **When** creating subscription, **Then** Trial status, trialEnd=now+30d
5. **Given** plan trialDays=0 with coupon "SAVE20", **When** creating subscription, **Then** Active, first invoice has discount applied
6. **Given** plan trialDays=0, per-seat=true, USD(10/seat), seatCount=5, **When** creating, **Then** Active, invoice PLAN_CHARGE=USD(50.00)
7. **Given** FREE tier plan with trialDays=14, **When** validating plan, **Then** error (FREE must have trialDays=0)
8. **Given** plan trialDays=-1, **When** validating, **Then** error (must be 0-60)
9. **Given** plan trialDays=61, **When** validating, **Then** error (must be 0-60)

---

### User Story 6 - Configurable Grace Period (UC-020, Priority: P3)

Plan's grace period for failed payments is now configurable (3-30 days). Higher-tier plans can offer longer grace periods.

**Why this priority**: Breaking change affecting payment recovery logic.

**Independent Test**: Can be tested by triggering payment failure on plans with different gracePeriodDays and verifying the grace period end date.

**Acceptance Scenarios**:

1. **Given** plan gracePeriodDays=7, payment fails on renewal, **When** entering PastDue, **Then** gracePeriodEnd=now+7d (backwards compatible)
2. **Given** plan gracePeriodDays=14, payment fails, **When** entering PastDue, **Then** gracePeriodEnd=now+14d
3. **Given** plan gracePeriodDays=30 (max), payment fails, **When** entering PastDue, **Then** gracePeriodEnd=now+30d
4. **Given** plan gracePeriodDays=3 (min), payment fails, 4 days pass, **When** attempting recovery, **Then** 409 error: grace expired, subscription→Canceled
5. **Given** plan gracePeriodDays=14, PastDue subscription, **When** recovering within 14 days, **Then** success, subscription→Active
6. **Given** plan gracePeriodDays=2 (below min), **When** validating, **Then** error (must be 3-30)
7. **Given** plan gracePeriodDays=31 (above max), **When** validating, **Then** error (must be 3-30)

---

### User Story 7 - Increased Pause Limit and Discount Scope Change (UC-021, Priority: P3)

Two additional breaking changes: pause limit increased from 2 to 3 per billing period, and discounts now exclude usage charges from calculation.

**Why this priority**: Breaking changes that modify existing business rules.

**Independent Test**: Can be tested by verifying 3 pauses are allowed and by checking that discount calculation excludes USAGE_CHARGE.

**Acceptance Scenarios**:

1. **Given** Active subscription with 2 prior pauses in period, **When** pausing (3rd time), **Then** success (was rejected in Phase 0)
2. **Given** Active subscription with 3 prior pauses in period, **When** pausing (4th time), **Then** 409 error (new limit is 3)
3. **Given** Active sub, 0 prior pauses, **When** Pause→Resume→Pause→Resume→Pause→Resume (triple cycle), **Then** all 3 pauses succeed
4. **Given** plan USD(49.99), usage USD(10.00), 20% discount, **When** processing renewal, **Then** discountable=USD(49.99), discount=USD(10.00), total=USD(49.99) (49.99*0.8 + 10.00)
5. **Given** plan USD(49.99), addon USD(9.99), usage USD(5.00), 20% discount, **When** renewal, **Then** discountable=USD(59.98), discount=USD(12.00), total=USD(52.98) (59.98*0.8 + 5.00)
6. **Given** FIXED_AMOUNT discount USD(100), plan USD(49.99), usage USD(10.00), **When** renewal, **Then** discount=min(100, 49.99)=USD(49.99), total=USD(10.00)
7. **Given** free plan USD(0), usage USD(10.00), 20% discount, **When** renewal, **Then** discountable=USD(0), discount=USD(0), total=USD(10.00)

---

### Edge Cases

- What happens when a customer redeems a coupon on a transferred subscription — does the original customer's redemption count?
- How does a no-trial plan (trialDays=0) interact with a coupon that normally applies during the first billing cycle?
- What happens when a plan's gracePeriodDays is changed while a subscription is already in PastDue (use the value at time of failure)?
- How does the discount scope change affect invoices generated before Phase 2 that are still in Open status?
- What happens when transferring a subscription with a coupon discount and the target customer already used a different coupon on a previous (now canceled) subscription?

---

## Clarifications *(resolved)*

### CL-001: Coupon Redemption Tracking

`CouponRedemption` is tracked by **(customerId, couponId)** pair. The one-use-per-customer rule is enforced on the **redeeming customer**, not the subscription. This means:
- If customer A redeems coupon X on subscription S, then S is transferred to customer B, customer A's redemption record remains. Customer B can still redeem coupon X on a different subscription (B has never redeemed X).
- The transferred subscription retains the discount from coupon X, but the `CouponRedemption` record is associated with customer A.

### CL-002: Coupon Code Normalization

- Coupon codes are stored in **uppercase**. Input "welcome-2025" is normalized to "WELCOME-2025" on creation.
- Lookup/redemption is **case-insensitive**: "save20", "Save20", "SAVE20" all match.
- Validation regex: `^[A-Z0-9-]{1,32}$` (applied after uppercase normalization).

### CL-003: Edge Case Resolutions

1. **Coupon on transferred subscription**: The original customer's redemption counts for the original customer only. The new owner (target customer) can still redeem the same coupon on a different subscription. The transferred subscription keeps the existing discount regardless.

2. **No-trial plan (trialDays=0) + coupon**: The coupon's discount applies to the first invoice generated immediately at subscription creation. `remainingCycles` decrements from the first invoice. This is the same behavior as trial-end → first renewal, just happening at creation time.

3. **Grace period change while PastDue**: The grace period is **captured at the time of failure** (snapshot). `gracePeriodEnd` is set when the subscription enters PastDue: `gracePeriodEnd = failureTimestamp + plan.gracePeriodDays`. Subsequent changes to the plan's `gracePeriodDays` do NOT retroactively affect the in-progress grace period.

4. **Discount scope change on pre-Phase 2 invoices**: Phase 2 is a **breaking change** — the new discount scope (excluding USAGE_CHARGE) applies to all invoices generated after Phase 2 deployment. Already-Open invoices from before Phase 2 are NOT retroactively recalculated. They retain their original totals.

5. **Transfer with coupon + target's previous coupon usage**: The target customer's previous coupon usage on a now-canceled subscription counts. If customer B used coupon Y on a canceled subscription, B cannot redeem coupon Y again. But the transferred subscription's existing discount (from coupon X, redeemed by customer A) is preserved — it is not re-validated against customer B.

### CL-004: Plan Validation Rules (Extended for Phase 2)

Plan creation/update must validate:
```
trialDays: 0..60 (inclusive), FREE tier must be 0
gracePeriodDays: 3..30 (inclusive)
transferable: boolean (default true)
```
These fields are immutable after plan creation (same as base price, currency, etc.). Changing them requires creating a new plan version.

### CL-005: Breaking Change — Discount Scope Calculation

The discount calculation is split into discountable and non-discountable subtotals:
```
discountableSubtotal = sum of (PLAN_CHARGE + SEAT_CHARGE + ADDON_CHARGE)
usageSubtotal = sum of (USAGE_CHARGE)
discountAmount = applyDiscount(discountableSubtotal)  // PERCENTAGE or FIXED_AMOUNT
total = (discountableSubtotal - discountAmount) + usageSubtotal + accountCreditApplied
```
For FIXED_AMOUNT discount: `discountAmount = min(fixedAmount, discountableSubtotal)` — discount cannot exceed discountable charges.

### CL-006: Transfer — Idempotency and Audit

Each transfer creates an immutable `SubscriptionTransfer` record with:
- `subscriptionId`, `fromCustomerId`, `toCustomerId`, `reason`, `transferredAt`
- The full transfer history is queryable by subscriptionId.
- Transfers are not idempotent — each API call creates a new transfer (no idempotency key).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-041**: System MUST validate coupon codes as unique, uppercase alphanumeric + hyphens, max 32 characters
- **FR-042**: System MUST enforce one coupon use per customer per lifetime (across all subscriptions)
- **FR-043**: System MUST replace existing discount when redeeming a new coupon
- **FR-044**: System MUST require currency for FIXED_AMOUNT coupons and prohibit currency for PERCENTAGE coupons
- **FR-045**: System MUST validate coupon is active, within validity period, and under max redemption limit before redemption
- **FR-046**: System MUST validate coupon tier and interval applicability against subscription's plan
- **FR-047**: System MUST allow subscription transfer only in Active, Paused, or Trial status
- **FR-048**: System MUST reject transfer of PastDue subscriptions (payment must be resolved first)
- **FR-049**: System MUST verify plan.transferable=true before allowing transfer
- **FR-050**: System MUST verify target customer has no active subscription before transfer
- **FR-051**: System MUST preserve discount, add-ons, credit balance, and seats during transfer
- **FR-052**: System MUST support configurable trial period per plan (0-60 days, was fixed 14)
- **FR-053**: System MUST start subscription Active with immediate charge when plan.trialDays=0
- **FR-054**: System MUST fail subscription creation with 502 when trialDays=0 and payment fails
- **FR-055**: System MUST enforce FREE tier plans have trialDays=0
- **FR-056**: System MUST support configurable grace period per plan (3-30 days, was fixed 7)
- **FR-057**: System MUST allow 3 pauses per billing period (was 2)
- **FR-058**: System MUST exclude USAGE_CHARGE from discount calculation (discount applies only to PLAN_CHARGE, SEAT_CHARGE, ADDON_CHARGE)

### Key Entities

- **Coupon**: Reusable promotional code with discount type (PERCENTAGE/FIXED_AMOUNT), validity period, tier/interval restrictions, and redemption limits. Replaces Phase 0 simple discount codes.
- **CouponRedemption**: Immutable audit record of coupon usage. One per (customer, coupon) pair. Enforces single-use-per-customer rule.
- **SubscriptionTransfer**: Immutable audit record of subscription ownership changes. Tracks from/to customer, reason, and timestamp.
- **Plan (extended)**: Extended with trialDays (0-60), gracePeriodDays (3-30), and transferable (boolean).
- **Subscription (extended)**: Extended with couponId reference for audit/tracking.
- **Invoice (extended)**: Extended with discountableSubtotal and usageSubtotal for the new discount scope rule.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 7 Phase 2 use cases (UC-015 through UC-021) pass their acceptance scenarios
- **SC-002**: All 4 Phase 0 breaking changes correctly modify existing behavior
- **SC-003**: Modified UC-001 handles configurable trial (including trialDays=0 immediate charge)
- **SC-004**: Modified UC-004 enforces new pause limit of 3
- **SC-005**: Modified UC-006 uses plan-specific gracePeriodDays and excludes USAGE_CHARGE from discount
- **SC-006**: Modified UC-007 uses plan-specific gracePeriodDays for recovery window
- **SC-007**: Coupon redemption correctly enforces one-use-per-customer lifetime rule
- **SC-008**: Subscription transfer preserves all state (discount, add-ons, credit, seats)
- **SC-009**: 80%+ test coverage maintained across all modules (Kover)
- **SC-010**: All custom detekt rules continue to pass
