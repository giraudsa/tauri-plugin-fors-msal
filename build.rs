const COMMANDS: &[&str] = &["init", "sign_in_silent", "sign_in_interactive", "sign_out"];

fn main() {
    tauri_plugin::Builder::new(COMMANDS)
        .android_path("android")
        .build();
}