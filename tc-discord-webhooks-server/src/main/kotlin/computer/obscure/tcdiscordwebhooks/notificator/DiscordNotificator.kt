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
package computer.obscure.tcdiscordwebhooks.notificator

import computer.obscure.tcdiscordwebhooks.discord.DiscordWebHookPayload
import computer.obscure.tcdiscordwebhooks.discord.DiscordWebHookProcessor
import computer.obscure.tcdiscordwebhooks.discord.embeds.DiscordEmbed
import computer.obscure.tcdiscordwebhooks.discord.embeds.DiscordEmbedColor
import computer.obscure.tcdiscordwebhooks.discord.embeds.DiscordEmbedField
import jetbrains.buildServer.Build
import jetbrains.buildServer.notification.Notificator
import jetbrains.buildServer.notification.NotificatorRegistry
import jetbrains.buildServer.responsibility.ResponsibilityEntry
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.serverSide.mute.MuteInfo
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo
import jetbrains.buildServer.tests.TestName
import jetbrains.buildServer.users.NotificatorPropertyKey
import jetbrains.buildServer.users.PropertyKey
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.vcs.VcsRoot
import org.apache.log4j.Logger
import java.io.IOException
import java.net.URISyntaxException
import kotlin.collections.ArrayList
import kotlin.collections.MutableCollection
import kotlin.collections.MutableList
import kotlin.collections.MutableSet

/**
 * The [Notificator] service that handles triggered notifications
 * 
 * @author Pascal Zarrad
 */
class DiscordNotificator(notificatorRegistry: NotificatorRegistry, sBuildServer: SBuildServer) : Notificator {
    /**
     * The [DiscordWebHookProcessor] that is used to trigger the WebHooks
     */
    private val discordWebHookProcessor: DiscordWebHookProcessor

    /**
     * The [SBuildServer] this [Notificator] belongs to
     */
    private val sBuildServer: SBuildServer

    init {
        this.discordWebHookProcessor = DiscordWebHookProcessor()
        this.sBuildServer = sBuildServer
        this.initializeNotificator(notificatorRegistry)
    }

    /**
     * Creates all the [UserPropertyInfo]'s  and registers this [Notificator]
     * 
     * @param notificatorRegistry The [NotificatorRegistry] where this [Notificator] will be registered to
     */
    private fun initializeNotificator(notificatorRegistry: NotificatorRegistry) {
        val userProperties = ArrayList<UserPropertyInfo?>()
        userProperties.add(UserPropertyInfo(WEBHOOK_URL_KEY, "WebHook URL"))
        userProperties.add(UserPropertyInfo(WEBHOOK_USERNAME_KEY, "Username"))
        notificatorRegistry.register(this, userProperties)
    }

    /**
     * Send the notification by triggering the
     * [DiscordWebHookProcessor.sendDiscordWebHook]
     * method using the data given in the discordWebHookPayload parameter.
     * 
     * @param discordWebHookPayload The payload to send
     * @param users                 The users that should be notified
     */
    private fun processNotify(discordWebHookPayload: DiscordWebHookPayload, users: MutableSet<SUser>) {
        for (user in users) {
            val webHookUrl = user.getPropertyValue(WEBHOOK_URL)
            val username = user.getPropertyValue(USERNAME)
            if (webHookUrl == null || webHookUrl == "") {
                LOGGER.error("The Discord WebHook URL for user '" + user.getName() + "' has not been set. Can't execute the WebHook!")
                return
            }
            if (username != null && username != "") {
                discordWebHookPayload.username = username
            }
            try {
                this.discordWebHookProcessor.sendDiscordWebHook(webHookUrl, discordWebHookPayload)
            } catch (e: IOException) {
                LOGGER.error("Failed to send the WebHook!", e)
            } catch (e: URISyntaxException) {
                LOGGER.error("Failed to send the WebHook!", e)
            }
        }
    }

    /**
     * Gets a Project from a [SRunningBuild] by searching for a project
     * that has the same project id as the running build.
     * 
     * @param sRunningBuild The [SRunningBuild] from which the Project should be grabbed
     * @return The project that owns the build or null
     */
    private fun getProjectFromRunningBuild(sRunningBuild: SRunningBuild): SProject? {
        for (project in this.sBuildServer.getProjectManager().getProjects()) {
            if (project.getProjectId() == sRunningBuild.getProjectId()) {
                return project
            }
        }
        return null
    }

    /**
     * Builds the [DiscordEmbedField]'s that are used in all notifications
     * that are based on an [SRunningBuild].
     * 
     * @param sRunningBuild The build from which the fields will be build
     * @return The [DiscordEmbedField]'s created from the [SRunningBuild]
     */
    private fun buildFieldsForRunningBuild(sRunningBuild: SRunningBuild): Array<DiscordEmbedField?> {
        val discordEmbedFields: MutableList<DiscordEmbedField?> = ArrayList<DiscordEmbedField?>()
        // Grab data
        // Project
        val project = getProjectFromRunningBuild(sRunningBuild)
        var projectName: String = NO_DATA
        if (project != null) {
            projectName = project.getName()
        }
        discordEmbedFields.add(DiscordEmbedField("Project: ", projectName, true))
        // Build name
        discordEmbedFields.add(DiscordEmbedField("Build:", sRunningBuild.getBuildTypeName(), true))
        // Branch
        val branch = sRunningBuild.getBranch()
        var branchName = "Default"
        if (branch != null && branch.getName() != Branch.DEFAULT_BRANCH_NAME) {
            branchName = branch.getDisplayName()
        }
        discordEmbedFields.add(DiscordEmbedField("Branch", branchName, true))
        val comment = sRunningBuild.getBuildComment()
        if (comment != null) {
            discordEmbedFields.add(DiscordEmbedField("Comment", comment.getComment(), false))
        }
        return discordEmbedFields.toTypedArray<DiscordEmbedField?>()
    }

    override fun notifyBuildStarted(sRunningBuild: SRunningBuild, users: MutableSet<SUser>) {
        val title = "Build started"
        val description = "A build with the ID " + sRunningBuild.getBuildNumber() + " has been started!"
        val url = this.sBuildServer.getRootUrl() + "/viewLog.html?buildId=" + sRunningBuild.getBuildId()
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    url,
                    DiscordEmbedColor.BLUE,
                    null,
                    null,
                    null,
                    buildFieldsForRunningBuild(sRunningBuild)
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyBuildSuccessful(sRunningBuild: SRunningBuild, users: MutableSet<SUser>) {
        val title = "Build succeeded!"
        val description = "The build with the ID " + sRunningBuild.getBuildNumber() + " has succeeded!"
        val url = this.sBuildServer.getRootUrl() + "/viewLog.html?buildId=" + sRunningBuild.getBuildId()
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    url,
                    DiscordEmbedColor.GREEN,
                    null,
                    null,
                    null,
                    buildFieldsForRunningBuild(sRunningBuild)
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyBuildFailed(sRunningBuild: SRunningBuild, users: MutableSet<SUser>) {
        val title = "Build failed"
        val description = "The build with the ID " + sRunningBuild.getBuildNumber() + " has failed!"
        val url = this.sBuildServer.getRootUrl() + "/viewLog.html?buildId=" + sRunningBuild.getBuildId()
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    url,
                    DiscordEmbedColor.RED,
                    null,
                    null,
                    null,
                    buildFieldsForRunningBuild(sRunningBuild)
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyBuildFailedToStart(sRunningBuild: SRunningBuild, users: MutableSet<SUser>) {
        val title = "Build failed to start"
        val description = "The build with the ID " + sRunningBuild.getBuildNumber() + " has failed to start!"
        val url = this.sBuildServer.getRootUrl() + "/viewLog.html?buildId=" + sRunningBuild.getBuildId()
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    url,
                    DiscordEmbedColor.RED,
                    null,
                    null,
                    null,
                    buildFieldsForRunningBuild(sRunningBuild)
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyLabelingFailed(build: Build, vcsRoot: VcsRoot, throwable: Throwable, users: MutableSet<SUser>) {
        val title = "Labeling failed"
        val description = "Labeling of build with the ID " + build.getBuildNumber() + " has failed!"
        val url = this.sBuildServer.getRootUrl() + "/viewLog.html?buildId=" + build.getBuildId()
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    url,
                    DiscordEmbedColor.RED,
                    null,
                    null,
                    null,
                    arrayOf<DiscordEmbedField?>()
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyBuildFailing(sRunningBuild: SRunningBuild, users: MutableSet<SUser>) {
        val title = "Build is failing"
        val description = "The build with the ID " + sRunningBuild.getBuildNumber() + " is failing!"
        val url = this.sBuildServer.getRootUrl() + "/viewLog.html?buildId=" + sRunningBuild.getBuildId()
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    url,
                    DiscordEmbedColor.RED,
                    null,
                    null,
                    null,
                    buildFieldsForRunningBuild(sRunningBuild)
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyBuildProbablyHanging(sRunningBuild: SRunningBuild, users: MutableSet<SUser>) {
        val title = "Build is probably hanging"
        val description = "The build with the ID " + sRunningBuild.getBuildNumber() + " is probably hanging!"
        val url = this.sBuildServer.getRootUrl() + "/viewLog.html?buildId=" + sRunningBuild.getBuildId()
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    url,
                    DiscordEmbedColor.ORANGE,
                    null,
                    null,
                    null,
                    buildFieldsForRunningBuild(sRunningBuild)
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyResponsibleChanged(sBuildType: SBuildType, users: MutableSet<SUser>) {
        val title = "Responsibility for build type has changed"
        val description = "The responsibility for the build type " + sBuildType.getExtendedFullName() + " has changed!"
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    "",
                    DiscordEmbedColor.ORANGE,
                    null,
                    null,
                    null,
                    arrayOf<DiscordEmbedField?>()
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyResponsibleAssigned(sBuildType: SBuildType, users: MutableSet<SUser>) {
        val title = "Responsibility assigned"
        val description = "Responsibility for build type " + sBuildType.getExtendedFullName() + " has been assigned!"
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    "",
                    DiscordEmbedColor.ORANGE,
                    null,
                    null,
                    null,
                    arrayOf<DiscordEmbedField?>()
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyResponsibleChanged(
        testNameResponsibilityEntry: TestNameResponsibilityEntry?,
        testNameResponsibilityEntry1: TestNameResponsibilityEntry,
        sProject: SProject,
        users: MutableSet<SUser>
    ) {
        val title = "Responsibility changed"
        val description = "Responsibility for the project " + sProject.getFullName() + " has changed!"
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    "",
                    DiscordEmbedColor.ORANGE,
                    null,
                    null,
                    null,
                    arrayOf<DiscordEmbedField?>()
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyResponsibleAssigned(
        testNameResponsibilityEntry: TestNameResponsibilityEntry?,
        testNameResponsibilityEntry1: TestNameResponsibilityEntry,
        sProject: SProject,
        users: MutableSet<SUser>
    ) {
        val title = "Responsibility assigned"
        val description = "Responsibility for project " + sProject.getFullName() + " has been assigned!"
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    "",
                    DiscordEmbedColor.ORANGE,
                    null,
                    null,
                    null,
                    arrayOf<DiscordEmbedField?>()
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyResponsibleChanged(
        collection: MutableCollection<TestName?>,
        responsibilityEntry: ResponsibilityEntry,
        sProject: SProject,
        users: MutableSet<SUser>
    ) {
        val title = "Responsibility changed"
        val description = "Responsibility for project " + sProject.getFullName() + " has been changed!"
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    "",
                    DiscordEmbedColor.ORANGE,
                    null,
                    null,
                    null,
                    arrayOf<DiscordEmbedField?>()
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyResponsibleAssigned(
        collection: MutableCollection<TestName?>,
        responsibilityEntry: ResponsibilityEntry,
        sProject: SProject,
        users: MutableSet<SUser>
    ) {
        val title = "Responsibility assigned"
        val description =
            "Responsibility for one or more tests of project " + sProject.getFullName() + " have been assigned!"
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    "",
                    DiscordEmbedColor.ORANGE,
                    null,
                    null,
                    null,
                    arrayOf<DiscordEmbedField?>()
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyBuildProblemResponsibleAssigned(
        collection: MutableCollection<BuildProblemInfo?>,
        responsibilityEntry: ResponsibilityEntry,
        sProject: SProject,
        users: MutableSet<SUser>
    ) {
        val title = "Responsibility assigned"
        val description =
            "Responsibility for one or more build problems of project " + sProject.getFullName() + " have been assigned!"
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    "",
                    DiscordEmbedColor.ORANGE,
                    null,
                    null,
                    null,
                    arrayOf<DiscordEmbedField?>()
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyBuildProblemResponsibleChanged(
        collection: MutableCollection<BuildProblemInfo?>,
        responsibilityEntry: ResponsibilityEntry,
        sProject: SProject,
        users: MutableSet<SUser>
    ) {
        val title = "Responsibility assigned"
        val description =
            "Responsibility for one or more tests of project " + sProject.getFullName() + " has been changed!"
        val discordWebHookPayload = DiscordWebHookPayload()
        discordWebHookPayload.embeds = 
            arrayOf<DiscordEmbed?>(
                DiscordEmbed(
                    title,
                    description,
                    "",
                    DiscordEmbedColor.ORANGE,
                    null,
                    null,
                    null,
                    arrayOf<DiscordEmbedField?>()
                )
            )
        this.processNotify(discordWebHookPayload, users)
    }

    override fun notifyTestsMuted(collection: MutableCollection<STest?>, muteInfo: MuteInfo, users: MutableSet<SUser>) {
        val title = "Tests muted"
        if (muteInfo.getProject() != null) {
            muteInfo.getProject()!!.getFullName()
            val description =
                "One or more tests of the project " + muteInfo.getProject()!!.getFullName() + " have been muted!"
            val discordWebHookPayload = DiscordWebHookPayload()
            discordWebHookPayload.embeds = 
                arrayOf<DiscordEmbed?>(
                    DiscordEmbed(
                        title,
                        description,
                        "",
                        DiscordEmbedColor.ORANGE,
                        null,
                        null,
                        null,
                        arrayOf<DiscordEmbedField?>()
                )
            )
            this.processNotify(discordWebHookPayload, users)
        }
    }

    override fun notifyTestsUnmuted(
        collection: MutableCollection<STest?>,
        muteInfo: MuteInfo,
        sUser: SUser?,
        users: MutableSet<SUser>
    ) {
        val title = "Tests unmuted"
        if (muteInfo.getProject() != null) {
            muteInfo.getProject()!!.getFullName()
            val description =
                "One or more tests of the project " + muteInfo.getProject()!!.getFullName() + " have been unmuted!"
            val discordWebHookPayload = DiscordWebHookPayload()
            discordWebHookPayload.embeds = 
                arrayOf<DiscordEmbed?>(
                    DiscordEmbed(
                        title,
                        description,
                        "",
                        DiscordEmbedColor.ORANGE,
                        null,
                        null,
                        null,
                        arrayOf<DiscordEmbedField?>()
                )
            )
            this.processNotify(discordWebHookPayload, users)
        }
    }

    override fun notifyBuildProblemsMuted(
        collection: MutableCollection<BuildProblemInfo?>,
        muteInfo: MuteInfo,
        users: MutableSet<SUser>
    ) {
        val title = "Build problems muted"
        if (muteInfo.getProject() != null) {
            muteInfo.getProject()!!.getFullName()
            val description = "One or more build problems of the project " + muteInfo.getProject()!!
                .getFullName() + " have been muted!"
            val discordWebHookPayload = DiscordWebHookPayload()
            discordWebHookPayload.embeds = 
                arrayOf<DiscordEmbed?>(
                    DiscordEmbed(
                        title,
                        description,
                        "",
                        DiscordEmbedColor.ORANGE,
                        null,
                        null,
                        null,
                        arrayOf<DiscordEmbedField?>()
                )
            )
            this.processNotify(discordWebHookPayload, users)
        }
    }

    override fun notifyBuildProblemsUnmuted(
        collection: MutableCollection<BuildProblemInfo?>,
        muteInfo: MuteInfo,
        sUser: SUser?,
        users: MutableSet<SUser>
    ) {
        val title = "Build problems unmuted"
        if (muteInfo.getProject() != null) {
            muteInfo.getProject()!!.getFullName()
            val description = "One or more build problems of the project " + muteInfo.getProject()!!
                .getFullName() + " have been unmuted!"
            val discordWebHookPayload = DiscordWebHookPayload()
            discordWebHookPayload.embeds = 
                arrayOf<DiscordEmbed?>(
                    DiscordEmbed(
                        title,
                        description,
                        "",
                        DiscordEmbedColor.ORANGE,
                        null,
                        null,
                        null,
                        arrayOf<DiscordEmbedField?>()
                )
            )
            this.processNotify(discordWebHookPayload, users)
        }
    }

    override fun getNotificatorType(): String {
        return TYPE
    }

    override fun getDisplayName(): String {
        return DISPLAY_NAME
    }

    companion object {
        /**
         * The logger used for debug messages.
         * Mostly used for error logging.
         */
        private val LOGGER: Logger = Logger.getLogger(DiscordNotificator::class.java)

        /**
         * The type of this [Notificator]
         */
        private const val TYPE = "DiscordNotificator"

        /**
         * The display name of this notificator
         */
        private const val DISPLAY_NAME = "Discord WebHook"

        /**
         * The name of the [PropertyKey] [.WEBHOOK_URL]
         */
        private const val WEBHOOK_URL_KEY = "DiscordWebHookURL"

        /**
         * The name of the [PropertyKey] [.USERNAME]
         */
        private const val WEBHOOK_USERNAME_KEY = "DiscordUsername"

        /**
         * [PropertyKey] of the property that defines the URL of the WebHook
         */
        private val WEBHOOK_URL: PropertyKey = NotificatorPropertyKey(TYPE, WEBHOOK_URL_KEY)

        /**
         * [PropertyKey] of the property that defines the Username of the WebHook
         */
        private val USERNAME: PropertyKey = NotificatorPropertyKey(TYPE, WEBHOOK_USERNAME_KEY)

        /**
         * The string used for situation where no data is available to display
         */
        private const val NO_DATA = "<No data available>"
    }
}
