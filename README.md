# tauri-plugin-fors-msal

[Tauri v2](https://v2.tauri.app/) plugin for Azure AD single sign-on on Android, using the native [MSAL Android SDK](https://github.com/AzureAD/microsoft-authentication-library-for-android) (5.3.0).

**Platform:** Android only (API 24+).

## Install

```toml
# src-tauri/Cargo.toml
[dependencies]
tauri-plugin-fors-msal = { git = "https://github.com/giraudsa/tauri-plugin-fors-msal", tag = "v0.1.0" }
```

```rust
// src-tauri/src/lib.rs
tauri::Builder::default()
    .plugin(tauri_plugin_fors_msal::init())
```

## Commands

| Command | Description |
|---------|-------------|
| `init` | Configure client ID, tenant ID and OAuth scopes |
| `sign_in_silent` | Acquire token from cache or refresh token |
| `sign_in_interactive` | Interactive browser / broker sign-in |
| `sign_out` | Remove account from device cache |

### JavaScript

```js
import { invoke } from '@tauri-apps/api/core';

await invoke('plugin:fors-msal|init', {
  clientId: '<azure-client-id>',
  tenantId: '<tenant-id>',
  scopes: ['User.Read'],
});

const auth = await invoke('plugin:fors-msal|sign_in_interactive');
// auth: { idToken, accessToken, token }
```

## Azure AD setup

Register a **mobile and desktop** redirect URI for each signed APK:

```
msauth://<application-id>/<url-encoded-signature-hash>
```

The signature hash must match the keystore used to sign the APK. URL-encode special characters (`=` → `%3D`, `/` → `%2F`).

This plugin ships `consumer-rules.pro` for R8/ProGuard to keep MSAL classes.

## License

MIT OR Apache-2.0
