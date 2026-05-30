package com.example.vmsadmin.viewmodel

import com.example.vmsadmin.data.UserManagementRepository
import com.example.vmsadmin.models.ApiResponse
import com.example.vmsadmin.models.AppUser
import com.example.vmsadmin.models.CreateCaptainRequest
import com.example.vmsadmin.models.CreateCartRequest
import com.example.vmsadmin.models.CreateCartTypeRequest
import com.example.vmsadmin.models.CreateFeeConfigRequest
import com.example.vmsadmin.models.CreateItemRequest
import com.example.vmsadmin.models.CreateRegionRequest
import com.example.vmsadmin.models.CreateTimeslotRequest
import com.example.vmsadmin.models.LoginRequest
import com.example.vmsadmin.models.UpdateCaptainRequest
import com.example.vmsadmin.models.UpdateCartRequest
import com.example.vmsadmin.models.UpdateCartTypeRequest
import com.example.vmsadmin.models.UpdateConfigRequest
import com.example.vmsadmin.models.UpdateFeeConfigRequest
import com.example.vmsadmin.models.UpdateGroundRequest
import com.example.vmsadmin.models.UpdateItemRequest
import com.example.vmsadmin.models.UpdateRegionRequest
import com.example.vmsadmin.models.UpdateTimeslotRequest
import com.example.vmsadmin.models.UpdateUserRequest
import com.example.vmsadmin.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserManagementViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun user(id: Int, role: String = "user", active: Boolean = true) =
        AppUser(id = id, name = "User $id", phone = "+1000$id", role = role, is_active = active)

    private fun fakeRepo(
        usersResult: () -> List<AppUser> = { emptyList() },
        updateResult: (Int, UpdateUserRequest) -> AppUser = { id, _ -> user(id) }
    ): UserManagementRepository {
        val fakeApi = object : ApiService by noOpApiService() {
            override suspend fun getUsers(): ApiResponse<List<AppUser>> =
                ApiResponse(success = true, data = usersResult())

            override suspend fun updateUser(id: Int, request: UpdateUserRequest): ApiResponse<AppUser> =
                ApiResponse(success = true, data = updateResult(id, request))
        }
        return UserManagementRepository(fakeApi)
    }

    // ── Tests ─────────────────────────────────────────────────────────

    @Test
    fun loadUsers_emitsSuccessWithList() = runTest {
        val users = listOf(user(1), user(2))
        val vm = UserManagementViewModel(fakeRepo(usersResult = { users }), currentUserId = null)
        val state = vm.state.value
        assertTrue(state is UserManagementState.Success)
        assertEquals(users, (state as UserManagementState.Success).users)
    }

    @Test
    fun loadUsers_networkError_emitsErrorState() = runTest {
        val failRepo = UserManagementRepository(object : ApiService by noOpApiService() {
            override suspend fun getUsers(): ApiResponse<List<AppUser>> =
                throw Exception("Network failure")

            override suspend fun updateUser(id: Int, request: UpdateUserRequest): ApiResponse<AppUser> =
                throw UnsupportedOperationException()
        })
        val vm = UserManagementViewModel(failRepo, currentUserId = null)
        val state = vm.state.value
        assertTrue(state is UserManagementState.Error)
        assertEquals("Network failure", (state as UserManagementState.Error).message)
    }

    @Test
    fun changeRole_updatesUserInList() = runTest {
        val original = listOf(user(1, role = "user"), user(2))
        val updated = user(1, role = "ops_manager")
        val vm = UserManagementViewModel(
            fakeRepo(usersResult = { original }, updateResult = { _, _ -> updated }),
            currentUserId = null
        )
        vm.changeRole(1, "ops_manager")
        val state = vm.state.value as UserManagementState.Success
        assertEquals("ops_manager", state.users.first { it.id == 1 }.role)
    }

    @Test
    fun changeRole_onSelf_isNoOp() = runTest {
        var updateCalled = false
        val vm = UserManagementViewModel(
            fakeRepo(
                usersResult = { listOf(user(1)) },
                updateResult = { _, _ ->
                    updateCalled = true
                    user(1)
                }
            ),
            currentUserId = 1
        )
        vm.changeRole(1, "ops_manager")
        assertFalse("updateRole must not be called on self", updateCalled)
    }

    @Test
    fun toggleActive_deactivatesUser() = runTest {
        val original = listOf(user(1, active = true))
        val deactivated = user(1, active = false)
        val vm = UserManagementViewModel(
            fakeRepo(usersResult = { original }, updateResult = { _, _ -> deactivated }),
            currentUserId = null
        )
        vm.toggleActive(1, currentActive = true)
        val state = vm.state.value as UserManagementState.Success
        assertFalse(state.users.first { it.id == 1 }.is_active)
    }

    @Test
    fun toggleActive_onSelf_isNoOp() = runTest {
        var updateCalled = false
        val vm = UserManagementViewModel(
            fakeRepo(
                usersResult = { listOf(user(1, active = true)) },
                updateResult = { _, _ ->
                    updateCalled = true
                    user(1)
                }
            ),
            currentUserId = 1
        )
        vm.toggleActive(1, currentActive = true)
        assertFalse("setActive must not be called on self", updateCalled)
    }

    @Test
    fun pendingIds_setDuringMutation_clearedAfter() = runTest {
        val vm = UserManagementViewModel(
            fakeRepo(usersResult = { listOf(user(1)) }),
            currentUserId = null
        )
        // After mutation completes, pendingIds should be empty
        vm.changeRole(1, "ops_manager")
        assertTrue(vm.pendingIds.value.isEmpty())
    }

    @Test
    fun refreshUsers_updatesListWithoutErrorState() = runTest {
        val initial = listOf(user(1))
        val refreshed = listOf(user(1), user(2))
        var callCount = 0
        val repo = UserManagementRepository(object : ApiService by noOpApiService() {
            override suspend fun getUsers(): ApiResponse<List<AppUser>> {
                callCount++
                return ApiResponse(success = true, data = if (callCount == 1) initial else refreshed)
            }

            override suspend fun updateUser(id: Int, request: UpdateUserRequest): ApiResponse<AppUser> =
                throw UnsupportedOperationException()
        })
        val vm = UserManagementViewModel(repo, currentUserId = null)
        vm.refreshUsers()
        val state = vm.state.value as UserManagementState.Success
        assertEquals(2, state.users.size)
    }

    // ── No-op ApiService delegate ─────────────────────────────────────

    private fun noOpApiService(): ApiService = object : ApiService {
        override suspend fun login(request: LoginRequest) = throw UnsupportedOperationException()
        override suspend fun getDashboardStats() = throw UnsupportedOperationException()
        override suspend fun getBookings() = throw UnsupportedOperationException()
        override suspend fun startBooking(bookingId: Int) = throw UnsupportedOperationException()
        override suspend fun completeBooking(bookingId: Int) = throw UnsupportedOperationException()
        override suspend fun cancelBooking(bookingId: Int) = throw UnsupportedOperationException()
        override suspend fun getPaymentsConfig() = throw UnsupportedOperationException()
        override suspend fun getPayments() = throw UnsupportedOperationException()
        override suspend fun approvePayment(paymentId: Int) = throw UnsupportedOperationException()
        override suspend fun rejectPayment(paymentId: Int) = throw UnsupportedOperationException()
        override suspend fun refundPayment(paymentId: Int) = throw UnsupportedOperationException()
        override suspend fun getRegions() = throw UnsupportedOperationException()
        override suspend fun createRegion(request: CreateRegionRequest) = throw UnsupportedOperationException()
        override suspend fun updateRegion(locationId: Int, request: UpdateRegionRequest) = throw UnsupportedOperationException()
        override suspend fun deleteRegion(locationId: Int) = throw UnsupportedOperationException()
        override suspend fun getCartTypes() = throw UnsupportedOperationException()
        override suspend fun getCartType(cartTypeId: Int) = throw UnsupportedOperationException()
        override suspend fun createCartType(request: CreateCartTypeRequest) = throw UnsupportedOperationException()
        override suspend fun updateCartType(cartTypeId: Int, request: UpdateCartTypeRequest) = throw UnsupportedOperationException()
        override suspend fun deleteCartType(cartTypeId: Int) = throw UnsupportedOperationException()
        override suspend fun getTimeslots() = throw UnsupportedOperationException()
        override suspend fun getTimeslot(timeslotId: Int) = throw UnsupportedOperationException()
        override suspend fun createTimeslot(request: CreateTimeslotRequest) = throw UnsupportedOperationException()
        override suspend fun updateTimeslot(timeslotId: Int, request: UpdateTimeslotRequest) = throw UnsupportedOperationException()
        override suspend fun deleteTimeslot(timeslotId: Int) = throw UnsupportedOperationException()
        override suspend fun getCarts() = throw UnsupportedOperationException()
        override suspend fun getCartById(id: Int) = throw UnsupportedOperationException()
        override suspend fun createCart(request: CreateCartRequest) = throw UnsupportedOperationException()
        override suspend fun updateCart(id: Int, request: UpdateCartRequest) = throw UnsupportedOperationException()
        override suspend fun deleteCart(id: Int) = throw UnsupportedOperationException()
        override suspend fun getFeeConfigs() = throw UnsupportedOperationException()
        override suspend fun getFeeConfig(regionId: Int, cartTypeId: Int) = throw UnsupportedOperationException()
        override suspend fun createFeeConfig(request: CreateFeeConfigRequest) = throw UnsupportedOperationException()
        override suspend fun updateFeeConfig(id: Int, request: UpdateFeeConfigRequest) = throw UnsupportedOperationException()
        override suspend fun deleteFeeConfig(id: Int) = throw UnsupportedOperationException()
        override suspend fun getItems() = throw UnsupportedOperationException()
        override suspend fun getItemsByCartType(cartTypeId: Int) = throw UnsupportedOperationException()
        override suspend fun createItem(request: CreateItemRequest) = throw UnsupportedOperationException()
        override suspend fun updateItem(itemId: Int, request: UpdateItemRequest) = throw UnsupportedOperationException()
        override suspend fun deleteItem(itemId: Int) = throw UnsupportedOperationException()
        override suspend fun getGrounds() = throw UnsupportedOperationException()
        override suspend fun getGroundsByRegion(regionId: Int) = throw UnsupportedOperationException()
        override suspend fun updateGround(id: Int, request: UpdateGroundRequest) = throw UnsupportedOperationException()
        override suspend fun getMatches() = throw UnsupportedOperationException()
        override suspend fun cancelMatch(matchId: Int) = throw UnsupportedOperationException()
        override suspend fun completeMatch(matchId: Int) = throw UnsupportedOperationException()
        override suspend fun getUsers() = throw UnsupportedOperationException()
        override suspend fun updateUser(id: Int, request: UpdateUserRequest) = throw UnsupportedOperationException()
        override suspend fun getSystemConfigs() = throw UnsupportedOperationException()
        override suspend fun updateSystemConfig(key: String, request: UpdateConfigRequest) = throw UnsupportedOperationException()
        override suspend fun getQueueStats() = throw UnsupportedOperationException()
        override suspend fun searchUserByPhone(phone: String) = throw UnsupportedOperationException()
        override suspend fun getCaptains() = throw UnsupportedOperationException()
        override suspend fun createCaptain(request: CreateCaptainRequest) = throw UnsupportedOperationException()
        override suspend fun updateCaptain(id: Int, request: UpdateCaptainRequest) = throw UnsupportedOperationException()
        override suspend fun deleteCaptain(id: Int) = throw UnsupportedOperationException()
    }
}
