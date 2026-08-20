use std::marker::PhantomData;
use std::sync::Arc;

use pumpkin::entity::player::Player;
use pumpkin::plugin::{BoxFuture, Cancellable, EventHandler, Payload};
use pumpkin::server::Server;
use tokio::sync::{mpsc, oneshot};

use crate::java::jvm::commands::JvmCommand;
use crate::proto::patchbukkit::common::Uuid;
use crate::proto::patchbukkit::events::event::Data;
use crate::proto::patchbukkit::events::{
    BlockBreakEvent, BlockDamageEvent, BlockFadeEvent, BlockFormEvent, BlockGrowEvent,
    BlockIgniteEvent, BlockPlaceEvent, Event, PlayerChatEvent, PlayerGameModeChangeEvent,
    PlayerInteractEntityEvent, PlayerInteractEvent, PlayerJoinEvent, PlayerMoveEvent,
    PlayerQuitEvent, PlayerResourcePackStatusEvent, PlayerToggleFlightEvent,
    PlayerToggleSneakEvent, PlayerToggleSprintEvent, ServerCommandEvent, SignChangeEvent,
};

pub struct EventContext {
    pub server: Arc<Server>,
    pub player: Option<Arc<Player>>,
}

pub struct JvmEventPayload {
    pub event: Event,
    pub context: EventContext,
}

pub trait PatchBukkitEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload;
    fn apply_modifications(&mut self, server: &Arc<Server>, data: Data) -> Option<()>;
}

impl PatchBukkitEvent for pumpkin::plugin::player::player_join::PlayerJoinEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::PlayerJoin(PlayerJoinEvent {
                    player_uuid: Some(Uuid {
                        value: self.player.gameprofile.id.to_string(),
                    }),
                    join_message: serde_json::to_string(&self.join_message).unwrap_or_default(),
                })),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, data: Data) -> Option<()> {
        if let Data::PlayerJoin(event) = data
            && let Ok(msg) = serde_json::from_str(&event.join_message)
        {
            self.join_message = msg;
        }

        Some(())
    }
}

impl PatchBukkitEvent for pumpkin::plugin::player::player_leave::PlayerLeaveEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::PlayerQuit(PlayerQuitEvent {
                    player_uuid: Some(Uuid {
                        value: self.player.gameprofile.id.to_string(),
                    }),
                    quit_message: serde_json::to_string(&self.leave_message).unwrap_or_default(),
                })),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, data: Data) -> Option<()> {
        if let Data::PlayerQuit(event) = data
            && let Ok(msg) = serde_json::from_str(&event.quit_message)
        {
            self.leave_message = msg;
        }

        Some(())
    }
}

impl PatchBukkitEvent
    for pumpkin::plugin::player::player_resource_pack_status::PlayerResourcePackStatusEvent
{
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::PlayerResourcePackStatus(
                    PlayerResourcePackStatusEvent {
                        player_uuid: Some(Uuid {
                            value: self.player.gameprofile.id.to_string(),
                        }),
                        pack_uuid: Some(Uuid {
                            value: self.pack_id.clone(),
                        }),
                        status: match self.status.as_str() {
                            "DownloadSuccess" => "SUCCESSFULLY_LOADED",
                            "Declined" => "DECLINED",
                            "DownloadFail" => "FAILED_DOWNLOAD",
                            "Accepted" => "ACCEPTED",
                            "Downloaded" => "DOWNLOADED",
                            "InvalidUrl" => "INVALID_URL",
                            "ReloadFailed" => "FAILED_RELOAD",
                            "Discarded" => "DISCARDED",
                            _ => "FAILED_DOWNLOAD",
                        }
                        .to_string(),
                    },
                )),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

impl PatchBukkitEvent
    for pumpkin::plugin::player::player_gamemode_change::PlayerGamemodeChangeEvent
{
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::PlayerGamemodeChange(PlayerGameModeChangeEvent {
                    player_uuid: Some(Uuid {
                        value: self.player.gameprofile.id.to_string(),
                    }),
                    new_gamemode: format!("{:?}", self.new_gamemode),
                })),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

impl PatchBukkitEvent for pumpkin::plugin::player::player_interact_event::PlayerInteractEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        let (x, y, z) = match self.clicked_pos {
            Some(pos) => (pos.0.x, pos.0.y, pos.0.z),
            None => (0, 0, 0),
        };
        let action_str = match self.action {
            pumpkin::plugin::player::player_interact_event::InteractAction::LeftClickBlock => {
                "LEFT_CLICK_BLOCK"
            }
            pumpkin::plugin::player::player_interact_event::InteractAction::LeftClickAir => {
                "LEFT_CLICK_AIR"
            }
            pumpkin::plugin::player::player_interact_event::InteractAction::RightClickAir => {
                "RIGHT_CLICK_AIR"
            }
            pumpkin::plugin::player::player_interact_event::InteractAction::RightClickBlock => {
                "RIGHT_CLICK_BLOCK"
            }
        };
        JvmEventPayload {
            event: Event {
                data: Some(Data::PlayerInteract(PlayerInteractEvent {
                    player_uuid: Some(Uuid {
                        value: self.player.gameprofile.id.to_string(),
                    }),
                    action: action_str.to_string(),
                    clicked_x: x,
                    clicked_y: y,
                    clicked_z: z,
                    block_face: "SELF".to_string(),
                    hand: "HAND".to_string(),
                })),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

impl PatchBukkitEvent for pumpkin::plugin::block::block_break::BlockBreakEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::BlockBreak(BlockBreakEvent {
                    player_uuid: self.player.as_ref().map(|p| Uuid {
                        value: p.gameprofile.id.to_string(),
                    }),
                    block_x: self.block_position.0.x,
                    block_y: self.block_position.0.y,
                    block_z: self.block_position.0.z,
                    block_type: format!("{:?}", self.block),
                    exp: self.exp,
                    drop_items: self.drop,
                })),
            },
            context: EventContext {
                server,
                player: self.player.clone(),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

impl PatchBukkitEvent for pumpkin::plugin::block::block_place::BlockPlaceEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::BlockPlace(BlockPlaceEvent {
                    player_uuid: Some(Uuid {
                        value: self.player.gameprofile.id.to_string(),
                    }),
                    block_x: self.block_position.0.x,
                    block_y: self.block_position.0.y,
                    block_z: self.block_position.0.z,
                    block_placed_type: format!("{:?}", self.block_placed),
                    block_against_type: format!("{:?}", self.block_placed_against),
                    can_build: self.can_build,
                })),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

impl PatchBukkitEvent
    for pumpkin::plugin::player::player_toggle_sneak_event::PlayerToggleSneakEvent
{
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::PlayerToggleSneak(PlayerToggleSneakEvent {
                    player_uuid: Some(Uuid {
                        value: self.player.gameprofile.id.to_string(),
                    }),
                    is_sneaking: self.is_sneaking,
                })),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

impl PatchBukkitEvent
    for pumpkin::plugin::player::player_toggle_sprint_event::PlayerToggleSprintEvent
{
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::PlayerToggleSprint(PlayerToggleSprintEvent {
                    player_uuid: Some(Uuid {
                        value: self.player.gameprofile.id.to_string(),
                    }),
                    is_sprinting: self.is_sprinting,
                })),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

impl PatchBukkitEvent
    for pumpkin::plugin::player::player_toggle_flight_event::PlayerToggleFlightEvent
{
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::PlayerToggleFlight(PlayerToggleFlightEvent {
                    player_uuid: Some(Uuid {
                        value: self.player.gameprofile.id.to_string(),
                    }),
                    is_flying: self.is_flying,
                })),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

impl PatchBukkitEvent for pumpkin::plugin::player::player_move::PlayerMoveEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        let move_vel = self.to - self.from;
        self.player.living_entity.entity.velocity.store(move_vel);

        let player_uuid = self.player.gameprofile.id.to_string();
        let world_uuid = self.player.world().uuid.to_string();

        JvmEventPayload {
            event: Event {
                data: Some(Data::PlayerMove(PlayerMoveEvent {
                    player_uuid: Some(Uuid { value: player_uuid }),
                    from: Some(crate::proto::patchbukkit::common::Location {
                        world: Some(crate::proto::patchbukkit::common::World {
                            uuid: Some(Uuid {
                                value: world_uuid.clone(),
                            }),
                        }),
                        position: Some(crate::proto::patchbukkit::common::Vec3 {
                            x: self.from.x,
                            y: self.from.y,
                            z: self.from.z,
                        }),
                        yaw: self.player.living_entity.entity.yaw.load(),
                        pitch: self.player.living_entity.entity.pitch.load(),
                    }),
                    to: Some(crate::proto::patchbukkit::common::Location {
                        world: Some(crate::proto::patchbukkit::common::World {
                            uuid: Some(Uuid { value: world_uuid }),
                        }),
                        position: Some(crate::proto::patchbukkit::common::Vec3 {
                            x: self.to.x,
                            y: self.to.y,
                            z: self.to.z,
                        }),
                        yaw: self.player.living_entity.entity.yaw.load(),
                        pitch: self.player.living_entity.entity.pitch.load(),
                    }),
                })),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, data: Data) -> Option<()> {
        if let Data::PlayerMove(event) = data
            && let Some(to) = event.to
            && let Some(pos) = to.position
        {
            self.to = pumpkin_util::math::vector3::Vector3::new(pos.x, pos.y, pos.z);
        }
        Some(())
    }
}

impl PatchBukkitEvent for pumpkin::plugin::player::player_chat::PlayerChatEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::PlayerChat(PlayerChatEvent {
                    player_uuid: Some(Uuid {
                        value: self.player.gameprofile.id.to_string(),
                    }),
                    message: self.message.clone(),
                    format: "<%1$s> %2$s".to_string(),
                })),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, data: Data) -> Option<()> {
        if let Data::PlayerChat(event) = data {
            self.message = event.message;
        }
        Some(())
    }
}

impl PatchBukkitEvent
    for pumpkin::plugin::player::player_interact_entity_event::PlayerInteractEntityEvent
{
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        let action_str = match self.action {
            pumpkin_protocol::java::server::play::ActionType::Interact => "INTERACT",
            pumpkin_protocol::java::server::play::ActionType::Attack => "ATTACK",
            pumpkin_protocol::java::server::play::ActionType::InteractAt => "INTERACT_AT",
        };
        JvmEventPayload {
            event: Event {
                data: Some(Data::PlayerInteractEntity(PlayerInteractEntityEvent {
                    player_uuid: Some(Uuid {
                        value: self.player.gameprofile.id.to_string(),
                    }),
                    target_uuid: Some(Uuid {
                        value: self.target.get_entity().entity_uuid.to_string(),
                    }),
                    action: action_str.to_string(),
                    is_sneaking: self.sneaking,
                })),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

impl PatchBukkitEvent for pumpkin::plugin::block::sign_change::SignChangeEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::SignChange(SignChangeEvent {
                    player_uuid: Some(Uuid {
                        value: self.player.gameprofile.id.to_string(),
                    }),
                    block_x: self.block_pos.0.x,
                    block_y: self.block_pos.0.y,
                    block_z: self.block_pos.0.z,
                    lines: self.lines.clone(),
                })),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, data: Data) -> Option<()> {
        if let Data::SignChange(event) = data {
            self.lines = event.lines;
        }
        Some(())
    }
}

impl PatchBukkitEvent for pumpkin::plugin::block::block_damage::BlockDamageEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::BlockDamage(BlockDamageEvent {
                    player_uuid: Some(Uuid {
                        value: self.player.gameprofile.id.to_string(),
                    }),
                    block_x: self.block_pos.0.x,
                    block_y: self.block_pos.0.y,
                    block_z: self.block_pos.0.z,
                    insta_break: false,
                })),
            },
            context: EventContext {
                server,
                player: Some(self.player.clone()),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

impl PatchBukkitEvent for pumpkin::plugin::server::server_command::ServerCommandEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::ServerCommand(ServerCommandEvent {
                    sender_name: "Console".to_string(),
                    command: self.command.clone(),
                })),
            },
            context: EventContext {
                server,
                player: None,
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, data: Data) -> Option<()> {
        if let Data::ServerCommand(event) = data {
            self.command = event.command;
        }
        Some(())
    }
}

impl PatchBukkitEvent for pumpkin::plugin::block::block_ignite::BlockIgniteEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        let player_uuid = self.player.as_ref().map(|p| Uuid {
            value: p.gameprofile.id.to_string(),
        });
        JvmEventPayload {
            event: Event {
                data: Some(Data::BlockIgnite(BlockIgniteEvent {
                    player_uuid,
                    block_x: self.block_pos.0.x,
                    block_y: self.block_pos.0.y,
                    block_z: self.block_pos.0.z,
                    ignite_cause: "FLINT_AND_STEEL".to_string(),
                })),
            },
            context: EventContext {
                server,
                player: self.player.clone(),
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

impl PatchBukkitEvent for pumpkin::plugin::block::block_grow::BlockGrowEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::BlockGrow(BlockGrowEvent {
                    block_x: self.block_pos.0.x,
                    block_y: self.block_pos.0.y,
                    block_z: self.block_pos.0.z,
                    new_block_type: self.new_block.name.to_string(),
                })),
            },
            context: EventContext {
                server,
                player: None,
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

impl PatchBukkitEvent for pumpkin::plugin::block::block_form::BlockFormEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::BlockForm(BlockFormEvent {
                    block_x: self.block_pos.0.x,
                    block_y: self.block_pos.0.y,
                    block_z: self.block_pos.0.z,
                    new_block_type: self.block.name.to_string(),
                })),
            },
            context: EventContext {
                server,
                player: None,
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

impl PatchBukkitEvent for pumpkin::plugin::block::block_fade::BlockFadeEvent {
    fn to_payload(&self, server: Arc<Server>) -> JvmEventPayload {
        JvmEventPayload {
            event: Event {
                data: Some(Data::BlockFade(BlockFadeEvent {
                    block_x: self.block_pos.0.x,
                    block_y: self.block_pos.0.y,
                    block_z: self.block_pos.0.z,
                    new_block_type: self.block.name.to_string(),
                })),
            },
            context: EventContext {
                server,
                player: None,
            },
        }
    }

    fn apply_modifications(&mut self, _server: &Arc<Server>, _data: Data) -> Option<()> {
        Some(())
    }
}

pub struct PatchBukkitEventHandler<E: PatchBukkitEvent> {
    plugin_name: String,
    command_tx: mpsc::Sender<JvmCommand>,
    _phantom: PhantomData<E>,
}

impl<E: PatchBukkitEvent> PatchBukkitEventHandler<E> {
    #[must_use]
    pub const fn new(plugin_name: String, command_tx: mpsc::Sender<JvmCommand>) -> Self {
        Self {
            plugin_name,
            command_tx,
            _phantom: PhantomData,
        }
    }
}

impl<E> EventHandler<E> for PatchBukkitEventHandler<E>
where
    E: PatchBukkitEvent + Payload + Cancellable + 'static,
{
    fn handle<'a>(&'a self, server: &'a Arc<Server>, event: &'a E) -> BoxFuture<'a, ()> {
        let command_tx = self.command_tx.clone();
        let payload = event.to_payload(server.clone());
        if let Some(player) = &payload.context.player {
            crate::java::native_callbacks::utils::cache_player(player.clone());
        }

        Box::pin(async move {
            let (tx, rx) = oneshot::channel();
            if let Err(e) = command_tx
                .send(JvmCommand::FireEvent {
                    payload,
                    respond_to: tx,
                    plugin: self.plugin_name.clone(),
                })
                .await
            {
                tracing::error!("Failed to send event to JVM worker: {e}");
                return;
            }

            let _ = rx.await;
        })
    }

    fn handle_blocking<'a>(
        &'a self,
        server: &'a Arc<Server>,
        event: &'a mut E,
    ) -> BoxFuture<'a, ()> {
        let command_tx = self.command_tx.clone();
        let payload = event.to_payload(server.clone());
        if let Some(player) = &payload.context.player {
            crate::java::native_callbacks::utils::cache_player(player.clone());
        }

        Box::pin(async move {
            let (tx, rx) = oneshot::channel();
            if let Err(e) = command_tx
                .send(JvmCommand::FireEvent {
                    payload,
                    respond_to: tx,
                    plugin: self.plugin_name.clone(),
                })
                .await
            {
                tracing::error!("Failed to send event to JVM worker: {e}");
                return;
            }

            match rx.await {
                Ok(response) => {
                    event.set_cancelled(response.cancelled);
                    if let Some(event_data) = response.data.and_then(|d| d.data) {
                        let _ = event.apply_modifications(server, event_data);
                    }
                }
                Err(_) => {
                    tracing::warn!("JVM worker dropped response channel for event");
                }
            }
        })
    }
}
