use crate::java::native_callbacks::CALLBACK_CONTEXT;
use crate::proto::patchbukkit::{
    common::Uuid,
    inventory::{GetInventoryRequest, GetInventoryResponse, SetInventorySlotRequest, SetInventorySlotResponse},
};

pub fn ffi_native_bridge_get_inventory_impl(
    request: GetInventoryRequest,
) -> Option<GetInventoryResponse> {
    let _ctx = CALLBACK_CONTEXT.get()?;
    let _player_uuid = uuid::Uuid::parse_str(&request.player_uuid?.value).ok()?;

    Some(GetInventoryResponse {
        slots: vec![],
        size: 36,
    })
}

pub fn ffi_native_bridge_set_inventory_slot_impl(
    request: SetInventorySlotRequest,
) -> Option<SetInventorySlotResponse> {
    let _ctx = CALLBACK_CONTEXT.get()?;
    let _player_uuid = uuid::Uuid::parse_str(&request.player_uuid?.value).ok()?;

    Some(SetInventorySlotResponse { success: false })
}

pub fn ffi_native_bridge_get_player_address_impl(
    _request: Uuid,
) -> Option<crate::proto::patchbukkit::bridge::GetPlayerAddressResponse> {
    None
}
