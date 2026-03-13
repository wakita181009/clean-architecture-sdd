# Feature Specification: Subscription Management API — Phase 3 (Webhooks, Usage Alerts, Scheduled Changes)

**Feature Branch**: `spec5`
**Created**: 2026-03-11
**Status**: Clarified
**Input**: Extends subscription system with webhook event notifications, usage threshold alerts, and scheduled plan changes

Phase 3 extends the subscription system with three feature areas: **Webhook Event Notifications**, **Usage Alerts**, and **Scheduled Plan Changes**. All Phase 0/1/2 domain models, state machines, and business rules remain in effect.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register Webhook Endpoint (UC-022, Priority: P1)

A customer registers an HTTPS endpoint to receive event notifications about subscription lifecycle changes. The system validates the URL, generates a signing secret if not provided, and stores the endpoint with subscribed event types.

**Why this priority**: Webhook registration is the foundation for the entire event notification system. Must exist before events can be delivered.

**Independent Test**: Can be tested by registering an endpoint with valid URL and events, verifying creation and secret generation.

**Acceptance Scenarios**:

1. **Given** valid inputs, **When** registering url="https://example.com/hook" events={"subscription.activated"}, **Then** endpoint created with active=true, secret auto-generated (64 hex chars)
2. **Given** customer provides secret (64 chars), **When** registering, **Then** endpoint created with provided secret
3. **Given** events={"subscription.activated","invoice.paid","subscription.canceled"}, **When** registering, **Then** success, all 3 events subscribed
4. **Given** customer has 9 active endpoints, **When** registering 10th, **Then** success (at limit)
5. **Given** customer has 10 active endpoints, **When** registering another, **Then** 409 error (limit reached)
6. **Given** url="http://example.com/hook" (HTTP, not HTTPS), **When** registering, **Then** 400 error
7. **Given** events={"invalid.event"}, **When** registering, **Then** 400 error (unknown event type)
8. **Given** events={} (empty), **When** registering, **Then** 400 error (must subscribe to at least one event)
9. **Given** secret="short" (< 32 chars), **When** registering, **Then** 400 error

---

### User Story 2 - Update and Delete Webhook Endpoint (UC-023, Priority: P1)

A customer can update their webhook endpoint's URL, events, active status, and description. Deleting (soft-delete) an endpoint sets it inactive and abandons all pending/failed deliveries.

**Why this priority**: Completes the webhook endpoint lifecycle management, required for operational use.

**Independent Test**: Can be tested by updating an endpoint's URL and events, and by deleting an endpoint and verifying pending deliveries are abandoned.

**Acceptance Scenarios**:

1. **Given** existing endpoint, **When** updating url="https://new.example.com/hook", **Then** URL updated
2. **Given** existing endpoint, **When** updating events={"invoice.paid"}, **Then** events replaced
3. **Given** active endpoint, **When** setting active=false, **Then** endpoint deactivated
4. **Given** inactive endpoint, **When** setting active=true, **Then** endpoint reactivated
5. **Given** endpoint owned by customer 1, **When** customer 2 updates, **Then** 403 error (forbidden)
6. **Given** endpoint with Pending deliveries, **When** deleting endpoint, **Then** active=false, Pending deliveries→Abandoned
7. **Given** endpoint with Failed (pending retry) deliveries, **When** deleting, **Then** Failed deliveries→Abandoned
8. **Given** endpoint with Delivered deliveries, **When** deleting, **Then** Delivered stays Delivered (terminal state unaffected)
9. **Given** non-existent endpointId=999, **When** updating, **Then** 404 error

---

### User Story 3 - Webhook Event Delivery (UC-024, Priority: P1)

When domain events occur (subscription created, invoice paid, etc.), the system delivers webhook payloads to all active endpoints subscribed to that event type. Payloads are signed with HMAC-SHA256. Failed deliveries retry with exponential backoff (1m, 5m, 30m, 2h, 24h), max 5 attempts. HTTP 4xx = permanent failure (no retry).

**Why this priority**: Delivery is the core value of the webhook system — without it, registration is meaningless.

**Independent Test**: Can be tested by triggering a subscription activation and verifying the webhook is delivered with correct signature and payload format.

**Acceptance Scenarios**:

1. **Given** endpoint subscribed to "subscription.activated", subscription activates, **When** delivering, **Then** delivery status=Delivered, httpStatusCode=200, X-Webhook-Signature matches HMAC-SHA256
2. **Given** endpoint subscribed to {"subscription.activated"}, invoice.paid event occurs, **When** checking deliveries, **Then** no delivery created (not subscribed to that event)
3. **Given** 2 endpoints subscribed to "subscription.activated", **When** event occurs, **Then** both endpoints receive the event
4. **Given** endpoint returns HTTP 500, **When** delivering, **Then** status=Failed, nextRetryAt=now+1m, attemptCount=1
5. **Given** endpoint times out (>30s), **When** delivering, **Then** status=Failed, retry scheduled
6. **Given** Failed delivery, retry succeeds with HTTP 200, **When** retrying, **Then** status=Delivered
7. **Given** 4 failures already, 5th attempt fails, **When** retrying, **Then** status=Abandoned (max attempts reached)
8. **Given** endpoint returns HTTP 400 (client error), **When** delivering, **Then** status=Abandoned immediately (no retry)
9. **Given** endpoint returns HTTP 422 (client error), **When** delivering, **Then** status=Abandoned immediately
10. **Given** inactive endpoint (active=false), event occurs, **When** checking, **Then** no delivery created (skipped)
11. **Given** exponential backoff: attempt 1 fails, **When** scheduling retry, **Then** nextRetryAt=lastAttempt+1m
12. **Given** exponential backoff: attempt 2 fails, **When** scheduling retry, **Then** nextRetryAt=lastAttempt+5m
13. **Given** Active subscription with queued Pending deliveries, subscription canceled, **When** checking deliveries, **Then** deliveries remain Pending (not abandoned — only endpoint delete abandons)

---

### User Story 4 - Create Usage Alert (UC-025, Priority: P2)

A customer configures threshold-based alerts on subscription usage metrics. Alerts fire when usage reaches a percentage of the plan limit or an absolute value. Each alert triggers at most once per billing period and generates a webhook event.

**Why this priority**: Usage alerts depend on the webhook system for notification delivery.

**Independent Test**: Can be tested by creating a percentage alert and recording usage that crosses the threshold, verifying the alert triggers and webhook fires.

**Acceptance Scenarios**:

1. **Given** Active subscription with usageLimit=10000, **When** creating alert metric="api_calls" type=PERCENTAGE value=80, **Then** alert created, triggered=false
2. **Given** Active subscription, **When** creating alert type=ABSOLUTE value=5000, **Then** alert created
3. **Given** subscription with existing 50% alert, **When** creating 80% alert on same metric, **Then** success (multiple alerts per metric allowed)
4. **Given** Alert 80% of 10000, current usage=7900, **When** recording 100 units (total=8000), **Then** alert triggered, usage.threshold_reached webhook event generated
5. **Given** Alert 80% already triggered=true, usage=9000, **When** recording 500 more, **Then** NOT triggered again (once per period)
6. **Given** 50% and 80% alerts, usage=0, **When** recording 5100 of 10000 (51%), **Then** 50% triggered, 80% NOT triggered
7. **Given** triggered alerts from previous period, **When** renewal processes, **Then** all alerts reset (triggered=false)
8. **Given** plan usageLimit=null (unlimited), **When** creating PERCENTAGE alert, **Then** 409 error (requires usage limit)
9. **Given** subscription with 10 active alerts, **When** creating another, **Then** 409 error (limit reached)
10. **Given** Trial subscription, **When** creating alert, **Then** success (allowed in Trial)

---

### User Story 5 - Delete Usage Alert (UC-026, Priority: P2)

A customer can deactivate a usage alert by setting it inactive.

**Why this priority**: Completes the usage alert lifecycle.

**Independent Test**: Can be tested by deleting an alert and verifying it no longer triggers.

**Acceptance Scenarios**:

1. **Given** active alert, **When** deleting, **Then** active=false
2. **Given** non-existent alertId=999, **When** deleting, **Then** 404 error
3. **Given** alertId=0, **When** deleting, **Then** 400 error (invalid ID)

---

### User Story 6 - Schedule Plan Change (UC-027, Priority: P3)

A customer schedules a plan change to take effect at the start of the next billing period (no proration). Only one pending schedule per subscription. The change is applied during renewal, using the new plan's pricing for the invoice.

**Why this priority**: Scheduled changes are a convenience feature that depends on the renewal system and webhook notifications.

**Independent Test**: Can be tested by scheduling a downgrade and verifying it takes effect at the next renewal with the new plan's pricing.

**Acceptance Scenarios**:

1. **Given** Active subscription on PROFESSIONAL, **When** scheduling change to STARTER, **Then** ScheduledPlanChange created, status=Pending, scheduledFor=periodEnd
2. **Given** Active subscription on STARTER, **When** scheduling upgrade to PROFESSIONAL, **Then** success, Pending
3. **Given** Pending schedule PROFESSIONAL→STARTER, renewal triggered, **When** processing renewal, **Then** plan changed to STARTER, invoice uses STARTER pricing, no proration, schedule→Applied
4. **Given** Pending schedule, renewal triggered, **When** applied, **Then** scheduled_change.applied + subscription.plan_changed webhook events
5. **Given** Pending schedule, new plan becomes inactive before renewal, **When** renewal processes, **Then** schedule auto-canceled, invoice uses current plan, scheduled_change.canceled event
6. **Given** Pending schedule A→B, customer does immediate change to C (UC-002), **When** changing, **Then** schedule canceled (immediate overrides), plan=C
7. **Given** Pending schedule, subscription canceled before renewal, **When** canceling, **Then** schedule auto-canceled
8. **Given** existing Pending schedule, **When** scheduling another change, **Then** 409 error (only one pending allowed)
9. **Given** Paused subscription, **When** scheduling change, **Then** 409 error (must be Active)
10. **Given** same plan as current, **When** scheduling, **Then** 409 error
11. **Given** USD subscription, **When** scheduling to EUR plan, **Then** 409 error (currency mismatch)

---

### User Story 7 - Cancel Scheduled Plan Change (UC-028, Priority: P3)

A customer cancels a pending scheduled plan change before it takes effect.

**Why this priority**: Completes the scheduled change lifecycle.

**Independent Test**: Can be tested by canceling a pending schedule and verifying the status transition and webhook event.

**Acceptance Scenarios**:

1. **Given** Pending ScheduledPlanChange, **When** canceling, **Then** status=Canceled, canceledAt=now, scheduled_change.canceled webhook event
2. **Given** no pending schedule for subscription, **When** canceling, **Then** 404 error
3. **Given** non-existent subscription, **When** canceling, **Then** 404 error

---

### User Story 8 - Scheduled Change + Add-on Interaction (UC-029, Priority: P3)

When a scheduled plan change is applied during renewal, incompatible add-ons are detached (no proration since it's at the period boundary). PER_SEAT add-ons are detached when changing from per-seat to non-per-seat plan.

**Why this priority**: Ensures scheduled changes correctly handle the add-on system at renewal time.

**Independent Test**: Can be tested by scheduling a plan change to a tier that makes an add-on incompatible and verifying detachment at renewal.

**Acceptance Scenarios**:

1. **Given** Pending PROFESSIONAL→STARTER, add-on compatible with {PROFESSIONAL} only, **When** renewal applies change, **Then** add-on detached (no proration — boundary change)
2. **Given** Pending change, add-on compatible with both tiers, **When** renewal, **Then** add-on remains Active
3. **Given** Pending per-seat→non-per-seat, PER_SEAT add-on active, **When** renewal, **Then** PER_SEAT add-on detached, seats set to null
4. **Given** scheduled change applied, **When** checking invoice, **Then** invoice includes SCHEDULED_PLAN_CHANGE_NOTE line item (zero-amount, informational)

---

### User Story 9 - Domain Event Webhook Integration (UC-030, Priority: P3)

All domain events from Phase 0/1/2 use cases generate corresponding webhook events for subscribed endpoints. This is a cross-cutting concern affecting UC-001 through UC-021.

**Why this priority**: Integration work that connects the webhook system to all existing use cases.

**Independent Test**: Can be tested by performing various domain actions and verifying the correct webhook events are generated.

**Acceptance Scenarios**:

1. **Given** endpoint subscribed to subscription events, **When** UC-001 creates subscription, **Then** subscription.created event delivered
2. **Given** trial ends with payment success, **When** activating through UC-006, **Then** subscription.activated + invoice.created + invoice.paid events
3. **Given** UC-004 pauses subscription, **When** pausing, **Then** subscription.paused event
4. **Given** UC-004 resumes subscription, **When** resuming, **Then** subscription.resumed event
5. **Given** UC-003 cancels subscription, **When** canceling, **Then** subscription.canceled event
6. **Given** UC-006 renewal fails, **When** processing, **Then** subscription.past_due + invoice.created + invoice.payment_failed events
7. **Given** UC-002 changes plan, **When** changing, **Then** subscription.plan_changed event
8. **Given** UC-018 transfers subscription, **When** transferring, **Then** subscription.transferred event
9. **Given** UC-008 attaches add-on, **When** attaching, **Then** addon.attached + invoice.created events
10. **Given** UC-009 detaches add-on, **When** detaching, **Then** addon.detached event
11. **Given** UC-016 redeems coupon, **When** redeeming, **Then** coupon.redeemed event
12. **Given** UC-011 issues credit note, **When** issuing, **Then** credit_note.issued event

---

### Edge Cases

- What happens when a webhook endpoint URL becomes unreachable permanently — how does the exponential backoff interact with endpoint deactivation?
- How does the system handle a burst of events when a renewal triggers both scheduled plan change, add-on detachment, and invoice creation simultaneously?
- What happens when a usage alert's threshold is PERCENTAGE and the plan is changed to an unlimited plan mid-period?
- How does the system handle a scheduled plan change when the subscription is paused at renewal time (paused subs don't renew)?
- What happens when a webhook delivery is in Failed state and the endpoint is updated with a new URL — does the retry use the new URL?

---

## Clarifications *(resolved)*

### CL-001: WebhookDelivery State Machine

```
Pending     → Delivered (HTTP 2xx response)
Pending     → Failed (HTTP 5xx, timeout, network error)
Pending     → Abandoned (HTTP 4xx — permanent failure, no retry)
Failed      → Delivered (retry succeeds with HTTP 2xx)
Failed      → Failed (retry fails again, attempts < 5)
Failed      → Abandoned (5th attempt fails, or endpoint deleted)
Delivered   → (terminal)
Abandoned   → (terminal)
```

### CL-002: ScheduledPlanChange State Machine

```
Pending     → Applied (renewal processes and applies the change)
Pending     → Canceled (customer cancels, immediate plan change overrides, subscription canceled, or target plan inactive at renewal)
Applied     → (terminal)
Canceled    → (terminal)
```

### CL-003: UsageAlert State Machine

```
Active (triggered=false) → Active (triggered=true) — threshold crossed
Active (triggered=true)  → Active (triggered=false) — renewal resets
Active                   → Inactive (customer deletes)
Inactive                 → (terminal)
```

### CL-004: Webhook Event Types (Closed Enumeration)

```
subscription.created, subscription.activated, subscription.paused, subscription.resumed,
subscription.canceled, subscription.past_due, subscription.plan_changed, subscription.transferred,
invoice.created, invoice.paid, invoice.payment_failed, invoice.voided,
usage.recorded, usage.threshold_reached,
coupon.redeemed, coupon.deactivated,
addon.attached, addon.detached,
credit_note.issued, credit_note.applied,
scheduled_change.applied, scheduled_change.canceled
```
Total: 22 event types.

### CL-005: Webhook Payload Format

```json
{
  "id": "evt_<uuid>",
  "type": "subscription.activated",
  "timestamp": "2026-03-11T10:00:00Z",
  "data": {
    "subscriptionId": 123,
    "customerId": 456,
    ...event-specific fields...
  }
}
```
The `X-Webhook-Signature` header contains: `sha256=<HMAC-SHA256 hex digest of raw JSON body using endpoint secret>`.

### CL-006: Edge Case Resolutions

1. **Permanently unreachable endpoint**: The system retries up to 5 times with exponential backoff (1m, 5m, 30m, 2h, 24h). After the 5th failure, the delivery is Abandoned. The endpoint itself is **NOT** auto-deactivated — it remains active and will attempt delivery for future events. Customers are responsible for monitoring their endpoints. A future admin feature could auto-deactivate endpoints with high failure rates, but this is out of scope for Phase 3.

2. **Burst of events during renewal**: Each domain event generates independent webhook deliveries. When a renewal triggers scheduled plan change + add-on detachment + invoice creation, multiple events are created in sequence:
   - `scheduled_change.applied` → `subscription.plan_changed` → `addon.detached` → `invoice.created` → `invoice.paid` (or `invoice.payment_failed`)
   - Each event creates a separate `WebhookDelivery` per subscribed endpoint. Events are delivered independently (not batched). Ordering is best-effort based on creation timestamp but NOT guaranteed.

3. **PERCENTAGE alert + unlimited plan change**: If a plan is changed to an unlimited plan (`usageLimit=null`) mid-period, existing PERCENTAGE alerts become **inert** — they remain in the system but cannot trigger because there is no limit to calculate against. On the next renewal (with the new unlimited plan), they stay inert. The customer should delete them manually. ABSOLUTE alerts continue to function normally on unlimited plans.

4. **Scheduled change + paused subscription at renewal**: Paused subscriptions do NOT process renewal. The `ScheduledPlanChange` remains in `Pending` status until the subscription is resumed and the next renewal occurs. If the subscription is canceled while paused, the scheduled change is auto-canceled (FR-069).

5. **Failed delivery + endpoint URL update**: Retries use the **current URL** at the time of the retry attempt, not the URL at original delivery time. If a customer updates their endpoint URL, the next retry will target the new URL. This allows customers to fix broken endpoints without losing pending deliveries.

### CL-007: Webhook Delivery Timing

- Webhook deliveries are created **synchronously** when domain events occur (within the same transaction or immediately after).
- Actual HTTP delivery is **asynchronous** — deliveries are queued and processed by a background worker.
- The initial delivery attempt happens as soon as the worker picks up the Pending delivery (near-real-time, not guaranteed latency).

### CL-008: Usage Alert Evaluation Timing

Usage alerts are evaluated **synchronously** within the Record Usage use case (UC-005). After a usage record is persisted:
1. Load all active, non-triggered alerts for the subscription + metric
2. Calculate threshold: PERCENTAGE → `planLimit * value / 100`, ABSOLUTE → `value`
3. If `currentPeriodUsage >= threshold`, set `triggered=true` and emit `usage.threshold_reached` event
4. Multiple alerts can trigger on the same usage recording

### CL-009: FR Numbering

Phase 3 FR numbering starts at FR-075 (continuing from Phase 2's FR-058). Corrected from the original draft which overlapped with Phase 2.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-075**: System MUST allow at most 10 active webhook endpoints per customer
- **FR-076**: System MUST validate webhook endpoint URL is HTTPS
- **FR-077**: System MUST validate webhook secret is at least 32 characters, auto-generate 64-char hex if not provided
- **FR-078**: System MUST sign webhook payloads with HMAC-SHA256 using the endpoint's secret
- **FR-079**: System MUST retry failed webhook deliveries with exponential backoff: 1m, 5m, 30m, 2h, 24h (max 5 attempts)
- **FR-080**: System MUST NOT retry webhook deliveries that receive HTTP 4xx responses (permanent failure)
- **FR-081**: System MUST retry webhook deliveries that receive HTTP 5xx or timeout responses
- **FR-082**: System MUST allow at most 10 active usage alerts per subscription
- **FR-083**: System MUST require usage limit on plan for PERCENTAGE threshold alerts
- **FR-084**: System MUST trigger usage alerts at most once per billing period, resetting on renewal
- **FR-085**: System MUST evaluate alerts synchronously on each usage record creation
- **FR-086**: System MUST allow at most one pending scheduled plan change per subscription
- **FR-087**: System MUST apply scheduled plan changes at period boundary with no proration
- **FR-088**: System MUST cancel pending scheduled changes when an immediate plan change occurs (UC-002)
- **FR-089**: System MUST cancel pending scheduled changes when the subscription is canceled
- **FR-090**: System MUST auto-cancel scheduled changes during renewal if the target plan is inactive
- **FR-091**: System MUST use the new plan's pricing for renewal invoice when a scheduled change is applied
- **FR-092**: System MUST NOT abandon queued deliveries when a subscription is canceled (only endpoint deletion abandons)
- **FR-093**: System MUST validate webhook event types against the closed set of valid types
- **FR-094**: System MUST enforce 30-second timeout per webhook delivery attempt

### Key Entities

- **WebhookEndpoint**: Customer-configured HTTPS URL for event notifications. Subscribes to specific event types. Secret for HMAC-SHA256 signing. Max 10 per customer.
- **WebhookEventType**: Closed enumeration of 21 domain event types across subscription, invoice, usage, coupon, add-on, credit note, and schedule categories.
- **WebhookDelivery**: Tracks each delivery attempt. Status: Pending→Delivered/Failed→Abandoned. Exponential backoff retry. Max 5 attempts.
- **UsageAlert**: Threshold-based alert on usage metrics. Types: PERCENTAGE (of plan limit) or ABSOLUTE. Triggers once per billing period, resets on renewal. Generates webhook event.
- **ScheduledPlanChange**: Future plan change at next period boundary. Status: Pending→Applied/Canceled. No proration. Applied during renewal (UC-006). Max 1 per subscription.
- **Invoice Line Item (extended)**: New type SCHEDULED_PLAN_CHANGE_NOTE (zero-amount informational line item).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 9 Phase 3 use cases (UC-022 through UC-030) pass their acceptance scenarios
- **SC-002**: Webhook delivery system correctly handles success, retry, and permanent failure scenarios
- **SC-003**: Exponential backoff timing matches specification (1m, 5m, 30m, 2h, 24h)
- **SC-004**: All 21 domain event types correctly trigger webhook deliveries to subscribed endpoints
- **SC-005**: Usage alerts trigger exactly once per billing period and reset on renewal
- **SC-006**: Scheduled plan changes apply at renewal with correct pricing and no proration
- **SC-007**: Scheduled changes auto-cancel on subscription cancellation, immediate plan change, or target plan deactivation
- **SC-008**: HMAC-SHA256 signatures are correct and verifiable by consumers
- **SC-009**: 80%+ test coverage maintained across all modules (Kover)
- **SC-010**: All custom detekt rules continue to pass
