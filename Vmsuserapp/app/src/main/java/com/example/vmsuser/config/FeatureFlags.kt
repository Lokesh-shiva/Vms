package com.example.vmsuser.config

/**
 * Central kill-switch for user-app features.
 *
 * Each flag below is OFF because the backend (or admin app) does not yet expose
 * the endpoint the feature needs. Screens and routes for these features are NOT
 * deleted — they stay registered and are simply unreachable from the UI. To
 * re-enable a feature once its backend lands, flip the flag back to `true`.
 *
 * Verified against backend/modules on 2026-06-15 (see docs/plan-user-app-feature-gating.md).
 */
object FeatureFlags {

    // ── ON — backend endpoints exist and are wired ──────────────────────────
    const val MATCHMAKING = true      // /api/v1/matchmaking/play-now, /status, /leave
    const val TOURNAMENTS = true      // /api/v1/tournaments (+ /{id}, /register)
    const val SOCIETIES = true        // /api/v1/societies (+ members/join/leave/create)
    const val CAPTAIN_DASHBOARD = true // /api/v1/captains/me/stats (existing captains only)

    // ── OFF — no backend support yet ────────────────────────────────────────

    /** No chat module in backend. ChatRepository is pure mock. */
    const val CHAT = false

    /** No /api/v1/notifications endpoint. */
    const val NOTIFICATIONS = false

    /** Wallet balance/transactions endpoints are stubs returning 0 / empty — no real ledger. */
    const val WALLET = false

    /** No /api/v1/captains/apply and no KYC upload endpoints. */
    const val CAPTAIN_ONBOARDING = false

    /** OpenMatchesScreen still uses mock data — no backend browse-open-matches endpoint yet. */
    const val OPEN_MATCHES = false
}
