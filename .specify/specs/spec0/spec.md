# Feature Specification: Subscription Management API — Phase 0 (Core, Part 1)

**Feature Branch**: `spec0`
**Created**: 2026-03-11
**Status**: Clarified
**Input**: Core subscription lifecycle — plan selection, billing, usage, pause/resume, cancellation, payment recovery

Part 1 covers subscription creation, plan changes, and cancellation flows from the original Phase 0 specification.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create Subscription (UC-001, Priority: P1)

A customer selects a billing plan and creates a new subscription. The subscription begins with a 14-day trial period. No charge is made during the trial. An optional discount code can be applied at creation time.

**Why this priority**: This is the entry point for the entire subscription system. Without subscription creation, no other features can function.

**Independent Test**: Can be fully tested by creating a subscription with a valid plan and verifying it starts in Trial status with correct period dates.

**Acceptance Scenarios**:

1. **Given** an active plan "Pro" (MONTHLY, USD 49.99), **When** customer 1 creates a subscription with planId=pro, **Then** subscription is created with status=Trial, trialEnd=now+14d, no invoice generated
2. **Given** an active plan and discount code "WELCOME20" (20%, 3 months), **When** customer creates a subscription with discountCode="WELCOME20", **Then** subscription is created with discount attached, remainingCycles=3
3. **Given** a FREE tier plan (USD 0.00), **When** customer creates a subscription, **Then** subscription is created with status=Trial, no payment method required
4. **Given** customer 1 already has an Active subscription, **When** customer 1 tries to create another subscription, **Then** 409 error is returned (already subscribed)
5. **Given** customer 1 has a Canceled subscription, **When** customer 1 creates a new subscription, **Then** subscription is created successfully (Canceled is not active)
6. **Given** no plan with ID 999 exists, **When** customer creates a subscription with planId=999, **Then** 404 error is returned
7. **Given** customerId=0, **When** creating a subscription, **Then** 400 error is returned (invalid ID)

---

### User Story 2 - Change Plan (UC-002, Priority: P2)

A customer with an Active subscription can switch to a different billing plan. The system calculates proration for the remaining days in the current period - upgrade charges immediately, downgrade credits to the next invoice.

**Why this priority**: Plan changes are essential for customer retention and revenue growth. Proration ensures fair billing.

**Independent Test**: Can be tested by changing an Active subscription from Starter to Professional and verifying proration invoice is generated with correct amounts.

**Acceptance Scenarios**:

1. **Given** Active subscription on Starter USD(19.99) with 15 of 30 days remaining, **When** changing to Professional USD(49.99), **Then** proration invoice is generated: credit=USD(10.00), charge=USD(25.00), net=USD(15.00) charged immediately
2. **Given** Active subscription on Professional USD(49.99) with 15 days remaining, **When** downgrading to Starter USD(19.99), **Then** net proration is negative, credit stored for next invoice
3. **Given** USD(49.99) plan with 17 of 30 days remaining, **When** calculating proration credit, **Then** credit=USD(28.33) (HALF_UP rounding: 49.99*17/30=28.3277->28.33)
4. **Given** Active USD subscription, **When** changing to a EUR plan, **Then** 409 error is returned (currency mismatch)
5. **Given** Paused subscription, **When** attempting plan change, **Then** 409 error (must be Active)
6. **Given** Active subscription on Professional plan, **When** changing to Professional (same plan), **Then** 409 error (same plan)
7. **Given** upgrade with payment gateway failure, **When** charging proration, **Then** 502 error, plan NOT changed

---

### User Story 3 - Cancel Subscription (UC-003, Priority: P2)

A customer can cancel their subscription either immediately or at the end of the current billing period. Immediate cancellation voids open invoices. Trial subscriptions can only be canceled immediately (no charge).

**Why this priority**: Cancellation is a required lifecycle operation for any subscription system.

**Independent Test**: Can be tested by canceling an Active subscription and verifying the correct status transition and invoice handling.

**Acceptance Scenarios**:

1. **Given** Active subscription, **When** canceling with immediate=false, **Then** cancelAtPeriodEnd=true, canceledAt=now, status stays Active until period end
2. **Given** Active subscription with 1 open invoice, **When** canceling with immediate=true, **Then** status=Canceled, invoice voided
3. **Given** Paused subscription, **When** canceling with immediate=true, **Then** status=Canceled
4. **Given** Trial subscription, **When** canceling with immediate=true, **Then** status=Canceled, no charge
5. **Given** Canceled subscription, **When** attempting to cancel, **Then** 409 error (already canceled)
6. **Given** Paused subscription, **When** canceling with immediate=false, **Then** 409 error (paused subs can only be canceled immediately)
7. **Given** Active subscription with cancelAtPeriodEnd=true, **When** period ends, **Then** subscription transitions to Canceled (not renewed)
