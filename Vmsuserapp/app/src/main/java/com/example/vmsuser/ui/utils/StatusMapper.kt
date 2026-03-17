package com.example.vmsuser.ui.utils

object StatusMapper {

    fun bookingStatusLabel(status: String?): String = when (status) {
        "PENDING_PAYMENT" -> "Awaiting Payment"
        "UNDER_REVIEW"    -> "Payment Under Review"
        "CONFIRMED"       -> "Payment Verified"
        "IN_PROGRESS"     -> "Service In Progress"
        "COMPLETED"       -> "Completed"
        "CANCELLED"       -> "Cancelled"
        else              -> status ?: "Unknown"
    }

    fun paymentStatusLabel(status: String?): String = when (status) {
        "PENDING"      -> "Payment Pending"
        "UNDER_REVIEW" -> "Under Review"
        "SUCCESS"      -> "Payment Successful"
        "FAILED"       -> "Payment Failed"
        "REFUNDED"     -> "Refunded"
        else           -> status ?: "Unknown"
    }
}
