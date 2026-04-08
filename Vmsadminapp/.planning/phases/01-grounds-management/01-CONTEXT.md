# Phase 01 Context: Grounds Management

## Phase Goal
Add a Grounds Management screen to the Admin App so ops staff can view all sports grounds, enable/disable them, and see real-time system-managed status.

## Codebase Reality (from exploration)

### App Framework
- **Jetpack Compose** (not Android XML — despite initial description, all screens use Compose)
- Pattern: `ViewModel (StateFlow UiState) → Repository → ApiService`
- UI: `AppCard`, `StatusBadge`, `shimmerEffect` reusable components
- Navigation: `NavHost` inside `MainScreen`, routes added per feature

### Naming: Grounds = Carts
The backend introduced `/api/v1/grounds` as a **domain-named facade** over the existing Cart service.
Internal mapping: `label → name`, `cart_type_id → sport_id`, `region_id → location_id`

The existing `CartsScreen` + `Cart` model = the old name for what is now "Grounds".
**Do NOT rename or modify the Carts screen.** Add a new Grounds feature alongside it.

### Backend API: Grounds
- `GET /api/v1/grounds` → `{ success, data: [Ground] }`
- `PUT /api/v1/grounds/{id}` → `{ success, data: Ground }` with `{ is_active: bool }` body
- **Status is system-managed** — backend rejects any attempt to set `status` in the request.
  Status override is NOT possible through the current API. Display only.

### Ground Response Shape
```
{
  id: Int,
  name: String,          // display label (was cart.label)
  sport_id: Int,         // sport type (was cart.cart_type_id)
  location_id: Int,      // region (was cart.region_id)
  status: String,        // AVAILABLE | BUSY (system-managed, read-only)
  is_active: Boolean,    // admin-controllable
  latitude: Float?,
  longitude: Float?,
  created_at: String?,
  updated_at: String?
}
```

### Files to Create
- `models/Models.kt` — add `Ground`, `UpdateGroundRequest`
- `network/ApiService.kt` — add getGrounds, updateGround
- `data/GroundRepository.kt` — new file
- `viewmodel/GroundViewModel.kt` — new file
- `ui/screens/GroundsScreen.kt` — new file
- `ui/screens/PlaceholderScreens.kt` — add Grounds entry to ManageScreen
- `ui/screens/MainScreen.kt` — accept GroundViewModel + add nav route
- `navigation/AppNavigation.kt` — pass GroundViewModel

### Existing Pattern to Follow
`CartsScreen.kt` is the closest analogy. `GroundsScreen` should follow the same structure:
- `AppCard` per item
- `StatusBadge` for status
- `Switch` for is_active toggle
- `PullToRefreshBox` for refresh
- Shimmer skeleton on load
- Error state with Retry button
- Empty state message

## Constraints
- No animations added (skip the `AnimatedVisibility` slide-in that CartsScreen uses)
- No add/edit/delete — only display + toggle is_active
- Status is read-only — show it but no override button
- Reuse `AppCard`, `StatusBadge`, existing component library

## What Phase 01 Establishes
This phase wires up the full feature layer (model → api → repo → viewmodel → screen → navigation).
Subsequent phases (Queue, System Config) only need to add navigation wiring (screens already exist).
