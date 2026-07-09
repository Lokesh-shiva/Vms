package com.example.vmsuser.notifications

import com.example.vmsuser.navigation.Screen

/**
 * Maps a backend notification `type` (see NOTIFICATION_TYPE constants used by
 * notification_service.send() across the backend) to the in-app route to
 * navigate to on tap. Shared by the cold-start deep link path
 * (PlixoMessagingService -> MainActivity -> PendingDeepLink) and the
 * in-app tap path (NotificationsScreen).
 */
fun notificationDeepLinkRoute(type: String, matchId: Int?): String = when (type) {
    "MATCH_FOUND" -> matchId?.let { Screen.ActiveMatch.create(it) } ?: Screen.Home.route
    "CHAT_MESSAGE" -> matchId?.let { Screen.ChatThread.create(it.toString()) } ?: Screen.Chat.route
    "CAPTAIN_APPROVED", "CAPTAIN_REJECTED" -> Screen.KycStatus.route
    "CAPTAIN_PAYOUT" -> Screen.CaptainEarnings.route
    else -> Screen.Notifications.route
}
