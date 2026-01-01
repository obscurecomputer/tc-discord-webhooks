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
package computer.obscure.tcdiscordwebhooks.discord.embeds

/**
 * A [DiscordEmbed] represents a Java object that contains every attribute
 * that can be applied to an embedded Discord message.
 * 
 * 
 * NOTE: As embeds produced by this application are meant to be used for WebHooks,
 * the attribute type is always set to "rich".
 * Some attributes like video are not available because they are not
 * supported. So implementing them does not make any sense.
 * 
 * 
 * The documentation about Discord's embeds can be found here:
 * https://discordapp.com/developers/docs/resources/webhook
 * 
 * @author Pascal Zarrad
 */
class DiscordEmbed {
    /**
     * The title that of the embed
     */
    var title: String? = null

    /**
     * The description of the embed
     */
    var description: String? = null

    /**
     * The URL of the embed
     */
    var url: String? = null

    /**
     * The color that the embed should have
     */
    var color: Int = 0

    /**
     * The footer to use for the embed
     */
    var footer: DiscordEmbedFooter? = null

    /**
     * The image to use for the embed
     */
    var image: DiscordEmbedImage? = null

    /**
     * The thumbnail to set for the embed.
     * NOTE: The thumbnail embed object has the same properties as
     * an image embed, so they share a class.
     */
    var thumbnail: DiscordEmbedImage? = null

    /**
     * Contains additional fields of text for the embed
     */
    var fields: Array<DiscordEmbedField?>? = null

    constructor(
        title: String?,
        description: String?,
        url: String?,
        color: Int,
        footer: DiscordEmbedFooter?,
        image: DiscordEmbedImage?,
        thumbnail: DiscordEmbedImage?,
        fields: Array<DiscordEmbedField?>?
    ) {
        this.title = title
        this.description = description
        this.url = url
        this.color = color
        this.footer = footer
        this.image = image
        this.thumbnail = thumbnail
        this.fields = fields
    }

    constructor(
        title: String?,
        description: String?,
        url: String?,
        color: Int,
        footer: DiscordEmbedFooter?,
        thumbnail: DiscordEmbedImage?,
        fields: Array<DiscordEmbedField?>?
    ) {
        this.title = title
        this.description = description
        this.url = url
        this.color = color
        this.footer = footer
        this.thumbnail = thumbnail
        this.fields = fields
    }

    constructor(
        title: String?,
        description: String?,
        url: String?,
        color: Int,
        footer: DiscordEmbedFooter?,
        fields: Array<DiscordEmbedField?>?
    ) {
        this.title = title
        this.description = description
        this.url = url
        this.color = color
        this.footer = footer
        this.fields = fields
    }

    constructor()
}
