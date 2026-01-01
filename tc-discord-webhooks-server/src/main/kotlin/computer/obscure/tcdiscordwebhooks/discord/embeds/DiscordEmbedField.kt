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
 * Fields that can be added to an [DiscordEmbed]
 * to display embedded text
 * 
 * @author Pascal Zarrad
 */
class DiscordEmbedField {
    /**
     * The name of the embed field
     */
    var name: String? = null

    /**
     * The value of the embed field
     */
    var value: String? = null

    /**
     * Decides whether the field should be displayed inline or not
     */
    var isInline: Boolean = false

    constructor(name: String?, value: String?) {
        this.name = name
        this.value = value
    }

    constructor(name: String?, value: String?, inline: Boolean) {
        this.name = name
        this.value = value
        this.isInline = inline
    }

    constructor()
}
