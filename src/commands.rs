use serde::Deserialize;
use tauri::{command, AppHandle, Runtime};

use crate::{models::*, ForsMsalExt};

#[derive(Debug, Deserialize)]
pub struct InitArgs {
    pub client_id: String,
    #[serde(default = "default_tenant")]
    pub tenant_id: String,
    #[serde(default = "default_scopes")]
    pub scopes: Vec<String>,
}

fn default_tenant() -> String {
    "37e2c3f8-d936-4d6a-af0f-922879a4b5de".into()
}

fn default_scopes() -> Vec<String> {
    vec!["User.Read".into()]
}

#[command]
pub async fn init<R: Runtime>(app: AppHandle<R>, args: InitArgs) -> crate::Result<()> {
    app.fors_msal().init(args).await
}

#[command]
pub async fn sign_in_silent<R: Runtime>(app: AppHandle<R>) -> crate::Result<AuthResult> {
    app.fors_msal().sign_in_silent().await
}

#[command]
pub async fn sign_in_interactive<R: Runtime>(app: AppHandle<R>) -> crate::Result<AuthResult> {
    app.fors_msal().sign_in_interactive().await
}

#[command]
pub async fn sign_out<R: Runtime>(app: AppHandle<R>) -> crate::Result<()> {
    app.fors_msal().sign_out().await
}