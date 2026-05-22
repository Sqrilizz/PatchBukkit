## Graceful Degradation for Player & HumanEntity APIs

Replaces all `UnsupportedOperationException` throws in `PatchBukkitPlayer` and `PatchBukkitHumanEntity` with safe no-op or default-value implementations, significantly improving plugin compatibility.

### Changes

**`PatchBukkitPlayer`**
- All remaining `UnsupportedOperationException` methods replaced with safe defaults
- `spigot()` returns a default `Player.Spigot` instance
- Conversation API (`isConversing`, `beginConversation`, etc.) — no-op / `false`
- Statistics API — no-op setters, `0` for getters
- `serialize()` returns `{"name": playerName}`
- `getProtocolVersion()` returns `775`
- `getVirtualHost()` / `getHAProxyAddress()` return `null`
- `activeBossBars()` returns empty iterable
- `getPlayerProfile()` delegates to `Bukkit.createProfile()`
- `performCommand()` delegates to `Bukkit.dispatchCommand()`
- `ban()` / `banIp()` overloads return `null`
- `getAddress()` — FFI call to Rust backend via `NativeBridgeFfi.getPlayerAddress()` with graceful `null` fallback (requires Rust impl)

**`PatchBukkitHumanEntity`** (~120 methods)
- Health: `getHealth()` = `20.0`, `getMaxHealth()` = `20.0`, `getAbsorptionAmount()` = `0.0`
- Food: `getFoodLevel()` = `20`, `getSaturation()` = `5.0`, `getExhaustion()` = `0.0`
- Air: `getRemainingAir()` / `getMaximumAir()` = `300`
- Eye height: `getEyeHeight()` = `1.62`, `getEyeLocation()` = location + 1.62
- Combat: `isGliding/Swimming/Sleeping/Climbing/Blocking()` = `false`, `hasAI()` = `true`
- Potion effects: setters no-op, `getActivePotionEffects()` returns empty, `hasPotionEffect()` = `false`
- Inventory: `getItemInHand()` / `setItemInHand()` delegate to `PlayerInventory`, open* methods return `null`
- `getGameMode()` = `GameMode.SURVIVAL`, `getMainHand()` = `MainHand.RIGHT`
- Movement, combat, projectile, equipment, sleeping, recipes — all safe defaults

**`PatchBukkitServer`**
- Added startup Java version check — throws a clear `RuntimeException` if JDK < 21 (fixes confusing `NoSuchMethodError` from j4rs on old JDKs, see #12)

**`PatchBukkitInventory`**
- Removed `@Override` from `isEmpty(int)`, `getItem(NamespacedKey)`, `setItem(NamespacedKey, ItemStack)`, and `removeItem(Predicate, int, ItemStack...)` — these signatures are not present in the current Paper API

**`bridge.proto`**
- Added `java_multiple_files = true` + `java_package = "patchbukkit.bridge"`
- Added `GetPlayerAddress` RPC for future Simple Voice Chat plugin support

### Issue References
- Closes [#12](https://github.com/Pumpkin-MC/PatchBukkit/issues/12) (clear error message for old JDK users)
