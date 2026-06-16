package com.example.vmsuser.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int = 0,
    val name: String = "",
    val phone: String = "",
    val email: String? = null,
    val role: String = "user",
    val region: String = "",
    val gender: String? = null,
    val sports: List<String> = emptyList(),
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("coin_balance") val coinBalance: Int = 0,
    @SerialName("match_streak") val matchStreak: Int = 0,
    @SerialName("win_rate") val winRate: Float = 0f,
    @SerialName("matches_played") val matchesPlayed: Int = 0,
    @SerialName("skill_level") val skillLevel: String = "Intermediate",
    @SerialName("ghost_strikes") val ghostStrikes: Int = 0,
    @SerialName("xp_points") val xpPoints: Int = 0,
    @SerialName("level") val level: Int = 1,
    @SerialName("can_create_society") val canCreateSociety: Boolean = false,
    @SerialName("is_captain") val isCaptain: Boolean = false,
    // Registration fields
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val city: String? = null,
    @SerialName("sport_preferences") val sportPreferences: List<String> = emptyList(),
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    @SerialName("is_profile_complete") val isProfileComplete: Boolean = false,
)

@Serializable
data class OtpVerifyResponse(
    val token: String,
    @SerialName("is_new_user") val isNewUser: Boolean,
)

@Serializable
data class CompleteProfileRequest(
    val name: String,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val city: String,
    @SerialName("sport_preferences") val sportPreferences: List<String>,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null,
)

@Serializable
data class QueueEntry(
    val id: Int = 0,
    val sport: String = "",
    val status: String = "waiting",
    @SerialName("skill_level") val skillLevel: String = "Intermediate",
    @SerialName("joined_at") val joinedAt: String = "",
    @SerialName("match_id") val matchId: Int? = null,
)

@Serializable
data class QueueStatus(
    @SerialName("in_queue") val inQueue: Boolean = false,
    @SerialName("queue_position") val queuePosition: Int = 0,
    @SerialName("players_searching") val playersSearching: Int = 0,
    @SerialName("estimated_wait_seconds") val estimatedWaitSeconds: Int = 0,
    val sport: String? = null,
    @SerialName("match_found") val matchFound: Boolean = false,
    @SerialName("match_id") val matchId: Int? = null,
)

@Serializable
data class JoinQueueRequest(
    val sport: String,
    @SerialName("skill_level") val skillLevel: String = "Intermediate",
)

@Serializable
data class Match(
    val id: Int = 0,
    val sport: String = "",
    val status: String = "",
    @SerialName("ground_name") val groundName: String = "",
    @SerialName("ground_address") val groundAddress: String = "",
    @SerialName("scheduled_at") val scheduledAt: String = "",
    @SerialName("captain_name") val captainName: String? = null,
    @SerialName("captain_id") val captainId: Int? = null,
    @SerialName("player_ids") val playerIds: List<Int> = emptyList(),
    val price: Int = 0,
)

@Serializable
data class Tournament(
    val id: Int = 0,
    val name: String = "",
    val sport: String = "",
    val status: String = "upcoming",
    @SerialName("entry_fee") val entryFee: Int = 0,
    @SerialName("prize_pool") val prizePool: String = "",
    @SerialName("max_teams") val maxTeams: Int = 0,
    @SerialName("registered_teams") val registeredTeams: Int = 0,
    @SerialName("start_date") val startDate: String = "",
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("location") val location: String = "",
    @SerialName("banner_url") val bannerUrl: String? = null,
    val description: String = "",
    val format: String = "knockout",
    @SerialName("organizer_name") val organizerName: String = "",
)

@Serializable
data class Society(
    val id: Int = 0,
    val name: String = "",
    val sport: String = "",
    val description: String = "",
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("is_member") val isMember: Boolean = false,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_by") val createdBy: Int = 0,
    val visibility: String = "public",
)

@Serializable
data class SocietyMember(
    val id: Int = 0,
    val name: String = "",
    val role: String = "member",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("skill_level") val skillLevel: String = "Intermediate",
)

@Serializable
data class ChatThread(
    val id: String = "",
    val name: String = "",
    @SerialName("last_message") val lastMessage: String = "",
    @SerialName("last_message_time") val lastMessageTime: String = "",
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val type: String = "direct",
)

@Serializable
data class ChatMessage(
    val id: String = "",
    @SerialName("from_id") val fromId: Int = 0,
    @SerialName("from_name") val fromName: String = "",
    val text: String = "",
    val timestamp: String = "",
    @SerialName("is_self") val isSelf: Boolean = false,
)

@Serializable
data class Notification(
    val id: Int = 0,
    val type: String = "",
    val title: String = "",
    val body: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val read: Boolean = false,
)

@Serializable
data class WalletTransaction(
    val id: Int = 0,
    val type: String = "",
    val amount: Int = 0,
    val description: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class CaptainStats(
    @SerialName("today_earnings") val todayEarnings: Int = 0,
    @SerialName("week_earnings") val weekEarnings: Int = 0,
    @SerialName("rating") val rating: Float = 0f,
    @SerialName("matches_led") val matchesLed: Int = 0,
    @SerialName("active_matches") val activeMatches: List<Match> = emptyList(),
)

@Serializable
data class LocationOption(
    val id: Int = 0,
    val name: String = "",
)

@Serializable
data class CreateSocietyRequest(
    val name: String,
    val sport: String,
    val description: String,
    val visibility: String = "public",
)
