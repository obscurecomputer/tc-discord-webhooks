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
 * An element to set the author of an [DiscordEmbed].
 * 
 * @author Pascal Zarrad
 */
class DiscordAuthorEmbed {
    /**
     * The name of the author
     */
    var name: String? = null

    /**
     * The url of the author
     */
    var url: String? = null

    /**
     * The url to the icon of the author
     */
    var icon_url: String? = null

    constructor(name: String?, url: String?, icon_url: String?) {
        this.name = name
        this.url = url
        this.icon_url = icon_url
    }

    constructor(name: String?, url: String?) {
        this.name = name
        this.url = url
    }

    constructor(name: String?) {
        this.name = name
    }

    constructor()
}
