package com.intellij.terminal.backend.util

import com.google.common.base.Ascii
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.project.Project
import com.intellij.terminal.backend.TerminalSessionsManager
import com.intellij.terminal.backend.createTerminalSession
import com.intellij.terminal.backend.startTerminalProcess
import com.intellij.terminal.backend.util.TerminalSessionTestUtil.startTestStateAwareTerminalSession
import com.intellij.terminal.session.TerminalOutputEvent
import com.intellij.terminal.session.TerminalSession
import com.intellij.util.EnvironmentUtil
import com.intellij.util.asDisposable
import com.jediterm.core.util.TermSize
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.plugins.terminal.JBTerminalSystemSettingsProvider
import org.jetbrains.plugins.terminal.ShellStartupOptions
import org.jetbrains.plugins.terminal.TerminalEngine
import org.jetbrains.plugins.terminal.reworked.util.TerminalTestUtil
import java.nio.file.Files
import java.nio.file.Path

internal object TerminalSessionTestUtil {
  /**
   * Creates the same backend terminal session as in production.
   */
  suspend fun startTestStateAwareTerminalSession(
    shellPath: String,
    project: Project,
    coroutineScope: CoroutineScope,
    size: TermSize = TermSize(80, 24),
    extraEnvVariables: Map<String, String> = emptyMap(),
    workingDirectory: String = System.getProperty("user.home"),
  ): TerminalSession {
    TerminalTestUtil.setTerminalEngineForTest(TerminalEngine.REWORKED, coroutineScope.asDisposable())
    val options = createShellStartupOptions(shellPath, size, extraEnvVariables, workingDirectory)

    val manager = TerminalSessionsManager.getInstance()
    val sessionStartResult = manager.startSession(options, project, coroutineScope)
    return manager.getSession(sessionStartResult.sessionId)!!
  }

  /**
   * Creates the low-level terminal session that does not use [com.intellij.terminal.backend.StateAwareTerminalSession] wrapper.
   * Use this method only when you need to make low-level tests, for example, to check the exact sequences of the output events.
   * [startTestStateAwareTerminalSession] is not suitable for such tests because it can merge some initial events
   * into [com.intellij.terminal.session.TerminalInitialStateEvent].
   *
   * In all other cases please use [startTestStateAwareTerminalSession] method to test the same functionality used in production.
   */
  fun startTestJediTermTerminalSession(
    shellPath: String,
    project: Project,
    coroutineScope: CoroutineScope,
    size: TermSize = TermSize(80, 24),
    extraEnvVariables: Map<String, String> = emptyMap(),
    workingDirectory: String = System.getProperty("user.home"),
  ): TerminalSession {
    TerminalTestUtil.setTerminalEngineForTest(TerminalEngine.REWORKED, coroutineScope.asDisposable())
    val options = createShellStartupOptions(shellPath, size, extraEnvVariables, workingDirectory)

    val (ttyConnector, configuredOptions) = startTerminalProcess(project, options)
    val session = createTerminalSession(project, ttyConnector, configuredOptions, JBTerminalSystemSettingsProvider(), coroutineScope)
    return session
  }

  private fun createShellStartupOptions(
    shellPath: String,
    size: TermSize = TermSize(80, 24),
    extraEnvVariables: Map<String, String> = emptyMap(),
    workingDirectory: String = System.getProperty("user.home"),
  ): ShellStartupOptions {
    return ShellStartupOptions.Builder()
      .shellCommand(listOf(shellPath))
      .workingDirectory(workingDirectory)
      .initialTermSize(size)
      .envVariables(mapOf(EnvironmentUtil.DISABLE_OMZ_AUTO_UPDATE to "true", "HISTFILE" to "/dev/null") + extraEnvVariables)
      .build()
  }

  suspend fun TerminalSession.awaitOutputEvent(targetEvent: TerminalOutputEvent) {
    return coroutineScope {
      val promptFinishedEventDeferred = CompletableDeferred<Unit>()

      val flowCollectionJob = launch {
        getOutputFlow().collect { events ->
          if (events.any { it == targetEvent }) {
            promptFinishedEventDeferred.complete(Unit)
          }
        }
      }

      promptFinishedEventDeferred.await()
      flowCollectionJob.cancel()
    }
  }

  fun getShellPaths(): List<Path> {
    return listOf(
      "/bin/zsh",
      "/urs/bin/zsh",
      "/urs/local/bin/zsh",
      "/opt/homebrew/bin/zsh",
      "/bin/bash",
      "/opt/homebrew/bin/bash",
    ).mapNotNull {
      val path = Path.of(it)
      if (Files.isRegularFile(path)) path else PathEnvironmentVariableUtil.findInPath(it)?.toPath()
    }
  }

  val ENTER_BYTES: ByteArray = byteArrayOf(Ascii.CR)
}