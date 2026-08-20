use crate::{
    java::native_callbacks::CALLBACK_CONTEXT,
    proto::patchbukkit::world::{
        GetBlockDataRequest, GetBlockDataResponse, SetBlockDataRequest, SpawnParticleRequest,
    },
};

pub fn ffi_native_bridge_get_block_data_impl(
    request: GetBlockDataRequest,
) -> Option<GetBlockDataResponse> {
    let ctx = CALLBACK_CONTEXT.get()?;
    let uuid_str = &request.world_uuid.as_ref()?.value;
    let world_uuid = uuid::Uuid::parse_str(uuid_str).ok()?;

    let worlds = ctx.plugin_context.server.worlds.load_full();
    let world = worlds
        .iter()
        .find(|w| w.uuid == world_uuid)
        .cloned()
        .or_else(|| worlds.first().cloned())?;
    let pos = pumpkin_util::math::position::BlockPos::new(request.x, request.y, request.z);

    let state_id = world.get_block_state(&pos).id;
    let block = pumpkin_data::Block::from_state_id(state_id);
    let key = block.name;
    let block_state = if key.starts_with("minecraft:") {
        key.to_string()
    } else {
        format!("minecraft:{key}")
    };

    Some(GetBlockDataResponse { block_state })
}

pub fn ffi_native_bridge_set_block_data_impl(request: SetBlockDataRequest) -> Option<()> {
    let ctx = CALLBACK_CONTEXT.get()?;
    let uuid_str = &request.world_uuid.as_ref()?.value;
    let world_uuid = uuid::Uuid::parse_str(uuid_str).ok()?;

    let worlds = ctx.plugin_context.server.worlds.load_full();
    let world = worlds
        .iter()
        .find(|w| w.uuid == world_uuid)
        .cloned()
        .or_else(|| worlds.first().cloned())?;
    let pos = pumpkin_util::math::position::BlockPos::new(request.x, request.y, request.z);

    let block_state_str = request.block_state;
    let clean_key = block_state_str
        .split('[')
        .next()
        .unwrap_or(&block_state_str)
        .trim_start_matches("minecraft:");

    let state_id = if let Some(b) = pumpkin_data::Block::from_registry_key(clean_key) {
        b.default_state.id
    } else {
        pumpkin_data::BlockStateId::new_or_air(0)
    };

    ctx.runtime.spawn(async move {
        world
            .set_block_state(&pos, state_id, pumpkin::world::BlockFlags::NOTIFY_ALL)
            .await;
    });

    Some(())
}

pub fn ffi_native_bridge_spawn_particle_impl(_request: SpawnParticleRequest) -> Option<()> {
    Some(())
}

pub fn ffi_native_bridge_get_worlds_impl(
    _request: crate::proto::patchbukkit::common::EmptyRequest,
) -> Option<crate::proto::patchbukkit::world::GetWorldsResponse> {
    let ctx = CALLBACK_CONTEXT.get()?;
    let worlds = ctx.plugin_context.server.worlds.load_full();
    let world_uuids = worlds
        .iter()
        .map(|w| crate::proto::patchbukkit::common::Uuid {
            value: w.uuid.to_string(),
        })
        .collect();

    Some(crate::proto::patchbukkit::world::GetWorldsResponse { world_uuids })
}

pub fn ffi_native_bridge_get_world_border_impl(
    request: crate::proto::patchbukkit::world::GetWorldBorderRequest,
) -> Option<crate::proto::patchbukkit::world::WorldBorderData> {
    let ctx = CALLBACK_CONTEXT.get()?;
    let uuid_str = &request.world_uuid.as_ref()?.value;
    let world_uuid = uuid::Uuid::parse_str(uuid_str).ok()?;

    let worlds = ctx.plugin_context.server.worlds.load_full();
    let world = worlds
        .iter()
        .find(|w| w.uuid == world_uuid)
        .cloned()
        .or_else(|| worlds.first().cloned())?;

    let wb = world.worldborder.try_lock().ok()?;

    Some(crate::proto::patchbukkit::world::WorldBorderData {
        center_x: wb.center_x,
        center_z: wb.center_z,
        size: wb.old_diameter,
        target_size: wb.new_diameter,
        speed: wb.speed,
        warning_time: wb.warning_time,
        warning_blocks: wb.warning_blocks,
        damage_per_block: wb.damage_per_block as f64,
        damage_buffer: wb.buffer as f64,
        max_center_coordinate: wb.portal_teleport_boundary,
    })
}

pub fn ffi_native_bridge_set_world_border_impl(
    request: crate::proto::patchbukkit::world::SetWorldBorderRequest,
) -> Option<()> {
    let ctx = CALLBACK_CONTEXT.get()?;
    let uuid_str = &request.world_uuid.as_ref()?.value;
    let world_uuid = uuid::Uuid::parse_str(uuid_str).ok()?;
    let border_data = request.border?;

    let worlds = ctx.plugin_context.server.worlds.load_full();
    let world = worlds
        .iter()
        .find(|w| w.uuid == world_uuid)
        .cloned()
        .or_else(|| worlds.first().cloned())?;

    ctx.runtime.spawn(async move {
        let mut wb = world.worldborder.lock().await;
        wb.center_x = border_data.center_x;
        wb.center_z = border_data.center_z;
        wb.old_diameter = border_data.size;
        wb.new_diameter = border_data.target_size;
        wb.speed = border_data.speed;
        wb.warning_time = border_data.warning_time;
        wb.warning_blocks = border_data.warning_blocks;
        wb.damage_per_block = border_data.damage_per_block as f32;
        wb.buffer = border_data.damage_buffer as f32;
        wb.portal_teleport_boundary = border_data.max_center_coordinate;
    });

    Some(())
}

pub fn ffi_native_bridge_get_world_info_impl(
    request: crate::proto::patchbukkit::world::GetWorldInfoRequest,
) -> Option<crate::proto::patchbukkit::world::GetWorldInfoResponse> {
    let ctx = CALLBACK_CONTEXT.get()?;
    let uuid_str = &request.world_uuid.as_ref()?.value;
    let world_uuid = uuid::Uuid::parse_str(uuid_str).ok()?;

    let worlds = ctx.plugin_context.server.worlds.load_full();
    let world = worlds
        .iter()
        .find(|w| w.uuid == world_uuid)
        .cloned()
        .or_else(|| worlds.first().cloned())?;

    let min_height = world.dimension.min_y;
    let height = world.dimension.height;
    let max_height = min_height + height;

    Some(crate::proto::patchbukkit::world::GetWorldInfoResponse {
        min_height,
        max_height,
        height,
        seed: 0,
    })
}

pub fn ffi_native_bridge_get_loaded_chunks_impl(
    request: crate::proto::patchbukkit::world::GetLoadedChunksRequest,
) -> Option<crate::proto::patchbukkit::world::GetLoadedChunksResponse> {
    let ctx = CALLBACK_CONTEXT.get()?;
    let world_uuid = uuid::Uuid::parse_str(&request.world_uuid?.value).ok()?;
    let world = ctx
        .plugin_context
        .server
        .worlds
        .load_full()
        .iter()
        .find(|world| world.uuid == world_uuid)
        .cloned()?;
    let chunks = world
        .level
        .loaded_chunks
        .iter()
        .map(|chunk| crate::proto::patchbukkit::world::ChunkPosition {
            x: chunk.key().x,
            z: chunk.key().y,
        })
        .collect();
    Some(crate::proto::patchbukkit::world::GetLoadedChunksResponse { chunks })
}

pub fn ffi_native_bridge_is_chunk_loaded_impl(
    request: crate::proto::patchbukkit::world::IsChunkLoadedRequest,
) -> Option<crate::proto::patchbukkit::world::IsChunkLoadedResponse> {
    let ctx = CALLBACK_CONTEXT.get()?;
    let world_uuid = uuid::Uuid::parse_str(&request.world_uuid?.value).ok()?;
    let world = ctx
        .plugin_context
        .server
        .worlds
        .load_full()
        .iter()
        .find(|world| world.uuid == world_uuid)
        .cloned()?;
    let position = pumpkin_util::math::vector2::Vector2::new(request.x, request.z);
    Some(crate::proto::patchbukkit::world::IsChunkLoadedResponse {
        loaded: world.level.is_chunk_loaded(&position),
    })
}

pub fn ffi_native_bridge_get_world_statistics_impl(
    request: crate::proto::patchbukkit::world::GetWorldStatisticsRequest,
) -> Option<crate::proto::patchbukkit::world::GetWorldStatisticsResponse> {
    let ctx = CALLBACK_CONTEXT.get()?;
    let world_uuid = uuid::Uuid::parse_str(&request.world_uuid?.value).ok()?;
    let world = ctx
        .plugin_context
        .server
        .worlds
        .load_full()
        .iter()
        .find(|world| world.uuid == world_uuid)
        .cloned()?;
    let tile_entity_count = world
        .block_entities
        .iter()
        .map(|chunk| chunk.value().len())
        .sum::<usize>();
    let tickable_tile_entity_count = world
        .active_chunks
        .load()
        .iter()
        .filter_map(|chunk_pos| world.block_entities.get(chunk_pos))
        .map(|chunk| chunk.value().len())
        .sum::<usize>();
    Some(
        crate::proto::patchbukkit::world::GetWorldStatisticsResponse {
            player_count: world.players.load().len() as i32,
            entity_count: (world.players.load().len() + world.entities.load().len()) as i32,
            tile_entity_count: tile_entity_count as i32,
            tickable_tile_entity_count: tickable_tile_entity_count as i32,
        },
    )
}
