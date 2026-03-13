# Feature Specification: Subscription Management API — Phase 1 (Add-ons, Seats, Credit Notes)

**Feature Branch**: `spec3`
**Created**: 2026-03-11
**Status**: Clarified
**Input**: Extends Phase 0 with add-on management, seat-based billing, and credit note/refund functionality

Phase 1 extends the Phase 0 subscription system with three feature areas: **Add-on Management**, **Seat Management**, and **Credit Notes (Refunds)**. All Phase 0 domain models, state machines, and business rules remain in effect.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Attach Add-on (UC-008, Priority: P1)

A customer with an Active subscription attaches an optional add-on (e.g., Priority Support, Extra Storage). The system validates tier compatibility and currency match, calculates prorated charge for the remaining billing period, and charges immediately.

**Why this priority**: Add-ons are the primary revenue expansion feature in Phase 1. They demonstrate mid-cycle billing and tier validation.

**Independent Test**: Can be tested by attaching a FLAT add-on to an Active subscription and verifying proration charge and SubscriptionAddOn creation.

**Acceptance Scenarios**:

1. **Given** Active subscription (PROFESSIONAL tier), compatible FLAT add-on USD(9.99), 15 of 30 days remaining, **When** attaching the add-on, **Then** SubscriptionAddOn created with qty=1, prorated charge=USD(5.00) charged immediately
2. **Given** Active subscription (PROFESSIONAL, 5 seats, per-seat plan), PER_SEAT add-on USD(2.00), 15/30 remaining, **When** attaching, **Then** qty=5, prorated charge=USD(5.00) (2.00*5*15/30)
3. **Given** Active subscription with 4 existing add-ons, **When** attaching a 5th add-on, **Then** success (at maximum limit)
4. **Given** Active subscription with 5 existing add-ons, **When** attaching another add-on, **Then** 409 error (limit reached)
5. **Given** USD subscription, **When** attaching EUR add-on, **Then** 409 error (currency mismatch)
6. **Given** STARTER subscription, add-on compatible with {PROFESSIONAL, ENTERPRISE} only, **When** attaching, **Then** 409 error (tier incompatibility)
7. **Given** non-per-seat plan, **When** attaching PER_SEAT add-on, **Then** 409 error
8. **Given** add-on already attached (Active status), **When** attaching same add-on, **Then** 409 error (duplicate)
9. **Given** payment gateway failure during proration charge, **When** attaching, **Then** 502 error, add-on NOT attached

---

### User Story 2 - Detach Add-on (UC-009, Priority: P1)

A customer can detach an add-on from an Active or Paused subscription. The system calculates prorated credit for unused days and adds it to the subscription's account credit balance.

**Why this priority**: Detach completes the add-on lifecycle and is required alongside attach for a complete feature.

**Independent Test**: Can be tested by detaching a FLAT add-on mid-cycle and verifying the credit calculation and account balance update.

**Acceptance Scenarios**:

1. **Given** Active subscription, FLAT add-on USD(9.99), 15 of 30 days remaining, **When** detaching, **Then** credit=USD(5.00) added to account balance, SubscriptionAddOn status=Detached
2. **Given** Active subscription, PER_SEAT add-on USD(2.00) qty=5, 15/30 remaining, **When** detaching, **Then** credit=USD(5.00) (2.00*5*15/30) added to balance
3. **Given** Paused subscription with 20 frozen days remaining (30 day period), **When** detaching add-on, **Then** credit=price*20/30 added to balance
4. **Given** add-on on last day of period (1/30 remaining), **When** detaching, **Then** minimal credit (1/30 of price)
5. **Given** Trial subscription, **When** attempting to detach, **Then** 409 error (not Active or Paused)
6. **Given** already detached add-on, **When** attempting to detach, **Then** 404 error

---

### User Story 3 - Update Seat Count (UC-010, Priority: P2)

A customer with a per-seat plan can increase or decrease the number of seats. Seat increase charges immediately (like upgrade), seat decrease credits to account balance (like downgrade). PER_SEAT add-ons auto-update their quantity.

**Why this priority**: Seat management is critical for team-based SaaS products but depends on add-on infrastructure.

**Independent Test**: Can be tested by increasing seats on a per-seat subscription and verifying proration charge and PER_SEAT add-on quantity update.

**Acceptance Scenarios**:

1. **Given** Active per-seat subscription USD(10.00), 5 seats, 15/30 remaining, **When** increasing to 6 seats, **Then** charge=10.00*1*15/30=USD(5.00), seats updated to 6
2. **Given** Active per-seat subscription, 5 seats, 15/30 remaining, **When** decreasing to 4 seats, **Then** credit=10.00*1*15/30=USD(5.00) added to account balance
3. **Given** Active per-seat sub with PER_SEAT add-on USD(2.00) qty=5, 15/30 remaining, **When** increasing to 7 seats, **Then** seat proration + add-on proration for 2 extra seats charged
4. **Given** Active per-seat sub with PER_SEAT add-on, 5 seats, **When** decreasing to 3 seats, **Then** add-on qty updated to 3, seat credit + add-on credit applied
5. **Given** seat increase with FLAT add-on, **When** updating seats, **Then** FLAT add-on qty stays 1, no add-on proration
6. **Given** minSeats=2, **When** setting seatCount=1, **Then** 409 error (below minimum)
7. **Given** maxSeats=10, **When** setting seatCount=11, **Then** 409 error (above maximum)
8. **Given** non-per-seat plan, **When** updating seat count, **Then** 409 error
9. **Given** same seat count as current, **When** updating, **Then** 409 error
10. **Given** payment gateway failure on seat increase, **When** charging, **Then** 502 error, seats NOT changed

---

### User Story 4 - Issue Credit Note (UC-011, Priority: P2)

An admin issues a full or partial refund against a paid invoice. Refunds can be processed through the payment gateway (REFUND_TO_PAYMENT) or added as account credit for future invoices (ACCOUNT_CREDIT).

**Why this priority**: Credit notes are essential for customer support and compliance but depend on the invoice system.

**Independent Test**: Can be tested by issuing a full refund on a paid invoice and verifying the credit note creation and refund processing.

**Acceptance Scenarios**:

1. **Given** Paid invoice USD(49.99) with no existing credits, **When** issuing type=FULL app=REFUND_TO_PAYMENT, **Then** CreditNote amount=USD(49.99), status=Applied, refundTransactionId set
2. **Given** Paid invoice USD(49.99), **When** issuing type=FULL app=ACCOUNT_CREDIT, **Then** CreditNote Applied, account balance += USD(49.99)
3. **Given** Paid invoice USD(49.99), **When** issuing type=PARTIAL amount=USD(20.00) app=REFUND_TO_PAYMENT, **Then** CreditNote USD(20.00) Applied
4. **Given** Paid invoice USD(49.99) with existing credit USD(20.00), **When** issuing type=FULL, **Then** amount=USD(29.99) (remaining refundable)
5. **Given** Paid invoice USD(49.99) with existing credit USD(49.99), **When** issuing type=FULL, **Then** 409 error (already fully refunded)
6. **Given** Paid invoice USD(49.99) with existing credit USD(40.00), **When** issuing type=PARTIAL amount=USD(10.00), **Then** 409 error (exceeds remaining USD(9.99))
7. **Given** Open invoice (not Paid), **When** issuing credit note, **Then** 409 error (invoice must be Paid)
8. **Given** payment gateway refund failure, **When** issuing REFUND_TO_PAYMENT, **Then** 502 error, credit note stays Issued (can retry)

---

### User Story 5 - Phase 0 Extensions: Seats in Subscription Creation (UC-012, Priority: P3)

Phase 0 subscription creation (UC-001) is extended to support per-seat plans. When subscribing to a per-seat plan, the customer must provide a seat count within the plan's allowed range.

**Why this priority**: Integrates seat management into the existing subscription creation flow.

**Independent Test**: Can be tested by creating a subscription with a per-seat plan and verifying seat count validation and storage.

**Acceptance Scenarios**:

1. **Given** per-seat plan (minSeats=1, maxSeats=50), **When** creating subscription with seatCount=5, **Then** subscription created with seatCount=5
2. **Given** per-seat plan (minSeats=3), **When** creating with seatCount=2, **Then** 400 error (below minimum)
3. **Given** per-seat plan (maxSeats=10), **When** creating with seatCount=11, **Then** 400 error (above maximum)
4. **Given** per-seat plan, **When** creating without seatCount, **Then** 400 error (required for per-seat plan)
5. **Given** non-per-seat plan, **When** creating with seatCount=5, **Then** success, seatCount set to null (ignored)

---

### User Story 6 - Phase 0 Extensions: Plan Change with Add-ons (UC-013, Priority: P3)

Phase 0 plan change (UC-002) is extended to handle add-on compatibility. Incompatible add-ons are auto-detached with prorated credit when the plan tier changes.

**Why this priority**: Ensures plan changes correctly handle the add-on system.

**Independent Test**: Can be tested by changing plans and verifying that incompatible add-ons are detached with correct proration credits.

**Acceptance Scenarios**:

1. **Given** PROFESSIONAL subscription with add-on compatible with both PROFESSIONAL and ENTERPRISE, **When** changing to ENTERPRISE plan, **Then** add-on remains Active
2. **Given** PROFESSIONAL subscription with add-on only for {PROFESSIONAL, ENTERPRISE}, **When** changing to STARTER, **Then** add-on detached with proration credit
3. **Given** per-seat plan with PER_SEAT add-on, **When** changing to non-per-seat plan, **Then** PER_SEAT add-on detached, seat count set to null
4. **Given** non-per-seat plan, **When** changing to per-seat plan (minSeats=3), **Then** seatCount initialized to 3
5. **Given** plan change with incompatible add-on, 15/30 remaining, **When** changing, **Then** proration invoice includes PRORATION_CREDIT + PRORATION_CHARGE + ADDON_PRORATION_CREDIT

---

### User Story 7 - Phase 0 Extensions: Renewal with Add-ons and Credit (UC-014, Priority: P3)

Phase 0 renewal (UC-006) is extended to include add-on charges, per-seat billing, and account credit application in the renewal invoice.

**Why this priority**: Integrates Phase 1 features into the billing cycle.

**Independent Test**: Can be tested by triggering renewal for a subscription with add-ons and account credit balance and verifying the invoice line items.

**Acceptance Scenarios**:

1. **Given** Active subscription with 1 FLAT add-on USD(9.99), **When** processing renewal, **Then** invoice includes ADDON_CHARGE USD(9.99)
2. **Given** Active subscription with 5 seats, PER_SEAT add-on USD(2.00), **When** processing renewal, **Then** ADDON_CHARGE=USD(10.00) (2.00*5)
3. **Given** per-seat plan USD(10.00) with 5 seats, **When** processing renewal, **Then** PLAN_CHARGE=USD(50.00) (10.00*5)
4. **Given** account credit balance USD(15.00), renewal total USD(49.99), **When** processing renewal, **Then** ACCOUNT_CREDIT line item USD(-15.00), gateway charged USD(34.99), balance=USD(0.00)
5. **Given** account credit balance USD(60.00), renewal total USD(49.99), **When** processing renewal, **Then** ACCOUNT_CREDIT=USD(-49.99), gateway not charged, balance=USD(10.01)
6. **Given** account balance USD(10.00), subtotal USD(49.99), 20% discount, **When** processing renewal, **Then** after discount USD(39.99), credit applied USD(10.00), charge USD(29.99)

---

### Edge Cases

- What happens when all 5 add-ons are detached and re-attached within the same billing period?
- How does per-seat proration interact with add-on proration when both happen on the same seat change?
- What happens when account credit exactly equals the invoice total after discount?
- How does a full refund interact with a subscription that already has account credit from a previous downgrade?
- What happens when a PER_SEAT add-on is detached while the subscription is paused and the frozen days differ from actual days?

---

## Clarifications *(resolved)*

### CL-001: SubscriptionAddOn State Machine

```
Active    → Detached (customer detaches, or plan change incompatibility)
Detached  → (terminal)
```

Only Active add-ons count toward the 5 add-on limit. A Detached add-on can be re-attached (creates a new SubscriptionAddOn record, not reactivating the old one).

### CL-002: CreditNote State Machine

```
Issued    → Applied (refund processed successfully or account credit applied)
Issued    → Voided (admin voids before processing)
Applied   → (terminal)
Voided    → (terminal)
```

When `REFUND_TO_PAYMENT` fails at the gateway, the credit note stays in `Issued` status and can be retried.

### CL-003: Edge Case Resolutions

1. **All 5 add-ons detached and re-attached in same period**: Allowed. Detached add-ons don't count toward the limit. Each attach creates a new SubscriptionAddOn. Each detach/attach calculates its own proration independently.

2. **Per-seat + add-on proration on same seat change**: Both prorations are calculated separately and included as separate line items on the same invoice. Seat proration uses `perSeatPrice * seatDelta * remainingDays / totalDays`. PER_SEAT add-on proration uses `addOnPrice * seatDelta * remainingDays / totalDays`. They are additive.

3. **Account credit exactly equals invoice total after discount**: The entire invoice is paid from account credit. Gateway is not charged (`charge amount = 0` → skip gateway call). Account balance becomes `USD(0.00)`. Invoice marked as Paid.

4. **Full refund + existing account credit from downgrade**: Account credit from downgrades and refunds are independent balances tracked in the same `accountCreditBalance` field. A full refund via `ACCOUNT_CREDIT` adds to the existing balance. A full refund via `REFUND_TO_PAYMENT` goes through the gateway and does not affect account credit balance.

5. **PER_SEAT add-on detach while paused**: Proration uses **frozen remaining days** (not actual calendar days). If the subscription was paused with 20 frozen days remaining in a 30-day period, detach credit = `addOnPrice * qty * 20 / 30`. This matches US3 scenario 3.

### CL-004: Add-on Proration Calculation

Add-on proration follows the same formula as plan proration:
```
proratedAmount = addOnPrice * quantity * remainingDays / totalDaysInPeriod
```
- FLAT add-ons: `quantity = 1` (always)
- PER_SEAT add-ons: `quantity = subscription.seatCount`
- Rounding: HALF_UP at currency scale (JPY=0, USD/EUR=2)

### CL-005: Account Credit Balance — Never Negative

`accountCreditBalance` MUST never go below zero. When applying credit to an invoice:
```
creditApplied = min(accountCreditBalance, postDiscountTotal)
gatewayCharge = postDiscountTotal - creditApplied
newBalance = accountCreditBalance - creditApplied
```

### CL-006: Seat Count on Non-Per-Seat Plans

When a subscription is on a non-per-seat plan, `seatCount` is `null`. If a customer provides `seatCount` during creation for a non-per-seat plan, it is silently ignored (set to null). This is explicitly stated in US5 scenario 5.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-021**: System MUST allow at most 5 active add-ons per subscription
- **FR-022**: System MUST prevent duplicate add-on attachment (same add-on cannot be attached twice)
- **FR-023**: System MUST validate add-on currency matches subscription's plan currency
- **FR-024**: System MUST validate add-on tier compatibility with subscription's current plan tier
- **FR-025**: System MUST restrict PER_SEAT add-ons to per-seat plan subscriptions only
- **FR-026**: System MUST calculate prorated charge for mid-cycle add-on attachment (HALF_UP rounding)
- **FR-027**: System MUST calculate prorated credit for mid-cycle add-on detachment
- **FR-028**: System MUST auto-detach incompatible add-ons with prorated credit on plan changes
- **FR-029**: System MUST prevent FREE tier plans from having per-seat pricing
- **FR-030**: System MUST enforce minimum seats >= 1 for per-seat plans
- **FR-031**: System MUST enforce seat count within plan's [minSeats, maxSeats] range
- **FR-032**: System MUST charge immediately for seat increases and credit for seat decreases (like upgrade/downgrade)
- **FR-033**: System MUST auto-update PER_SEAT add-on quantities on seat count changes with proration
- **FR-034**: System MUST only allow credit notes for Paid invoices
- **FR-035**: System MUST enforce total credit notes for an invoice cannot exceed invoice total
- **FR-036**: System MUST process REFUND_TO_PAYMENT through payment gateway
- **FR-037**: System MUST add ACCOUNT_CREDIT to subscription's credit balance, applied on next renewal
- **FR-038**: System MUST apply account credit after discount but before payment gateway during renewal
- **FR-039**: System MUST ensure account credit applied to an invoice does not exceed the post-discount total
- **FR-040**: System MUST set seat count to null and detach PER_SEAT add-ons when changing from per-seat to non-per-seat plan

### Key Entities

- **AddOn**: Optional feature attached to subscriptions. Has billing type (FLAT/PER_SEAT), compatible tiers, and price. PER_SEAT price is multiplied by seat count.
- **SubscriptionAddOn**: Link entity between Subscription and AddOn. Tracks quantity, status (Active/Detached), and timestamps. Max 5 per subscription.
- **CreditNote**: Refund document against a paid invoice. Types: FULL/PARTIAL. Applications: REFUND_TO_PAYMENT/ACCOUNT_CREDIT. Status: Issued→Applied/Voided.
- **Plan (extended)**: Extended with per-seat pricing fields: perSeatPricing (boolean), minSeats, maxSeats.
- **Subscription (extended)**: Extended with seatCount (for per-seat plans) and accountCreditBalance (from credit notes and downgrades).
- **Invoice Line Item (extended)**: New types: ADDON_CHARGE, ADDON_PRORATION_CREDIT/CHARGE, SEAT_CHARGE, SEAT_PRORATION_CREDIT/CHARGE, ACCOUNT_CREDIT.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 7 Phase 1 use cases (UC-008 through UC-014) pass their acceptance scenarios
- **SC-002**: Phase 0 extension use cases (UC-012, UC-013, UC-014) correctly integrate seat and add-on logic
- **SC-003**: SubscriptionAddOn and CreditNote state machines enforce all valid/invalid transitions
- **SC-004**: Proration calculations for add-ons and seats produce exact HALF_UP-rounded results for USD, EUR, and JPY
- **SC-005**: Account credit balance correctly applied during renewal with proper ordering (after discount, before gateway)
- **SC-006**: 80%+ test coverage maintained across all modules (Kover)
- **SC-007**: All custom detekt rules continue to pass
