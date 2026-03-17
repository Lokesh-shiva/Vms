package com.example.vmsuser.repository

import com.example.vmsuser.models.ConfirmPaymentRequest
import com.example.vmsuser.models.InitiatePaymentResponse
import com.example.vmsuser.models.Payment
import com.example.vmsuser.network.ApiService
import retrofit2.HttpException

class PaymentRepository(private val apiService: ApiService) : BaseRepository() {

    suspend fun initiatePayment(bookingId: Int): InitiatePaymentResponse {
        try {
            val response = apiService.initiatePayment(bookingId)
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to initiate payment")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to initiate payment")
        }
    }

    suspend fun submitTransactionId(bookingId: Int, transactionId: String): Payment {
        try {
            val response = apiService.confirmManualPayment(
                bookingId = bookingId,
                request = ConfirmPaymentRequest(transaction_id = transactionId)
            )
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to submit transaction ID")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to submit transaction ID")
        }
    }
}
