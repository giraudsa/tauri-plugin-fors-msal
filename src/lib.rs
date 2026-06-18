use tauri::{
    plugin::{PluginHandle, TauriPlugin},
    Manager, Runtime,
};

mod commands;
mod error;
mod models;

pub use error::{Error, Result};
pub use models::AuthResult;

#[cfg(target_os = "android")]
const PLUGIN_IDENTIFIER: &str = "app.fors.msal";

pub struct ForsMsal<R: Runtime> {
    handle: PluginHandle<R>,
    scopes: std::sync::Mutex<Vec<String>>,
}

impl<R: Runtime> ForsMsal<R> {
    pub async fn init(&self, args: commands::InitArgs) -> Result<()> {
        *self.scopes.lock().unwrap() = args.scopes.clone();
        self.handle
            .run_mobile_plugin(
                "init",
                models::InitPayload {
                    client_id: args.client_id,
                    tenant_id: args.tenant_id,
                    scopes: args.scopes,
                },
            )
            .map_err(Into::into)
    }

    pub async fn sign_in_silent(&self) -> Result<AuthResult> {
        self.handle
            .run_mobile_plugin("signInSilent", ())
            .map_err(Into::into)
    }

    pub async fn sign_in_interactive(&self) -> Result<AuthResult> {
        self.handle
            .run_mobile_plugin("signInInteractive", ())
            .map_err(Into::into)
    }

    pub async fn sign_out(&self) -> Result<()> {
        self.handle
            .run_mobile_plugin::<()>("signOut", ())
            .map_err(Into::into)
    }
}

pub trait ForsMsalExt<R: Runtime> {
    fn fors_msal(&self) -> &ForsMsal<R>;
}

impl<R: Runtime, T: Manager<R>> ForsMsalExt<R> for T {
    fn fors_msal(&self) -> &ForsMsal<R> {
        self.state::<ForsMsal<R>>().inner()
    }
}

pub fn init<R: Runtime>() -> TauriPlugin<R> {
    tauri::plugin::Builder::new("fors-msal")
        .invoke_handler(tauri::generate_handler![
            commands::init,
            commands::sign_in_silent,
            commands::sign_in_interactive,
            commands::sign_out,
        ])
        .setup(|app, api| {
            #[cfg(target_os = "android")]
            {
                let handle = api.register_android_plugin(PLUGIN_IDENTIFIER, "ForsMsalPlugin")?;
                app.manage(ForsMsal {
                    handle,
                    scopes: std::sync::Mutex::new(vec!["User.Read".into()]),
                });
            }
            Ok(())
        })
        .build()
}