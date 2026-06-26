package com.momosoftworks.kawaidea

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * Run configuration that starts a Kawa REPL server on a TCP port.
 * Uses the first Java module's classpath.
 *
 * Equivalent to:  java -cp <classpath> kawa.repl --server <port>
 * After starting, connect from Emacs: M-x geiser-kawa-connect → port 4243.
 */
class KawaReplRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
) : RunConfigurationBase<Any?>(project, factory, name) {

    var port: Int = 4243

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        KawaReplSettingsEditor()

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

        val javaHome = sdk.homePath
            ?: throw ExecutionException("SDK home not set")
        val javaExe = "$javaHome/bin/java"

        return object : CommandLineState(env) {
            @Throws(ExecutionException::class)
            override fun startProcess(): ProcessHandler {
                val cmd = GeneralCommandLine(javaExe)
                    .withParameters("-cp", classpath, "kawa.repl", "--server", port.toString())
                val handler = ProcessHandlerFactory.getInstance()
                    .createColoredProcessHandler(cmd)
                ProcessTerminatedListener.attach(handler)
                return handler
            }
        }
    }
}

class KawaReplConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = KawaReplConfigurationType.ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        KawaReplRunConfiguration(project, this, "Kawa REPL")
}

class KawaReplConfigurationType : ConfigurationTypeBase(
    ID, "Kawa REPL", "Starts a Kawa Scheme REPL server",
    NotNullLazyValue.createValue { KawaIcons.FILE },
) {
    init {
        addFactory(KawaReplConfigurationFactory(this))
    }

    companion object {
        const val ID = "kawa-repl"
    }
}

class KawaReplSettingsEditor : SettingsEditor<KawaReplRunConfiguration>() {
    private val portField = JTextField("4243", 6)

    override fun createEditor(): JComponent =
        FormBuilder.createFormBuilder()
            .addLabeledComponent("TCP port:", portField)
            .panel

    override fun applyEditorTo(config: KawaReplRunConfiguration) {
        config.port = portField.text.toIntOrNull() ?: 4243
    }

    override fun resetEditorFrom(config: KawaReplRunConfiguration) {
        portField.text = config.port.toString()
    }
}
