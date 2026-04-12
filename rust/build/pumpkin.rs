use std::{fs, path::Path};

fn extract_git_revision(source: &str) -> Option<String> {
    source.rsplit('#').next().and_then(|revision| {
        let trimmed = revision.trim();
        if trimmed.is_empty() {
            None
        } else {
            Some(trimmed.to_string())
        }
    })
}

pub fn setup_pumpkin_build_info(base: &Path) {
    let lock_path = base.join("Cargo.lock");
    let lock_contents =
        fs::read_to_string(&lock_path).expect("Failed to read Cargo.lock for Pumpkin build info");

    let mut in_pumpkin_package = false;
    let mut pumpkin_version: Option<String> = None;
    let mut pumpkin_revision: Option<String> = None;

    for line in lock_contents.lines() {
        let trimmed = line.trim();

        if trimmed == "[[package]]" {
            if in_pumpkin_package {
                break;
            }
            in_pumpkin_package = false;
            pumpkin_version = None;
            pumpkin_revision = None;
            continue;
        }

        if trimmed == r#"name = "pumpkin""# {
            in_pumpkin_package = true;
            continue;
        }

        if !in_pumpkin_package {
            continue;
        }

        if let Some(version) = trimmed.strip_prefix(r#"version = ""#) {
            pumpkin_version = Some(version.trim_end_matches('"').to_string());
            continue;
        }

        if let Some(source) = trimmed.strip_prefix(r#"source = ""#) {
            let source = source.trim_end_matches('"');
            pumpkin_revision = extract_git_revision(source);
        }
    }

    let pumpkin_version = pumpkin_version.unwrap_or_else(|| "unknown".to_string());
    let pumpkin_revision = pumpkin_revision.unwrap_or_else(|| "unknown".to_string());
    let pumpkin_revision_short = pumpkin_revision.chars().take(7).collect::<String>();

    println!("cargo::rustc-env=PATCHBUKKIT_PUMPKIN_VERSION={pumpkin_version}");
    println!("cargo::rustc-env=PATCHBUKKIT_PUMPKIN_REVISION={pumpkin_revision}");
    println!("cargo::rustc-env=PATCHBUKKIT_PUMPKIN_REVISION_SHORT={pumpkin_revision_short}");
    println!("cargo::rerun-if-changed={}", lock_path.display());
}
