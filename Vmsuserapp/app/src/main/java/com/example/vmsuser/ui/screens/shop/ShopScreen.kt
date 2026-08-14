package com.example.vmsuser.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.vmsuser.models.ShopItem
import com.example.vmsuser.models.SportItem
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.network.RetrofitClient
import com.example.vmsuser.network.absoluteMediaUrl
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.ShopViewModel

@Composable
fun ShopScreen(navController: NavController) {
    val parentEntry = remember(navController) { navController.getBackStackEntry("shop_graph") }
    val vm: ShopViewModel = viewModel(parentEntry)

    val shopItems by vm.items.collectAsState()
    val loading by vm.itemsLoading.collectAsState()
    val error by vm.itemsError.collectAsState()
    val cart by vm.cart.collectAsState()

    var sports by remember { mutableStateOf<List<SportItem>>(emptyList()) }
    var selectedSport by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        try {
            val res = RetrofitClient.api.getSports()
            if (res.success && res.data != null) sports = res.data.filter { it.isActive }
        } catch (_: Exception) {}
    }

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        PlixoTopBar(title = "Shop", onBack = { navController.popBackStack() })

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SportFilterChip("All", selectedSport == null) {
                    selectedSport = null
                    vm.loadItems(null)
                }
            }
            lazyRowItems(sports) { sport ->
                SportFilterChip(sport.name, selectedSport == sport.id) {
                    selectedSport = sport.id
                    vm.loadItems(sport.id)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                loading && shopItems.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PlixoPrimary)
                    }
                }
                error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error!!, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoDanger)
                    }
                }
                shopItems.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Filled.Storefront, null, tint = PlixoText3, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Nothing here yet", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PlixoText)
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(shopItems) { shopItem ->
                            ShopItemCard(
                                item = shopItem,
                                quantity = cart[shopItem.id] ?: 0,
                                onAdd = { vm.addToCart(shopItem.id) },
                                onRemove = { vm.removeFromCart(shopItem.id) },
                            )
                        }
                    }
                }
            }
        }

        if (cart.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PlixoInk)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        navController.navigate(Screen.Checkout.route)
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.ShoppingCart, null, tint = PlixoLime, modifier = Modifier.size(20.dp))
                    Text(
                        "${vm.cartCount()} item${if (vm.cartCount() == 1) "" else "s"} · ₹${"%.0f".format(vm.cartTotal())}",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                    )
                }
                Text("View cart", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PlixoLime)
            }
        }
    }
}

@Composable
private fun SportFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) PlixoInk else PlixoSurface2)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (selected) Color.White else PlixoText,
        )
    }
}

@Composable
private fun ShopItemCard(item: ShopItem, quantity: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PlixoSurface),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(PlixoSurface2)) {
            val url = absoluteMediaUrl(item.imageUrl)
            if (url != null) {
                AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Storefront, null, tint = PlixoText3, modifier = Modifier.size(28.dp))
                }
            }
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.name, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = PlixoText, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text("₹${"%.0f".format(item.price)}", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PlixoPrimary)
            Spacer(Modifier.height(8.dp))

            if (quantity == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PlixoPrimaryLight)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAdd() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Add", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = PlixoPrimary)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PlixoPrimary),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onRemove() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Remove, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                    Text("$quantity", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    Box(
                        modifier = Modifier.size(32.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAdd() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }
}
