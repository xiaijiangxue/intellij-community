// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.intellij.build.BuildContext
import org.jetbrains.intellij.build.BuildOptions
import org.jetbrains.intellij.build.OsFamily
import org.jetbrains.intellij.build.PLUGIN_XML_RELATIVE_PATH
import org.jetbrains.intellij.build.buildCommunityStandaloneJpsBuilder
import org.jetbrains.intellij.build.createCommunityBuildContext
import org.jetbrains.intellij.build.findFileInModuleSources
import org.jetbrains.intellij.build.findUnprocessedDescriptorContent
import org.jetbrains.intellij.build.impl.buildDistributions
import org.jetbrains.intellij.build.telemetry.TraceManager.spanBuilder
import org.jetbrains.intellij.build.telemetry.use

@ApiStatus.Internal
object OpenSourceCommunityInstallersBuildTarget {
  /**
   * The steps that are excessive because the results are never published by `.github/workflows/IntelliJ_IDEA.yml`.
   * Also, skipping them allows sparing the GitHub runner's disk space.
   */
  private val BUILD_STEPS_DISABLED_FOR_GITHUB_ACTIONS: Set<String> = setOf(
    BuildOptions.WINDOWS_ZIP_STEP,
    BuildOptions.CROSS_PLATFORM_DISTRIBUTION_STEP,
    BuildOptions.SOURCES_ARCHIVE_STEP,
    BuildOptions.ARCHIVE_PLUGINS,
  )

  val OPTIONS: BuildOptions = BuildOptions().apply {
    // do not bother external users about clean/incremental
    // just remove out/ directory for clean build
    incrementalCompilation = true
    useCompiledClassesFromProjectOutput = false
    buildStepsToSkip += BuildOptions.MAC_SIGN_STEP
    buildStepsToSkip += BuildOptions.WIN_SIGN_STEP
    if (OsFamily.currentOs == OsFamily.MACOS) {
      // generally not needed; doesn't work well on build agents
      buildStepsToSkip += BuildOptions.WINDOWS_EXE_INSTALLER_STEP
    }
  }

  @JvmStatic
  fun main(args: Array<String>) {
    runBlocking(Dispatchers.Default) {
      val options = OPTIONS.copy(buildStepsToSkip = OPTIONS.buildStepsToSkip + BUILD_STEPS_DISABLED_FOR_GITHUB_ACTIONS)
      val context = createCommunityBuildContext(options)
      filterUnavailablePluginLayouts(context)
      context.compileModules(moduleNames = null, includingTestsInModules = listOf("intellij.platform.jps.build.tests"))
      buildDistributions(context)
      spanBuilder("build standalone JPS").use {
        buildCommunityStandaloneJpsBuilder(targetDir = context.paths.artifactDir.resolve("jps"), context)
      }
    }
  }

  private suspend fun filterUnavailablePluginLayouts(context: BuildContext) {
    val outputProvider = context.outputProvider
    val productLayout = context.productProperties.productLayout
    val unavailablePlugins = LinkedHashSet<String>()
    val bundledPluginModules = context.getBundledPluginModules().toHashSet()
    productLayout.pluginLayouts = productLayout.pluginLayouts
      .filter { layout ->
        val hasPluginDescriptor = isPluginDescriptorAvailable(layout.mainModule, context)
        val isAvailable = layout.includedModules.all { outputProvider.findModule(it.moduleName) != null } &&
                          hasPluginDescriptor
        if (!isAvailable) {
          if (hasPluginDescriptor) {
            unavailablePlugins.add(layout.mainModule)
          }
          Span.current().addEvent("Plugin layout '${layout.mainModule}' is excluded because it is not available in module output")
        }
        isAvailable
      }
      .toPersistentList()

    if (productLayout.buildAllCompatiblePlugins) {
      for (module in context.project.modules) {
        val moduleName = module.name
        if (moduleName in bundledPluginModules ||
            moduleName in productLayout.compatiblePluginsToIgnore ||
            outputProvider.findModule(moduleName) == null ||
            findFileInModuleSources(module = module, relativePath = PLUGIN_XML_RELATIVE_PATH, onlyProductionSources = true) == null ||
            isPluginDescriptorAvailable(moduleName, context)) {
          continue
        }
        unavailablePlugins.add(moduleName)
        Span.current().addEvent("Compatible plugin '$moduleName' is ignored because it is not available in module output")
      }
    }
    productLayout.compatiblePluginsToIgnore = productLayout.compatiblePluginsToIgnore.addAll(unavailablePlugins)
  }

  private suspend fun isPluginDescriptorAvailable(moduleName: String, context: BuildContext): Boolean {
    val module = context.outputProvider.findModule(moduleName) ?: return false
    return try {
      findUnprocessedDescriptorContent(module = module, path = PLUGIN_XML_RELATIVE_PATH, outputProvider = context.outputProvider) != null
    }
    catch (_: Throwable) {
      false
    }
  }
}
