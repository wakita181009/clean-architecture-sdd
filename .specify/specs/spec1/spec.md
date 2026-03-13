# Feature Specification: Subscription Management API — Phase 0 (Core, Part 2)

**Feature Branch**: `spec1`
**Created**: 2026-03-11
**Status**: Clarified
**Input**: Core subscription lifecycle — plan selection, billing, usage, pause/resume, cancellation, payment recovery

Part 2 covers pause/resume, usage metering, renewals, and payment recovery from the original Phase 0 specification.

## User Scenarios & Testing *(mandatory)*

### User Story 4 - Pause and Resume Subscription (UC-004, Priority: P3)

A customer can pause an Active subscription, freezing the billing period. Resuming restores the subscription with the remaining days preserved. A subscription can be paused at most 2 times per billing period, and auto-cancels after 30 consecutive days paused.

**Why this priority**: Pause/resume adds flexibility for customers and reduces churn, but is not part of the core billing flow.

**Independent Test**: Can be tested by pausing an Active subscription and resuming it, verifying the remaining days are preserved.

**Acceptance Scenarios**:

1. **Given** Active subscription with 20 days remaining and 0 prior pauses, **When** pausing, **Then** status=Paused, pausedAt=now, billing period frozen
2. **Given** Paused subscription with 20 frozen days remaining, paused 5 days ago, **When** resuming, **Then** status=Active, periodEnd=now+20d, pausedAt=null
3. **Given** Active subscription with 2 prior pauses in current period, **When** attempting 3rd pause, **Then** 409 error (pause limit reached)
4. **Given** Active subscription with 0 prior pauses, **When** Pause->Resume->Pause->Resume (double), **Then** both pauses succeed (limit=2)
5. **Given** Paused subscription for 30 days, **When** system checks, **Then** subscription auto-cancels to Canceled
6. **Given** Paused subscription for 29 days, **When** system checks, **Then** subscription remains Paused (not yet 30 days)
7. **Given** Active subscription with 2 pauses in previous period now in new period, **When** pausing, **Then** success (count reset for new period)
8. **Given** Trial subscription, **When** attempting to pause, **Then** 409 error (cannot pause during trial)

---

### User Story 5 - Record Usage (UC-005, Priority: P3)

A customer's application records metered usage (e.g., API calls, storage) against an Active subscription. Usage is tracked per billing period with idempotency keys to prevent duplicates.

**Why this priority**: Usage-based billing is a key monetization feature but depends on subscription and plan infrastructure.

**Independent Test**: Can be tested by recording usage against an Active subscription and verifying quantity tracking and idempotency.

**Acceptance Scenarios**:

1. **Given** Active subscription with usage limit=10000, **When** recording metric="api_calls" qty=100 key="req-1", **Then** usage recorded, total period usage=100
2. **Given** Active subscription with limit=1000 and existing usage=900, **When** recording qty=100, **Then** success, total=1000 (exactly at limit)
3. **Given** existing usage record with key="req-1", **When** recording with same key="req-1", **Then** returns existing record (idempotent), no duplicate created
4. **Given** Active subscription with usageLimit=null (unlimited), **When** recording qty=999999, **Then** success (no limit to enforce)
5. **Given** Active subscription with limit=1000 and existing usage=950, **When** recording qty=51, **Then** 409 error (950+51=1001 exceeds limit)
6. **Given** Paused subscription, **When** recording usage, **Then** 409 error (not Active)
7. **Given** metric="" (blank), **When** recording usage, **Then** 400 error

---

### User Story 6 - Process Renewal (UC-006, Priority: P4)

The system processes subscription renewals when a billing period ends. It generates an invoice with plan charges, usage charges, and applicable discounts, then charges via the payment gateway. Failed payments transition the subscription to PastDue with a 7-day grace period.

**Why this priority**: Renewal is a background system process that depends on all other subscription features being in place.

**Independent Test**: Can be tested by triggering renewal for an Active subscription whose period has ended and verifying the invoice and period advancement.

**Acceptance Scenarios**:

1. **Given** Active subscription with periodEnd<=now, plan USD(49.99), no usage, **When** processing renewal, **Then** invoice [PLAN_CHARGE USD(49.99)] is generated, paid, period advanced by 1 month
2. **Given** Active subscription with 500 API calls in period at rate USD(0.01)/call, **When** processing renewal, **Then** invoice includes USAGE_CHARGE USD(5.00) in addition to PLAN_CHARGE
3. **Given** Active subscription with 20% discount (2 remaining cycles), **When** processing renewal, **Then** subtotal=USD(49.99), discount=USD(10.00), total=USD(39.99), remainingCycles decremented to 1
4. **Given** free tier plan with no usage charges, **When** processing renewal, **Then** invoice auto-marked as Paid (zero total, no charge attempt)
5. **Given** Active subscription with pending credit USD(15.00) from downgrade, **When** processing renewal with plan USD(49.99), **Then** credit applied, total=USD(34.99)
6. **Given** Active subscription, payment gateway declines, **When** processing renewal, **Then** invoice status=Open, subscription status=PastDue, gracePeriodEnd=now+7d
7. **Given** usage records at period start/middle/end, **When** calculating usage, **Then** records at start included, records at end excluded (end is exclusive)

---

### User Story 7 - Recover Payment (UC-007, Priority: P4)

When a payment fails, the subscription enters PastDue status with a 7-day grace period. During this period, the system or customer can retry payment. After 3 failed attempts or grace period expiration, the subscription is canceled.

**Why this priority**: Payment recovery is critical for revenue but only applies after renewal failures.

**Independent Test**: Can be tested by retrying payment on a PastDue subscription with an Open invoice and verifying recovery or escalation.

**Acceptance Scenarios**:

1. **Given** Open invoice, PastDue subscription, grace period not expired, attempts=1, **When** retrying payment successfully, **Then** invoice=Paid, subscription=Active, gracePeriod cleared
2. **Given** Open invoice, attempts=2, payment fails, **When** retrying, **Then** attempts=3, invoice=Uncollectible, subscription=Canceled
3. **Given** PastDue subscription, gracePeriodEnd=yesterday, **When** attempting payment, **Then** 409 error, subscription->Canceled, invoice->Uncollectible
4. **Given** gracePeriodEnd=now (exact boundary), **When** attempting payment, **Then** allowed (boundary inclusive)
5. **Given** Paid invoice, **When** attempting recovery, **Then** 409 error (not in Open status)
6. **Given** Active subscription with Open invoice, **When** attempting recovery, **Then** 409 error (subscription must be PastDue)
7. **Given** Open invoice, 1st retry fails, **When** retrying again (2nd failure), **Then** attempts=2, invoice stays Open, subscription stays PastDue

---

### Edge Cases

- What happens when a yearly plan subscription changes to a monthly plan at the end of a leap year billing period?
- How does proration handle a plan change on the last day of a billing period (1 day remaining)?
- What happens when a subscription is paused on the last day of a billing period?
- How does the system handle concurrent renewal and cancellation requests?
- What happens when a discount expires during the same renewal cycle it was supposed to apply?
- How does usage recording handle the exact boundary between two billing periods?
