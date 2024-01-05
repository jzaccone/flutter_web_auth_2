package com.linusu.flutter_web_auth_2

import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.browser.customtabs.CustomTabsIntent

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class FlutterWebAuth2Plugin(private var context: Context? = null, private var channel: MethodChannel? = null): MethodCallHandler, FlutterPlugin {
  companion object {
    val callbacks = mutableMapOf<String, Result>()

    @JvmStatic
    fun registerWith(registrar: Registrar) {
        val plugin = FlutterWebAuth2Plugin()
        plugin.initInstance(registrar.messenger(), registrar.context())
    }

  }

  fun initInstance(messenger: BinaryMessenger, context: Context) {
      this.context = context
      channel = MethodChannel(messenger, "flutter_web_auth_2")
      channel?.setMethodCallHandler(this)
  }

  override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
      initInstance(binding.binaryMessenger, binding.applicationContext)
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
      context = null
      channel = null
  }

  override fun onMethodCall(call: MethodCall, resultCallback: Result) {
    when (call.method) {
        "authenticate" -> {
          val url = Uri.parse(call.argument("url"))
          val callbackUrlScheme = call.argument<String>("callbackUrlScheme")!!
          val options = call.argument<Map<String, Any>>("options")!!

          callbacks[callbackUrlScheme] = resultCallback

          val intent = CustomTabsIntent.Builder().build()
          val keepAliveIntent = Intent(context, KeepAliveService::class.java)

          intent.intent.addFlags(options["intentFlags"] as Int)
          intent.intent.putExtra("android.support.customtabs.extra.KEEP_ALIVE", keepAliveIntent)
          intent.intent.putExtra("android.support.customtabs.extra.user_opt_out", true);

          intent.launchUrl(context!!, url)
        }
        "cleanUpDanglingCalls" -> {
          callbacks.forEach{ (_, danglingResultCallback) ->
              danglingResultCallback.error("CANCELED", "User canceled login", null)
          }
          callbacks.clear()
          resultCallback.success(null)
        }
        else -> resultCallback.notImplemented()
    }
  }
}
