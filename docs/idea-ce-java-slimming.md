# IDEA CE Java Slimming Notes

Branch: `codex/slim-java-ce-plugins`

Goal: keep Java full-stack development support while reducing bundled non-essential plugins and feature modules.

## Kept

- Markdown preview: `intellij.markdown`
- Java core: `intellij.java.plugin`
- JVM/Kotlin/Groovy core support
- Maven and Gradle project support
- JUnit and TestNG support
- Git support
- XML, JSON, YAML, Properties, TOML support
- Terminal support

## Removed From IDEA Bundled Plugins

| Area | Removed module | Reason |
| --- | --- | --- |
| Diagrams in Markdown | `intellij.mermaid` | Mermaid rendering is optional; Markdown preview itself is kept. |
| AI/tooling integration | `intellij.mcpserver` | Not required for Java full-stack development. |
| Writing assistance | `intellij.grazie` | Natural-language/spellchecking assistance is optional. |
| Training/onboarding | `intellij.featuresTrainer` | Tutorial content is optional. |
| Performance tooling | `intellij.performanceTesting` | Internal/performance test plugin is not needed in a slim runtime. |
| Android residual support | `intellij.android.gradle.declarative.lang.ide` | Android-specific Gradle declarative support is not needed. |
| Android residual support | `intellij.android.gradle.dsl` | Android-specific Gradle DSL support is not needed. |
| Android design plugin | `intellij.android.design-plugin.descriptor` | Entire Android UI/design plugin removed; not needed for Java full-stack. |
| Python support | `intellij.python.community.plugin` | Python language support removed; not needed for Java-only CE. |
| Legacy import | `intellij.eclipse` | Eclipse project import/support is optional legacy compatibility. |
| Legacy keymaps | `intellij.keymap.eclipse` | Optional keymap. |
| Legacy keymaps | `intellij.keymap.visualStudio` | Optional keymap. |
| Legacy keymaps | `intellij.keymap.netbeans` | Optional keymap. |
| JavaFX | `intellij.javaFX.community` | JavaFX desktop support is not needed for the target Java full-stack setup. |

## Removed From Java/Kotlin Plugin Content

| Area | Removed module | Reason |
| --- | --- | --- |
| Java training | `intellij.java.featuresTrainer` | Depends on the removed feature trainer flow. |
| Kotlin training | `intellij.kotlin.featuresTrainer` | Depends on the removed feature trainer flow. |

## Removed Module Aliases

| Alias | Reason |
| --- | --- |
| `com.intellij.modules.python-core-capable` | Python capability declaration removed from IDEA CE. |
| `com.intellij.modules.python-in-non-pycharm-ide-capable` | Python capability declaration removed from IDEA CE. |

## Files Changed

- `platform/build-scripts/src/org/jetbrains/intellij/build/BaseIdeaProperties.kt`
- `build/src/org/jetbrains/intellij/build/IdeaCommunityProperties.kt`
- `java/plugin/resources/META-INF/plugin.xml`
- `java/plugin/plugin-content.yaml`
- `platform/build-scripts/src/org/jetbrains/intellij/build/kotlin/KotlinPluginBuilder.kt`
- `plugins/kotlin/plugin/k2/resources/kotlin.plugin.k2.xml`
- `plugins/kotlin/plugin/plugin-content.yaml`

## Notes

- `.idea/modules.xml` currently has unrelated Android module removals in the working tree. Treat that separately from this product-bundling slimming record.
- This document tracks packaging-level slimming. Source directories are not deleted.
- Expected runtime memory improvement from this group is modest to medium because Java IDE core, indexing, PSI, editor, Maven/Gradle, and debugger remain enabled.
