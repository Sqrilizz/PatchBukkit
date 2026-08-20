use pumpkin::{
    command::args::entities::{EntitySelectorType, TargetSelector},
    entity::EntityBase,
};

use crate::{
    java::native_callbacks::{CALLBACK_CONTEXT, utils::with_player},
    proto::patchbukkit::{
        common::Uuid,
        entity::{
            DamageEntityRequest, EntityHealthResponse, SetEntityHealthRequest,
            SetEntityVelocityRequest, TeleportEntityRequest,
        },
    },
};

pub fn ffi_native_bridge_get_entity_health_impl(request: Uuid) -> Option<EntityHealthResponse> {
    with_player(Some(&request), |player| {
        let health = f64::from(player.living_entity.health.load());

        EntityHealthResponse {
            health,
            max_health: 20.0,
        }
    })
}

pub fn ffi_native_bridge_set_entity_health_impl(request: SetEntityHealthRequest) -> Option<()> {
    let ctx = CALLBACK_CONTEXT.get()?;
    with_player(request.uuid.as_ref(), |player| {
        let health = request.health as f32;
        let player = player.clone();
        ctx.runtime.spawn(async move {
            player.set_health(health).await;
        });
    })
}

pub fn ffi_native_bridge_damage_entity_impl(request: DamageEntityRequest) -> Option<()> {
    let ctx = CALLBACK_CONTEXT.get()?;
    with_player(request.uuid.as_ref(), |player| {
        let amount = request.amount as f32;
        let player = player.clone();
        ctx.runtime.spawn(async move {
            let current = player.living_entity.health.load();
            player.set_health((current - amount).max(0.0)).await;
        });
    })
}

pub fn ffi_native_bridge_get_entity_velocity_impl(
    request: Uuid,
) -> Option<crate::proto::patchbukkit::entity::EntityVelocityResponse> {
    with_player(Some(&request), |player| {
        let vel = player.living_entity.entity.velocity.load();
        crate::proto::patchbukkit::entity::EntityVelocityResponse {
            x: vel.x,
            y: vel.y,
            z: vel.z,
        }
    })
}

pub fn ffi_native_bridge_set_entity_velocity_impl(request: SetEntityVelocityRequest) -> Option<()> {
    with_player(request.uuid.as_ref(), |player| {
        let velocity = pumpkin_util::math::vector3::Vector3::new(request.x, request.y, request.z);
        player.living_entity.entity.set_velocity(velocity);
    })
}

pub fn ffi_native_bridge_set_entity_pose_impl(
    _request: crate::proto::patchbukkit::entity::SetEntityPoseRequest,
) -> Option<()> {
    Some(())
}

pub fn ffi_native_bridge_get_gamemode_impl(
    request: Uuid,
) -> Option<crate::proto::patchbukkit::entity::GetGamemodeResponse> {
    with_player(Some(&request), |player| {
        let gamemode = player.gamemode.load();
        crate::proto::patchbukkit::entity::GetGamemodeResponse {
            gamemode: gamemode as i32,
        }
    })
}

pub fn ffi_native_bridge_set_gamemode_impl(
    request: crate::proto::patchbukkit::entity::SetGamemodeRequest,
) -> Option<()> {
    let ctx = CALLBACK_CONTEXT.get()?;
    with_player(request.uuid.as_ref(), |player| {
        let gamemode_val = request.gamemode as i8;
        if let Ok(gamemode) = pumpkin_util::gamemode::GameMode::try_from(gamemode_val) {
            let player = player.clone();
            ctx.runtime.spawn(async move {
                player.set_gamemode(gamemode).await;
            });
        }
    })
}

pub fn ffi_native_bridge_teleport_entity_impl(request: TeleportEntityRequest) -> Option<()> {
    let ctx = CALLBACK_CONTEXT.get()?;
    let loc = request.location?;
    let pos = loc.position?;
    let yaw = loc.yaw;
    let pitch = loc.pitch;

    with_player(request.uuid.as_ref(), |player| {
        let position = pumpkin_util::math::vector3::Vector3::new(pos.x, pos.y, pos.z);
        let world = player.living_entity.entity.world.load_full();
        let player = player.clone();
        ctx.runtime.spawn(async move {
            player
                .teleport(position, Some(yaw), Some(pitch), world)
                .await;
        });
    })
}

pub fn ffi_native_bridge_is_on_ground_impl(
    request: Uuid,
) -> Option<crate::proto::patchbukkit::entity::IsOnGroundResponse> {
    with_player(Some(&request), |player| {
        let on_ground = player
            .living_entity
            .entity
            .on_ground
            .load(std::sync::atomic::Ordering::Relaxed);
        crate::proto::patchbukkit::entity::IsOnGroundResponse { on_ground }
    })
}

pub fn ffi_native_bridge_get_player_locale_impl(
    request: Uuid,
) -> Option<crate::proto::patchbukkit::entity::GetPlayerLocaleResponse> {
    with_player(Some(&request), |player| {
        let config = player.config.load();
        crate::proto::patchbukkit::entity::GetPlayerLocaleResponse {
            locale: config.locale.clone(),
        }
    })
}

pub fn ffi_native_bridge_is_op_impl(
    request: Uuid,
) -> Option<crate::proto::patchbukkit::entity::IsOpResponse> {
    let ctx = CALLBACK_CONTEXT.get()?;
    let player_uuid = uuid::Uuid::parse_str(&request.value).ok()?;

    let is_op = with_player(Some(&request), |player| {
        let perm_lvl = player.permission_lvl.load();
        let op_lvl = ctx.plugin_context.server.basic_config.op_permission_level;
        let mut is_op = perm_lvl >= op_lvl || perm_lvl > pumpkin_util::PermissionLvl::Zero;

        if !is_op
            && let Ok(op_config) = ctx.plugin_context.server.data.operator_config.try_read()
            && op_config.get_entry(&player_uuid).is_some()
        {
            is_op = true;
        }

        is_op
    })
    .unwrap_or_else(|| {
        ctx.plugin_context
            .server
            .data
            .operator_config
            .try_read()
            .is_ok_and(|ops| ops.get_entry(&player_uuid).is_some())
    });

    Some(crate::proto::patchbukkit::entity::IsOpResponse { is_op })
}

pub fn ffi_native_bridge_get_entity_bounding_box_impl(
    request: Uuid,
) -> Option<crate::proto::patchbukkit::entity::EntityBoundingBoxResponse> {
    let ctx = CALLBACK_CONTEXT.get()?;
    let uuid = uuid::Uuid::parse_str(&request.value).ok()?;
    let entity = ctx
        .plugin_context
        .server
        .select_entities(&TargetSelector::new(EntitySelectorType::Uuid(uuid)), None)
        .into_iter()
        .next()?
        .get_entity()
        .bounding_box
        .load();
    Some(
        crate::proto::patchbukkit::entity::EntityBoundingBoxResponse {
            min_x: entity.min.x,
            min_y: entity.min.y,
            min_z: entity.min.z,
            max_x: entity.max.x,
            max_y: entity.max.y,
            max_z: entity.max.z,
        },
    )
}
