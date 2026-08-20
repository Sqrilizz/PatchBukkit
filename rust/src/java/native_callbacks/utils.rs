use super::CALLBACK_CONTEXT;
use crate::proto::patchbukkit::common::Uuid as ProtoUuid;
use pumpkin::entity::player::Player;
use std::collections::HashMap;
use std::ffi::{CStr, c_char};
use std::sync::{Arc, RwLock};

static PLAYER_HANDLE_CACHE: RwLock<Option<HashMap<uuid::Uuid, Arc<Player>>>> = RwLock::new(None);

#[must_use]
pub fn get_string(str_ptr: *const c_char) -> String {
    unsafe { CStr::from_ptr(str_ptr).to_string_lossy().into_owned() }
}

pub fn cache_player(player: Arc<Player>) {
    let player_uuid = player.gameprofile.id;
    if let Ok(mut write_guard) = PLAYER_HANDLE_CACHE.write() {
        let cache = write_guard.get_or_insert_with(HashMap::new);
        cache.insert(player_uuid, player);
    }
}

pub fn with_player<F, R>(proto_uuid: Option<&ProtoUuid>, f: F) -> Option<R>
where
    F: FnOnce(Arc<Player>) -> R,
{
    let Some(ctx) = CALLBACK_CONTEXT.get() else {
        tracing::warn!("Player callback invoked before CALLBACK_CONTEXT was initialized");
        return None;
    };
    let Some(uuid) = proto_uuid else {
        tracing::warn!("Player callback invoked without a player UUID");
        return None;
    };
    let Ok(player_uuid) = uuid::Uuid::parse_str(&uuid.value) else {
        tracing::warn!("Player callback received an invalid UUID: {}", uuid.value);
        return None;
    };

    if let Ok(read_guard) = PLAYER_HANDLE_CACHE.read()
        && let Some(ref cache) = *read_guard
        && let Some(player) = cache.get(&player_uuid)
    {
        return Some(f(player.clone()));
    }

    let Some(player) = ctx.plugin_context.server.get_player_by_uuid(player_uuid) else {
        tracing::warn!("Player callback could not find online player {player_uuid}");
        return None;
    };
    if let Ok(mut write_guard) = PLAYER_HANDLE_CACHE.write() {
        let cache = write_guard.get_or_insert_with(HashMap::new);
        cache.insert(player_uuid, player.clone());
    }

    Some(f(player))
}
