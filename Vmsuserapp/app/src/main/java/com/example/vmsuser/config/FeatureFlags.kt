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
    const val NOTIFICATIONS = true    // /api/v1/notifications + /api/v1/users/me/fcm-token, Firebase Admin SDK wired
    const val CHAT = true             // /api/v1/chat/threads, /api/v1/matches/{id}/messages — polling every 3s
    const val CAPTAIN_ONBOARDING = true // /api/v1/captains/apply, /me/kyc, /me/application-status — manual admin review
    const val OPEN_MATCHES = true     // /api/v1/matches/open
    const val WALLET = true           // /api/v1/wallet/balance + /transactions — real earn-only ledger, no purchase path
}
