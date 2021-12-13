package cloudburst.plugins.sendembedsextra

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin

import com.aliucord.PluginManager
import com.aliucord.utils.ReflectUtils
import com.aliucord.Utils
import java.util.HashMap
import cloudburst.plugins.sendembedsextra.utils.*
import com.discord.models.domain.NonceGenerator
import com.discord.utilities.time.ClockFactory
import com.discord.restapi.RestAPIParams
import com.aliucord.Http
import com.discord.utilities.rest.RestAPI
import com.discord.utilities.analytics.AnalyticSuperProperties
import com.discord.stores.StoreStream
import java.net.URLEncoder
import com.aliucord.utils.RxUtils.createActionSubscriber
import com.aliucord.utils.RxUtils.subscribe
import com.jaredrummler.android.colorpicker.ColorPickerDialog

@AliucordPlugin
class SendEmbedsExtra : Plugin() {
    lateinit var modes : MutableList<String>
    lateinit var extraFunctions: HashMap<String, (Long,   String, String, String,  String, String,   String) -> Unit>

    override fun start(context: Context) {
        val sendEmbeds = PluginManager.plugins.get("SendEmbeds")
        if (sendEmbeds == null)
            return
        
        modes = ReflectUtils.getField(sendEmbeds, "modes") as MutableList<String>
        modes.add("selfbot")
        modes.add("hidden link")
        
        extraFunctions = ReflectUtils.getField(sendEmbeds, "extraFunctions") as HashMap<String, (Long,   String, String, String,  String, String,   String) -> Unit>
        extraFunctions.put("selfbot", ::sendSelfBotEmbed)
        extraFunctions.put("hidden link", ::sendHiddenLinkEmbed)
    }

    override fun stop(context: Context) {
        modes.remove("selfbot")
        extraFunctions.remove("selfbot")

        modes.remove("hidden link")
        extraFunctions.remove("hidden link")
    }

    private fun sendSelfBotEmbed(channelId: Long, author: String, title: String, content: String, url: String, imageUrl: String, color: String) {
        try {
            val msg = Message(
                null,
                false,
                NonceGenerator.computeNonce(ClockFactory.get()).toString(),
                Embed(
                    Author(author),
                    title, 
                    content,
                    url,
                    EmbedImage(imageUrl),
                    toColorInt(color)
                )
            )
            Http.Request("https://discord.com/api/v9/channels/%d/messages".format(channelId), "POST")
                .setHeader("Authorization", ReflectUtils.getField(StoreStream.getAuthentication(), "authToken") as String?)
                .setHeader("User-Agent", RestAPI.AppHeadersProvider.INSTANCE.userAgent)
                .setHeader("X-Super-Properties", AnalyticSuperProperties.INSTANCE.superPropertiesStringBase64)
                .setHeader("Accept", "*/*")
                .executeWithJson(msg)
            .text()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun sendHiddenLinkEmbed(channelId: Long, author: String, title: String, content: String, url: String, imageUrl: String, color: String) {
        val msg = "|".repeat(995) + "https://embed.rauf.workers.dev/?author=%s&title=%s&description=%s&color=%s&image=%s&redirect=%s".format(URLEncoder.encode(author, "utf-8"), URLEncoder.encode(title, "utf-8"), URLEncoder.encode(content, "utf-8"), color.replace("#", ""), URLEncoder.encode(imageUrl, "utf-8"), URLEncoder.encode(url, "utf-8"))
        val message = RestAPIParams.Message(
            msg,
            NonceGenerator.computeNonce(ClockFactory.get()).toString(),
            null,
            null,
            emptyList(),
            null,
            RestAPIParams.Message.AllowedMentions(
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    false
            ),
            null
        )
        RestAPI.api.sendMessage(channelId, message).subscribe(createActionSubscriber({ }))
    }

    private fun toColorInt(a: String): Int {
        try {
            return a
                .replace("#", "")
                .toInt(16)
        } catch(e:Throwable) {
            Utils.showToast("Color parser error: %s".format(e.message))
            e.printStackTrace()
        }
        return 0
    }
}
