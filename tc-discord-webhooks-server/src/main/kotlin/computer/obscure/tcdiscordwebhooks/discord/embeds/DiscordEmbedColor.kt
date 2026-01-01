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
 * Provides the basic colors that can be used for embeds.
 * The values of the colors are their decimal value.
 * 
 * 
 * Also this class provides a method to get the decimal value of a
 * 
 * @author Pascal Zarrad
 */
object DiscordEmbedColor {
    /**
     * Red color
     */
    const val RED: Int = 16711680

    /**
     * Blue color
     */
    const val BLUE: Int = 26367

    /**
     * Green color
     */
    const val GREEN: Int = 510208

    /**
     * Yellow color
     */
    const val YELLOW: Int = 15924992

    /**
     * Orange color
     */
    const val ORANGE: Int = 16746496

    /**
     * Converts a hexadecimal color code to a decimal color code.
     * This method supports hexadecimal strings as parameter with and without a leading #.
     * 
     * @param hexCode The hexadecimal color code to convert to a decimal value
     * @return The decimal value
     */
    @Throws(NumberFormatException::class)
    fun convertHexToDecColor(hexCode: String): Int {
        if (hexCode.startsWith("#")) {
            val pureHex: String = hexCode.substring(1)
            return pureHex.toInt(16)
        } else {
            return hexCode.toInt(16)
        }
    }
}
