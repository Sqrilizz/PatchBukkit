use std::{
    collections::HashSet,
    fs,
    path::{Path, PathBuf},
};

use anyhow::Result;
use rust_embed::Embed;

#[derive(Embed)]
#[folder = "resources/"]
pub struct Resources;

pub fn setup_j4rs(j4rs_folder: &Path) -> Result<()> {
    sync_embedded_resources(j4rs_folder)?;
    cleanup_stale_files(j4rs_folder);

    Ok(())
}

fn cleanup_stale_files(j4rs_folder: &Path) {
    let embedded_paths: HashSet<PathBuf> = Resources::iter()
        .map(|p| PathBuf::from(p.to_string()))
        .collect();

    for entry in walkdir::WalkDir::new(j4rs_folder)
        .into_iter()
        .filter_map(|e| e.ok())
        .filter(|e| e.file_type().is_file())
    {
        let path = entry.path();
        if let Ok(rel_path) = path.strip_prefix(j4rs_folder)
            && !embedded_paths.contains(rel_path)
        {
            tracing::warn!("Removing stale embedded file: {}", rel_path.display());
            let _ = fs::remove_file(path);
        }
    }
}

pub fn sync_embedded_resources(j4rs_folder: &Path) -> Result<()> {
    for resource_path_str in Resources::iter() {
        let resource_path = j4rs_folder.join(resource_path_str.to_string());
        let resource = Resources::get(&resource_path_str).unwrap();

        if resource_path.exists() {
            update_resource_if_changed(&resource_path, &resource.data)?;
        } else {
            tracing::info!("Extracting new resource: {}", resource_path.display());
            write_resource(&resource_path, &resource.data)?;
        }
    }
    Ok(())
}

fn write_resource(path: &Path, data: &[u8]) -> Result<()> {
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)?;
    }

    fs::write(path, data)?;
    Ok(())
}

fn update_resource_if_changed(path: &Path, new_data: &[u8]) -> Result<()> {
    // Quick check: If file sizes differ, it's definitely changed, TODO: use Hash ?
    let metadata = fs::metadata(path).ok();
    let size_matches = metadata.is_some_and(|m| m.len() == new_data.len() as u64);

    if !size_matches || fs::read(path)? != new_data {
        tracing::debug!("Updating changed resource: {}", path.display());
        fs::write(path, new_data)?;
    }

    Ok(())
}
