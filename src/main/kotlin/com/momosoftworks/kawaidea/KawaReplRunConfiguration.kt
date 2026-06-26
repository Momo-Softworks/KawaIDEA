package com.momosoftworks.kawaidea

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderEnumerator
import org.jdom.Element
import javax.swing.*

/**
 * Minimal run configuration that starts a Kawa REPL server on a TCP port.
 * Uses the first Java module's classpath.
 *
 * After starting, connect from Emacs: M-x geiser-kawa-connect → port 4243.
 */
class KawaReplRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
) : RunConfigurationBase<Any?>(project, factory, name) {

    var port: Int = 4243

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        object : SettingsEditor<KawaReplRunConfiguration>() {
            private val field = javax.swing.JTextField("4243", 6)

            override fun createEditor(): javax.swing.JComponent {
                val p = javax.swing.JPanel(java.awt.FlowLayout())
                p.add(javax.swing.JLabel("TCP port:"))
                p.add(field)
                return p
            }

            override fun applyEditorTo(config: KawaReplRunConfiguration) {
                config.port = field.text.toIntOrNull() ?: 4243
            }

            override fun resetEditorFrom(config: KawaReplRunConfiguration) {
                field.text = config.port.toString()
            }
        }

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        val project = env.project
        val module = ModuleManager.getInstance(project).modules.firstOrNull { m ->
            ModuleRootManager.getInstance(m).sdk?.sdkType is JavaSdk
        } ?: throw ExecutionException("No Java module found in project")

        val sdk = ModuleRootManager.getInstance(module).sdk!!

        val classpath = OrderEnumerator.orderEntries(module)
            .recursively()
            .runtimeOnly()
            .pathsList
            .pathsString

        val javaHome = sdk.homePath ?: throw ExecutionException("SDK home not set")
        val sep = java.io.File.separator
        val javaExe = "$javaHome${sep}bin${sep}java"

        return object : CommandLineState(env) {
            override fun startProcess(): OSProcessHandler {
                val cmd = GeneralCommandLine(javaExe)
                cmd.addParameter("-cp")
                cmd.addParameter(classpath)
                cmd.addParameter("kawa.repl")
                cmd.addParameter("--server")
                cmd.addParameter(port.toString())
                return OSProcessHandler(cmd)
            }
        }
    }
}

class KawaReplConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "kawa-repl"
    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        KawaReplRunConfiguration(project, this, "Kawa REPL")
}

class KawaReplConfigurationType : ConfigurationType {
    override fun getId(): String = "kawa-repl"
    override fun getDisplayName(): String = "Kawa REPL"
    override fun getConfigurationTypeDescription(): String = "Starts a Kawa Scheme REPL server"
    override fun getIcon(): javax.swing.Icon = KawaIcons.FILE
    override fun getConfigurationFactories(): Array<ConfigurationFactory> =
        arrayOf(KawaReplConfigurationFactory(this))
}
