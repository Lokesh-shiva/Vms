package com.example.vmsadmin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vmsadmin.data.UserManagementRepository
import com.example.vmsadmin.models.ApiResponse
import com.example.vmsadmin.models.AppUser
import com.example.vmsadmin.models.CreateCartRequest
import com.example.vmsadmin.models.CreateCartTypeRequest
import com.example.vmsadmin.models.CreateFeeConfigRequest
import com.example.vmsadmin.models.CreateItemRequest
import com.example.vmsadmin.models.CreateRegionRequest
import com.example.vmsadmin.models.CreateTimeslotRequest
import com.example.vmsadmin.models.LoginRequest
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
import com.example.vmsadmin.ui.screens.UsersScreen
import com.example.vmsadmin.viewmodel.UserManagementViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsersScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun user(id: Int, name: String = "User $id", role: String = "user", active: Boolean = true) =
        AppUser(id = id, name = name, phone = "+100$id", role = role, is_active = active)

    private fun makeVm(
        users: List<AppUser>,
        currentUserId: Int? = null
    ): UserManagementViewModel {
        val api = object : ApiService by noOpApi() {
            override suspend fun getUsers() = ApiResponse(success = true, data = users)
            override suspend fun updateUser(id: Int, r: UpdateUserRequest) =
                ApiResponse(success = true, data = users.first { it.id == id })
        }
        return UserManagementViewModel(UserManagementRepository(api), currentUserId)
    }

    @Test
    fun successState_displaysUserNames() {
        val vm = makeVm(listOf(user(1, name = "Alice"), user(2, name = "Bob")))
        composeTestRule.setContent {
            UsersScreen(viewModel = vm, currentUserId = null, onBack = {})
        }
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun successState_displaysPhoneNumbers() {
        val vm = makeVm(listOf(user(1, name = "Alice")))
        composeTestRule.setContent {
            UsersScreen(viewModel = vm, currentUserId = null, onBack = {})
        }
        composeTestRule.onNodeWithText("+1001").assertIsDisplayed()
    }

    @Test
    fun selfRow_showsYouLabel() {
        val vm = makeVm(listOf(user(1, name = "Me"), user(2, name = "Other")), currentUserId = 1)
        composeTestRule.setContent {
            UsersScreen(viewModel = vm, currentUserId = 1, onBack = {})
        }
        composeTestRule.onNodeWithText("(you)").assertIsDisplayed()
    }

    @Test
    fun selfRow_hasNoOverflowMenu() {
        val vm = makeVm(listOf(user(1, name = "Me")), currentUserId = 1)
        composeTestRule.setContent {
            UsersScreen(viewModel = vm, currentUserId = 1, onBack = {})
        }
        // Overflow menu button should not exist for self row
        composeTestRule.onNodeWithContentDescription("Actions").assertDoesNotExist()
    }

    @Test
    fun nonSelfRow_hasOverflowMenu() {
        val vm = makeVm(listOf(user(1, name = "Alice"), user(2, name = "Bob")), currentUserId = 1)
        composeTestRule.setContent {
            UsersScreen(viewModel = vm, currentUserId = 1, onBack = {})
        }
        composeTestRule.onNodeWithContentDescription("Actions").assertIsDisplayed()
    }

    @Test
    fun overflowMenu_activeUser_showsDeactivateOption() {
        val vm = makeVm(listOf(user(1, name = "Alice", active = true)), currentUserId = null)
        composeTestRule.setContent {
            UsersScreen(viewModel = vm, currentUserId = null, onBack = {})
        }
        composeTestRule.onNodeWithContentDescription("Actions").performClick()
        composeTestRule.onNodeWithText("Deactivate").assertIsDisplayed()
    }

    @Test
    fun overflowMenu_inactiveUser_showsReactivateOption() {
        val vm = makeVm(listOf(user(1, name = "Alice", active = false)), currentUserId = null)
        composeTestRule.setContent {
            UsersScreen(viewModel = vm, currentUserId = null, onBack = {})
        }
        composeTestRule.onNodeWithContentDescription("Actions").performClick()
        composeTestRule.onNodeWithText("Reactivate").assertIsDisplayed()
    }

    @Test
    fun changeRoleMenu_opensDialogWithAllRoles() {
        val vm = makeVm(listOf(user(1, name = "Alice")), currentUserId = null)
        composeTestRule.setContent {
            UsersScreen(viewModel = vm, currentUserId = null, onBack = {})
        }
        composeTestRule.onNodeWithContentDescription("Actions").performClick()
        composeTestRule.onNodeWithText("Change role").performClick()
        // Dialog should show all 8 roles
        listOf(
            "super_admin", "ops_manager", "ground_owner", "tournament_manager",
            "support", "finance", "csr_partner", "user"
        ).forEach { role ->
            composeTestRule.onNodeWithText(role).assertIsDisplayed()
        }
    }

    @Test
    fun errorState_showsRetryButton() {
        val api = object : ApiService by noOpApi() {
            override suspend fun getUsers(): ApiResponse<List<AppUser>> = throw Exception("Offline")
            override suspend fun updateUser(id: Int, r: UpdateUserRequest): ApiResponse<AppUser> =
                throw UnsupportedOperationException()
        }
        val vm = UserManagementViewModel(UserManagementRepository(api), currentUserId = null)
        composeTestRule.setContent {
            UsersScreen(viewModel = vm, currentUserId = null, onBack = {})
        }
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun topBar_showsUsersTitle() {
        val vm = makeVm(emptyList())
        composeTestRule.setContent {
            UsersScreen(viewModel = vm, currentUserId = null, onBack = {})
        }
        composeTestRule.onNodeWithText("Users").assertIsDisplayed()
    }

    // ── No-op ApiService delegate ────────────────────────────────────

    private fun noOpApi(): ApiService = object : ApiService {
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
        override suspend fun updateGround(id: Int, request: UpdateGroundRequest) = throw UnsupportedOperationException()
        override suspend fun getMatches() = throw UnsupportedOperationException()
        override suspend fun cancelMatch(matchId: Int) = throw UnsupportedOperationException()
        override suspend fun completeMatch(matchId: Int) = throw UnsupportedOperationException()
        override suspend fun getUsers() = throw UnsupportedOperationException()
        override suspend fun updateUser(id: Int, request: UpdateUserRequest) = throw UnsupportedOperationException()
        override suspend fun getSystemConfigs() = throw UnsupportedOperationException()
        override suspend fun updateSystemConfig(key: String, request: UpdateConfigRequest) = throw UnsupportedOperationException()
        override suspend fun getQueueStats() = throw UnsupportedOperationException()
    }
}
