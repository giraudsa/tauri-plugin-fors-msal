package app.fors.msal

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest

@InvokeArg
class InitArgs {
    lateinit var clientId: String
    var tenantId: String = "37e2c3f8-d936-4d6a-af0f-922879a4b5de"
    var scopes: List<String> = listOf("User.Read")
}

@TauriPlugin
class ForsMsalPlugin(private val activity: Activity) : Plugin(activity) {
    private var singleAccountApp: ISingleAccountPublicClientApplication? = null
    private var scopes: Array<String> = arrayOf("User.Read")

    private fun signingCertificateSha1Base64(): String {
        val pm = activity.packageManager
        val pkg = activity.packageName
        val certBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val packageInfo = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
            packageInfo.signingInfo?.apkContentsSigners?.get(0)?.toByteArray()
        } else {
            @Suppress("DEPRECATION")
            val packageInfo = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            packageInfo.signatures?.get(0)?.toByteArray()
        } ?: throw IllegalStateException("No signing certificate found")
        val digest = MessageDigest.getInstance("SHA-1").digest(certBytes)
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private fun redirectUri(): String {
        val hash = URLEncoder.encode(signingCertificateSha1Base64(), "UTF-8")
        return "msauth://${activity.packageName}/$hash"
    }

    private fun writeAuthConfig(args: InitArgs): File {
        val authorities = JSONArray().put(
            JSONObject()
                .put("type", "AAD")
                .put(
                    "audience",
                    JSONObject()
                        .put("type", "AzureADMyOrg")
                        .put("tenant_id", args.tenantId)
                )
                .put("default", true)
        )
        val config = JSONObject()
            .put("client_id", args.clientId)
            .put("redirect_uri", redirectUri())
            .put("account_mode", "SINGLE")
            .put("authorization_user_agent", "DEFAULT")
            .put("broker_redirect_uri_registered", false)
            .put("authorities", authorities)
        val file = File(activity.cacheDir, "fors_msal_config.json")
        file.writeText(config.toString())
        return file
    }

    private fun authResultToJs(result: IAuthenticationResult): JSObject {
        val ret = JSObject()
        val idToken = result.account.idToken
        val accessToken = result.accessToken
        ret.put("idToken", idToken)
        ret.put("accessToken", accessToken)
        ret.put("token", idToken ?: accessToken)
        return ret
    }

    @Command
    fun init(invoke: Invoke) {
        try {
            val args = invoke.parseArgs(InitArgs::class.java)
            scopes = args.scopes.toTypedArray()
            val configFile = writeAuthConfig(args)
            PublicClientApplication.createSingleAccountPublicClientApplication(
                activity.applicationContext,
                configFile,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        singleAccountApp = application
                        invoke.resolve()
                    }

                    override fun onError(exception: MsalException) {
                        invoke.reject(exception.message ?: "MSAL init failed")
                    }
                }
            )
        } catch (ex: Exception) {
            invoke.reject(ex.message ?: "MSAL init error")
        }
    }

    @Command
    fun signInSilent(invoke: Invoke) {
        val app = singleAccountApp
        if (app == null) {
            invoke.reject("MSAL not initialized")
            return
        }
        val authority = app.configuration.defaultAuthority.authorityURL.toString()
        app.acquireTokenSilentAsync(
            scopes,
            authority,
            object : SilentAuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    invoke.resolve(authResultToJs(authenticationResult))
                }

                override fun onError(exception: MsalException) {
                    invoke.reject(exception.message ?: "Silent sign-in failed")
                }
            }
        )
    }

    @Command
    fun signInInteractive(invoke: Invoke) {
        val app = singleAccountApp
        if (app == null) {
            invoke.reject("MSAL not initialized")
            return
        }
        app.signIn(
            activity,
            null,
            scopes,
            object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    invoke.resolve(authResultToJs(authenticationResult))
                }

                override fun onError(exception: MsalException) {
                    invoke.reject(exception.message ?: "Interactive sign-in failed")
                }

                override fun onCancel() {
                    invoke.reject("User cancelled")
                }
            }
        )
    }

    @Command
    fun signOut(invoke: Invoke) {
        val app = singleAccountApp
        if (app == null) {
            invoke.resolve()
            return
        }
        app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                invoke.resolve()
            }

            override fun onError(exception: MsalException) {
                invoke.reject(exception.message ?: "Sign-out failed")
            }
        })
    }
}