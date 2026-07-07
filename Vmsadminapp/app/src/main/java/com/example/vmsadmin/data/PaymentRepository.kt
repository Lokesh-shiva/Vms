package com.example.vmsadmin.data

import com.example.vmsadmin.models.Payment
import com.example.vmsadmin.models.PaymentReportEntry
import com.example.vmsadmin.models.PaymentSummary
import com.example.vmsadmin.network.ApiService

class PaymentRepository(private val apiService: ApiService) {

    suspend fun fetchPayments(): List<Payment> {
        val response = apiService.getPayments()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch payments")
    }

    suspend fun fetchSummary(): PaymentSummary {
        val response = apiService.getPaymentSummary()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch summary")
    }

    suspend fun fetchReport(startDate: String, endDate: String): List<PaymentReportEntry> {
        val response = apiService.getPaymentReport(startDate, endDate)
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch report")
    }

    suspend fun approvePayment(paymentId: Int) {
        val response = apiService.approvePayment(paymentId)
        if (!response.success) {
            throw Exception(response.message ?: "Failed to approve payment")
        }
    }

    suspend fun rejectPayment(paymentId: Int) {
        val response = apiService.rejectPayment(paymentId)
        if (!response.success) {
            throw Exception(response.message ?: "Failed to reject payment")
        }
    }

    suspend fun refundPayment(paymentId: Int) {
        val response = apiService.refundPayment(paymentId)
        if (!response.success) {
            throw Exception(response.message ?: "Failed to refund payment")
        }
    }
}
