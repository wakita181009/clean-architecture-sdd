# Feature Specification: Subscription Management API — Phase 0 (Core, Part 3)

**Feature Branch**: `spec2`
**Created**: 2026-03-11
**Status**: Clarified
**Input**: Core subscription lifecycle — plan selection, billing, usage, pause/resume, cancellation, payment recovery

Part 3 captures shared clarifications, business rules, entities, and success criteria from the original Phase 0 specification.

## Clarifications *(resolved)*

### CL-001: Subscription State Machine Transitions

```
Trial     -> Active (trial ends + payment success)
Trial     -> Canceled (customer cancels)
Active    -> Paused (customer pauses)
Active    -> PastDue (renewal payment fails)
Active    -> Canceled (customer cancels immediately, or period-end cancel triggers)
Paused    -> Active (customer resumes)
Paused    -> Canceled (customer cancels, or 30-day auto-cancel)
PastDue   -> Active (payment recovered)
PastDue   -> Canceled (3 failed attempts, or grace period expired)
Canceled  -> (terminal state)
```

All other transitions are invalid and MUST return an error.

### CL-002: Invoice State Machine Transitions

```
Draft     -> Open (finalized for payment)
Open      -> Paid (payment succeeds, or zero-total auto-pay)
Open      -> Void (subscription canceled immediately)
Open      -> Uncollectible (3 failed payment attempts)
Paid      -> (terminal - only credit notes can issue refunds)
Void      -> (terminal)
Uncollectible -> (terminal)
```

### CL-003: Trial -> Active Transition

The trial-to-active transition is triggered by the **renewal process** (UC-006). When the system detects `periodEnd <= now` for a Trial subscription, it attempts to charge the first invoice. On payment success, status transitions to Active. On payment failure, status transitions to PastDue (not Canceled - the grace period applies).

### CL-004: Payment Gateway Interface

The payment gateway is modeled as a **port interface** in `application/command/port/`:
```
PaymentGatewayPort.charge(amount: Money, customerId: CustomerId): Either<PaymentError, TransactionId>
```
Infrastructure implements this port. For Phase 0, the implementation can be a stub/mock. The port returns `Either<PaymentError, TransactionId>` - no exceptions.

### CL-005: Billing Period Calculation

- **MONTHLY**: `periodEnd = periodStart.plusMonths(1)`. Uses `java.time` month addition (e.g., Jan 31 -> Feb 28/29).
- **YEARLY**: `periodEnd = periodStart.plusYears(1)`.
- Proration uses **actual days in the period**: `daysInPeriod = ChronoUnit.DAYS.between(periodStart, periodEnd)`.
- This means February periods have 28 or 29 days, and proration fractions adjust accordingly.

### CL-006: Edge Case Resolutions

1. **Yearly->Monthly at leap year boundary**: Proration uses actual days in the yearly period (365 or 366). The new monthly period starts from the change date. No special leap year handling needed - `java.time` handles it naturally.

2. **Proration on last day (1 day remaining)**: Proration applies normally. Credit = `oldPrice * 1 / daysInPeriod`. Charge = `newPrice * 1 / daysInPeriod`. Both are small but valid amounts.

3. **Pause on last day of billing period**: Allowed. When resumed, `periodEnd = now + 1 day`. If renewal is due (`periodEnd <= now`), the system processes renewal first, then the pause applies to the new period.

4. **Concurrent renewal and cancellation**: The system uses optimistic locking on the subscription entity. If both operations attempt simultaneously, one succeeds and the other receives a conflict error. The cancel-at-period-end flag (`cancelAtPeriodEnd=true`) is checked during renewal - if set, renewal transitions to Canceled instead of renewing.

5. **Discount expires during renewal cycle**: The discount is applied if `remainingCycles > 0` at the time of invoice generation. After applying, `remainingCycles` is decremented. If it reaches 0, the discount is cleared. A discount with `remainingCycles=1` is applied one final time and then removed.

6. **Usage at period boundary**: `periodStart` is **inclusive**, `periodEnd` is **exclusive**. Usage recorded at exactly `periodEnd` belongs to the **next** period. Usage at exactly `periodStart` belongs to the **current** period.

### CL-007: Discount Code Validation at Subscription Creation

If an invalid or non-existent discount code is provided during subscription creation (UC-001), the system returns a 404 error. Discount codes are validated before subscription creation - the subscription is not created if the discount is invalid.

### CL-008: One Active Subscription Rule - Status Scope

"Active subscription" for the uniqueness check means status in `{Trial, Active, Paused, PastDue}`. A customer with a `Canceled` subscription can create a new one. This is explicitly stated in FR-001.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST enforce that a customer has at most ONE active subscription (status in Trial, Active, Paused, PastDue) at any time
- **FR-002**: System MUST provide a 14-day trial period from subscription creation with no charge
- **FR-003**: System MUST enforce a 7-day grace period for failed payments before canceling
- **FR-004**: System MUST allow at most 2 pauses per subscription per billing period
- **FR-005**: System MUST auto-cancel subscriptions after 30 consecutive days paused
- **FR-006**: System MUST calculate proration to the day using HALF_UP rounding at currency scale
- **FR-007**: System MUST charge immediately for upgrades (higher tier) and credit for downgrades (lower tier)
- **FR-008**: System MUST prevent currency changes during plan changes
- **FR-009**: System MUST enforce plan usage limits per billing period
- **FR-010**: System MUST only allow usage recording for Active subscriptions
- **FR-011**: System MUST auto-mark invoices with zero total as Paid
- **FR-012**: System MUST mark invoices as Uncollectible after 3 failed payment attempts
- **FR-013**: System MUST NOT apply discounts to proration invoices
- **FR-014**: System MUST enforce at most one active discount per subscription
- **FR-015**: System MUST validate PERCENTAGE discount values between 1 and 100 inclusive
- **FR-016**: System MUST enforce that FREE tier plans have base price of zero
- **FR-017**: System MUST enforce idempotency via idempotency keys for usage recording
- **FR-018**: System MUST follow subscription state machine transitions strictly (§3.1)
- **FR-019**: System MUST follow invoice state machine transitions strictly (§3.2)
- **FR-020**: System MUST support YEARLY billing interval with 20% discount over monthly equivalent

### Key Entities

- **Plan**: Billing plan with tier (FREE/STARTER/PROFESSIONAL/ENTERPRISE), billing interval (MONTHLY/YEARLY), base price, usage limit, and features. Immutable during subscription.
- **Subscription**: Central entity representing customer-plan relationship. Tracks status (Trial/Active/Paused/PastDue/Canceled/Expired), billing period, trial/pause/cancel timestamps.
- **Money**: Value object with amount (BigDecimal) and currency (USD/EUR/JPY). JPY uses scale=0, others scale=2. Supports arithmetic with currency validation.
- **Invoice**: Billing document with line items, subtotal, discount, total. Status transitions: Draft->Open->Paid/Void/Uncollectible.
- **Invoice Line Item**: Single charge/credit on invoice. Types: PLAN_CHARGE, USAGE_CHARGE, PRORATION_CREDIT, PRORATION_CHARGE.
- **Usage Record**: Metered usage tracking with metric name, quantity, and idempotency key. Scoped to billing period.
- **Discount**: Subscription-level discount (PERCENTAGE or FIXED_AMOUNT) with duration in billing cycles.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 7 Phase 0 use cases (UC-001 through UC-007) pass their acceptance scenarios
- **SC-002**: Subscription state machine enforces all valid transitions and rejects all invalid transitions
- **SC-003**: Invoice state machine enforces all valid transitions and rejects all invalid transitions
- **SC-004**: Proration calculations produce exact results matching HALF_UP rounding for all currency types (USD, EUR, JPY)
- **SC-005**: 80%+ test coverage across domain, application, and presentation modules (Kover)
- **SC-006**: All custom detekt rules pass (ForbiddenLayerImport, NoThrowOutsidePresentation, NoExplicitAny)
- **SC-007**: No `throw` in domain, application, or infrastructure layers - all errors expressed as Either
