use pumpkin::entity::player::TitleMode;
use pumpkin_protocol::java::client::play::{CAddResourcePack, CClearTitle, CRemoveResourcePack};
use pumpkin_util::text::TextComponent;

use crate::{
    java::native_callbacks::CALLBACK_CONTEXT,
    proto::patchbukkit::{
        common::Uuid,
        player::{
            ClearPlayerTitleRequest, PlayerTitleMode, RemoveResourcePackRequest,
            SendPlayerTitleRequest, SendResourcePackRequest, SetPlayerTitleTimesRequest,
        },
    },
};

fn component(json: &str) -> TextComponent {
    serde_json::from_str(json).unwrap_or_else(|_| TextComponent::text(json.to_string()))
}

fn player(uuid: Option<&Uuid>) -> Option<std::sync::Arc<pumpkin::entity::player::Player>> {
    let ctx = CALLBACK_CONTEXT.get()?;
    ctx.plugin_context
        .server
        .get_player_by_uuid(uuid::Uuid::parse_str(&uuid?.value).ok()?)
}

pub fn ffi_native_bridge_send_player_title_impl(request: SendPlayerTitleRequest) -> Option<()> {
    let player = player(request.player_uuid.as_ref())?;
    let component = component(&request.component_json);
    let mode = match PlayerTitleMode::try_from(request.mode).ok()? {
        PlayerTitleMode::Title => TitleMode::Title,
        PlayerTitleMode::Subtitle => TitleMode::SubTitle,
        PlayerTitleMode::ActionBar => TitleMode::ActionBar,
    };
    CALLBACK_CONTEXT.get()?.runtime.spawn(async move {
        player.show_title(&component, &mode).await;
    });
    Some(())
}

pub fn ffi_native_bridge_set_player_title_times_impl(
    request: SetPlayerTitleTimesRequest,
) -> Option<()> {
    let player = player(request.player_uuid.as_ref())?;
    CALLBACK_CONTEXT.get()?.runtime.spawn(async move {
        player
            .send_title_animation(request.fade_in, request.stay, request.fade_out)
            .await;
    });
    Some(())
}

pub fn ffi_native_bridge_clear_player_title_impl(request: ClearPlayerTitleRequest) -> Option<()> {
    let player = player(request.player_uuid.as_ref())?;
    CALLBACK_CONTEXT.get()?.runtime.spawn(async move {
        player
            .send_client_packet(&CClearTitle::new(request.reset))
            .await;
    });
    Some(())
}

pub fn ffi_native_bridge_send_resource_pack_impl(request: SendResourcePackRequest) -> Option<()> {
    let player = player(request.player_uuid.as_ref())?;
    let pack_uuid = uuid::Uuid::parse_str(&request.pack_uuid?.value).ok()?;
    let prompt = (!request.prompt_json.is_empty()).then(|| component(&request.prompt_json));
    CALLBACK_CONTEXT.get()?.runtime.spawn(async move {
        player
            .send_client_packet(&CAddResourcePack::new(
                &pack_uuid,
                &request.url,
                &request.hash,
                request.force,
                prompt,
            ))
            .await;
    });
    Some(())
}

pub fn ffi_native_bridge_remove_resource_pack_impl(
    request: RemoveResourcePackRequest,
) -> Option<()> {
    let player = player(request.player_uuid.as_ref())?;
    let pack_uuid = uuid::Uuid::parse_str(&request.pack_uuid?.value).ok()?;
    CALLBACK_CONTEXT.get()?.runtime.spawn(async move {
        player
            .send_client_packet(&CRemoveResourcePack::new(Some(&pack_uuid)))
            .await;
    });
    Some(())
}

pub fn ffi_native_bridge_clear_resource_packs_impl(request: Uuid) -> Option<()> {
    let player = player(Some(&request))?;
    CALLBACK_CONTEXT.get()?.runtime.spawn(async move {
        player
            .send_client_packet(&CRemoveResourcePack::new(None))
            .await;
    });
    Some(())
}
