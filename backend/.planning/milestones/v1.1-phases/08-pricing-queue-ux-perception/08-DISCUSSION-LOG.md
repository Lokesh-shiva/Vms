# Phase 08: Pricing & Queue UX Perception - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-02
**Phase:** 08-pricing-queue-ux-perception
**Areas discussed:** Wait message phrasing & tone, Pricing reason copy, Pricing reason placement, Edge-case messaging

---

## Area 1: Wait Message Phrasing & Tone

**Q: What message style for WAITING state?**
Options: Time estimate / Player count / Both combined / You decide
→ Selected: **Both combined**

**Q: Exact format for combined style?**
Options: "Searching for opponent… (2 nearby, ~2 min wait)" / "2 players nearby — match likely in 2 mins" / You decide
→ Selected: **"2 players nearby — match likely in 2 mins"**

**Q: When queue_count = 0?**
Options: "Searching for opponent…" / "No players nearby yet — hang tight" / "You're first in line!"
→ Selected: **"No players nearby yet — hang tight"**

**Q: MATCHED state message?**
Options: "Opponent found!" / "Match ready — proceed to ground" / "You're matched — arrive in 20 mins or lose your spot"
→ Selected: **"You're matched — arrive in 20 mins or lose your spot"**

---

## Area 2: Pricing Reason Copy

**Q: Reason string approach?**
Options: Separate reasons per trigger / Compound when both / Single generic
→ Selected: **Separate reasons per trigger**

**Q: No surge reason?**
Options: "Standard pricing" / Omit field entirely / "Best time to play!"
→ Selected: **"Standard pricing"**

---

## Area 3: Pricing Reason Placement

**Q: Where should 'reason' live?**
Options: Inside calculate_price() only / Queue status only / Both independently
→ Selected: **Inside calculate_price() return dict only**
Note: This automatically propagates to queue_status since it embeds the full pricing dict.

---

## Area 4: Edge-Case Messaging

**Q: Show wait_estimation_msg when MATCHED?**
Options: Yes (replace with matched msg) / Yes (return both) / No (omit)
→ Selected: **Yes — replace with matched message**

**Q: Field location in queue_status response?**
Options: Top-level / Nested inside entry / You decide
→ Selected: **Top-level field alongside entry, pricing, etc.**
