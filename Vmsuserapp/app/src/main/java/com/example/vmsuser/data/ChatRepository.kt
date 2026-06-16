package com.example.vmsuser.data

import com.example.vmsuser.models.ChatMessage
import com.example.vmsuser.models.ChatThread

class ChatRepository {
    fun getMockThreads(): List<ChatThread> = listOf(
        ChatThread("1", "Badminton Match #42", "See you on the court tomorrow!", "10:30 AM", 2, null, "match"),
        ChatThread("2", "Indiranagar BC", "Great session everyone! 💪", "Yesterday", 0, null, "group"),
        ChatThread("3", "Rahul Singh (Captain)", "Please arrive 10 min early", "Yesterday", 1, null, "direct"),
    )

    fun getMockMessages(threadId: String): List<ChatMessage> = listOf(
        ChatMessage("1", 2, "Rahul Singh", "Hey team! Ready for tomorrow's match?", "10:00 AM", false),
        ChatMessage("2", 1, "You", "Yes! Bringing my new racket 🏸", "10:05 AM", true),
        ChatMessage("3", 3, "Priya Sharma", "Me too! What time should we reach?", "10:10 AM", false),
        ChatMessage("4", 2, "Rahul Singh", "Please arrive 10 min early. Court opens at 6 PM.", "10:15 AM", false),
        ChatMessage("5", 1, "You", "Perfect, see you all there!", "10:30 AM", true),
    )
}
