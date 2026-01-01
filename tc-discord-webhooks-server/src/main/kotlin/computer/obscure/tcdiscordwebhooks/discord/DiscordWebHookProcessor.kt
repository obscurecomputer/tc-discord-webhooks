/*
 * Copyright 2021 Pascal Zarrad
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package computer.obscure.tcdiscordwebhooks.discord

import com.google.gson.Gson
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Handles the communication between the Discord WebHook API and the TeamCity server.
 * Also handles the serialization of the Discord WebHook Payloads
 * 
 * @author Pascal Zarrad
 */
class DiscordWebHookProcessor {
    /**
     * The GSON instance used to serialize the [DiscordWebHookPayload]'s
     */
    val gSON: Gson

    init {
        this.gSON = Gson()
    }

    /**
     * Send a WebHook request to the Discord API.
     * This method accepts a [DiscordWebHookPayload] as argument and serialises it before sending it.
     * 
     * @param webHookURL            The URL of the WebHook that is targeted
     * @param discordWebHookPayload The payload which contains the content to send
     * @return true if the request succeeded
     * @throws IOException        Thrown when any I/O operation fails
     * @throws URISyntaxException Thrown when the given #webHookURL is invalid
     * @see DiscordWebHookProcessor.sendDiscordWebHook
     */
    @Throws(IOException::class, URISyntaxException::class)
    fun sendDiscordWebHook(webHookURL: String, discordWebHookPayload: DiscordWebHookPayload?): Boolean {
        return this.sendDiscordWebHook(webHookURL, this.serializeDiscordWebHookPayload(discordWebHookPayload)!!)
    }

    /**
     * Send a WebHook request to the Discord API
     * 
     * @param webHookURL            The URL of the WebHook that is targeted
     * @param discordWebHookPayload The payload which contains the content to send
     * @return true if the request succeeded
     * @throws IOException        Thrown when any I/O operation fails
     * @throws URISyntaxException Thrown when the given #webHookURL is invalid
     */
    @Throws(IOException::class, URISyntaxException::class)
    fun sendDiscordWebHook(webHookURL: String, discordWebHookPayload: String): Boolean {
        // Send Discord WebHook
        val url = URL(webHookURL)
        val responseCode: Int // We default to 400, when request succeeded, this should be 204
        HttpClients.createDefault().use { httpClient ->
            val httpPost = HttpPost(url.toURI())
            httpPost.addHeader("User-Agent", "TeamCity Discord WebHook v1")
            httpPost.addHeader("Accept-Language", "en-US,en;q=0.5")
            httpPost.addHeader("Content-Type", "application/json")
            httpPost.setEntity(StringEntity(discordWebHookPayload, HTTP_CHARSET))
            val httpProxyHost = System.getProperty("http.proxyHost")
            val httpProxyPort = System.getProperty("http.proxyPort")
            if (httpProxyHost != null && httpProxyPort.trim { it <= ' ' }.length > 0 && httpProxyPort != null) {
                val port: Int = httpProxyPort.toInt()
                val proxy = HttpHost(httpProxyHost, port, "http")
                var reqConfigBuilder = RequestConfig.custom()
                reqConfigBuilder = reqConfigBuilder.setProxy(proxy)
                val config = reqConfigBuilder.build()
                httpPost.setConfig(config)
            }
            httpClient.execute(httpPost).use { response ->
                responseCode = response.getStatusLine().getStatusCode()
            }
        }
        return responseCode == 204 // When request returned status 204, the request was a success
    }

    /**
     * Serializes a [DiscordWebHookPayload] into a JSON string.
     * 
     * @param discordWebHookPayload The payload the serialize
     * @return The JSOn string of the [DiscordWebHookPayload]
     */
    fun serializeDiscordWebHookPayload(discordWebHookPayload: DiscordWebHookPayload?): String? {
        return this.gSON.toJson(discordWebHookPayload)
    }

    companion object {
        /**
         * The charset used for the requests
         */
        private val HTTP_CHARSET: String? = StandardCharsets.UTF_8.toString()
    }
}
