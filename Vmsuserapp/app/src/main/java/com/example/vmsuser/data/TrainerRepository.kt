package com.example.vmsuser.data

import android.util.Log
import com.example.vmsuser.models.CreateTrainerBookingRequest
import com.example.vmsuser.models.Trainer
import com.example.vmsuser.models.TrainerBookingDto
import com.example.vmsuser.network.RetrofitClient
import com.example.vmsuser.network.toUserMessage
import retrofit2.HttpException

class TrainerRepository {
    private val api = RetrofitClient.api

    suspend fun getTrainers(): Result<List<Trainer>> = try {
        val res = api.getTrainers()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed to load trainers."))
    } catch (e: HttpException) {
        Log.e("TrainerRepo", "getTrainers", e)
        Result.failure(Exception(e.toUserMessage("Failed to load trainers.")))
    } catch (e: Exception) {
        Log.e("TrainerRepo", "getTrainers", e)
        Result.failure(Exception(e.message ?: "Failed to load trainers."))
    }

    suspend fun getTrainer(id: Int): Result<Trainer> = try {
        val res = api.getTrainer(id)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Trainer not found."))
    } catch (e: HttpException) {
        Log.e("TrainerRepo", "getTrainer", e)
        Result.failure(Exception(e.toUserMessage("Trainer not found.")))
    } catch (e: Exception) {
        Log.e("TrainerRepo", "getTrainer", e)
        Result.failure(Exception(e.message ?: "Trainer not found."))
    }

    suspend fun createBooking(trainerId: Int, sessionDate: String, sessionTime: String): Result<TrainerBookingDto> = try {
        val res = api.createTrainerBooking(CreateTrainerBookingRequest(trainerId, sessionDate, sessionTime))
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed to book session."))
    } catch (e: HttpException) {
        Log.e("TrainerRepo", "createBooking", e)
        Result.failure(Exception(e.toUserMessage("Failed to book session.")))
    } catch (e: Exception) {
        Log.e("TrainerRepo", "createBooking", e)
        Result.failure(Exception(e.message ?: "Failed to book session."))
    }

    suspend fun getMyBookings(): Result<List<TrainerBookingDto>> = try {
        val res = api.getMyTrainerBookings()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed to load bookings."))
    } catch (e: HttpException) {
        Log.e("TrainerRepo", "getMyBookings", e)
        Result.failure(Exception(e.toUserMessage("Failed to load bookings.")))
    } catch (e: Exception) {
        Log.e("TrainerRepo", "getMyBookings", e)
        Result.failure(Exception(e.message ?: "Failed to load bookings."))
    }

    suspend fun getBooking(id: Int): Result<TrainerBookingDto> = try {
        val res = api.getTrainerBooking(id)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Booking not found."))
    } catch (e: HttpException) {
        Log.e("TrainerRepo", "getBooking", e)
        Result.failure(Exception(e.toUserMessage("Booking not found.")))
    } catch (e: Exception) {
        Log.e("TrainerRepo", "getBooking", e)
        Result.failure(Exception(e.message ?: "Booking not found."))
    }

    suspend fun submitPayment(id: Int, transactionId: String): Result<TrainerBookingDto> = try {
        val res = api.submitTrainerBookingPayment(id, mapOf("transaction_id" to transactionId))
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed to submit payment."))
    } catch (e: HttpException) {
        Log.e("TrainerRepo", "submitPayment", e)
        Result.failure(Exception(e.toUserMessage("Failed to submit payment.")))
    } catch (e: Exception) {
        Log.e("TrainerRepo", "submitPayment", e)
        Result.failure(Exception(e.message ?: "Failed to submit payment."))
    }
}
