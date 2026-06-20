---
date: 2026-06-21
topic: agent-workbench-rich-chat
---

# Rich Visual Chat Interface for Agent Workbench

## Summary

Add a rich visual chat interface to the existing agent-workbench plugin, replacing the current terminal-based interaction with a proper chat UI that supports Markdown rendering, code syntax highlighting, streaming text display, and message role distinction. The underlying CLI bridge and multi-engine support (Claude, Codex, Junie, OpenCode) are already functional in agent-workbench.

## Problem Frame

Agent-workbench renders AI chat output inside a terminal emulator (`AgentChatFileEditor` wraps `TerminalView`). This prevents Markdown rendering, syntax-highlighted code blocks, structured tool output display, and interactive code actions. The sessions sidebar and engine bridge already provide structured session data — only the chat display layer needs replacement.

The ccguif project demonstrates what a rich AI chat interface looks like (React-based, Markdown rendering, streaming, code blocks, tool output visualization), but it's a standalone Tauri desktop app. The goal is to bring that level of chat UI quality into the IntelliJ ecosystem as a plugin.

## Key Decisions

- **JCEF for chat rendering.** The rich chat panel should use JCEF (JetBrains Chromium Embedded Framework) to render HTML/CSS/JS-based chat UI. Pure Swing cannot match the rendering quality needed for Markdown, syntax-highlighted code blocks, and interactive elements without disproportionate effort. JCEF is already used in IntelliJ for web-based tool windows and is the same technology that powers the built-in browser.

- **Consume existing structured session data.** Agent-workbench already exposes structured data through `AgentSessionSource` — thread outlines with typed message kinds (USER_PROMPT, ASSISTANT_RESPONSE, TOOL_CALL, TOOL_RESULT), WebSocket notifications, and JSONL rollout parsing (e.g., `CodexRolloutParser`). The chat UI should consume these existing abstractions directly, not parse raw terminal bytes.

- **Layout as IntelliJ tool windows.** The chat interface uses IntelliJ's native tool window system: conversation history in a left tool window (extending the existing sessions tree), chat panel in the center (as an editor area or dedicated tool window), and IntelliJ's built-in Project view on the right.

## Requirements

**Chat Interface Core**

- R1. The chat panel renders messages in a scrollable view with clear visual distinction between user messages, assistant responses, system messages, and tool outputs.
- R2. Assistant messages support full Markdown rendering including headers, bold/italic, lists, tables, blockquotes, and inline code.
- R3. Code blocks within messages display with syntax highlighting for common languages (TypeScript, JavaScript, Python, Java, Kotlin, Rust, Go, Bash, JSON, YAML).
- R4. Code blocks include a copy-to-clipboard button. When a code block represents a file change, it offers a lightweight quick-apply action. For multi-file changesets, the chat UI provides a full diff preview using IntelliJ's diff infrastructure (see R15).
- R5. The chat panel displays streaming text in real time as the AI engine produces output, without requiring a full message completion before display.
- R6. The chat panel supports collapsible sections for tool call invocations and their results, keeping the conversation readable when the AI uses many tools.

**Message Navigation and History**

- R7. The conversation history panel (left tool window) shows a list of sessions/threads with title, timestamp, engine type, and status indicators.
- R8. Selecting a session from the history loads its message content into the chat panel.
- R9. New sessions can be created from the history panel, with engine selection (Claude, Codex, Junie, OpenCode).
- R10. The chat panel supports scrolling through the full message history of a session with efficient virtualized rendering for long conversations.

**Engine Integration**

- R11. The chat UI consumes the existing structured session data layer (AgentSessionSource, WebSocket notifications, JSONL rollout parsing) rather than re-parsing terminal output. The terminal view may remain available as a fallback.
- R12. The user can switch between available engines (Claude, Codex, etc.) when creating a new session, with the engine choice reflected in the session metadata.
- R13. V1 ships a uniform chat UI that renders all engines' messages through a common abstraction. Engine-specific features (e.g., Claude's reasoning display, Codex's plan visibility) are deferred to a follow-up once the base chat UI is validated.

**Context and File Integration**

- R14. When the AI produces code changes, the chat UI offers a diff preview or inline apply option using IntelliJ's diff infrastructure.

## Success Criteria

- AI-generated Markdown renders correctly for all tested conversation patterns (headers, code blocks, tables, lists).
- Chat panel startup time is within 2x of the current terminal view startup.
- Users can read AI output, copy code, and apply a code change in fewer interactions than the terminal view requires.
- Streaming display shows text within 100ms of data receipt from the engine bridge.

## Scope Boundaries

**Deferred for later:**
- Image/file preview within chat messages
- Drag-and-drop file attachment
- File/code selection from editor as context attachment (requires JCEF-to-IntelliJ IPC for file data)
- Engine-specific UI features (reasoning display, plan panels, per-engine rendering)
- Rich notification system for background AI tasks
- Marketplace publication and plugin packaging for distribution
- JetBrains AI Assistant coexistence or integration testing

**Outside this product's identity:**
- Replacing or competing with JetBrains AI Assistant — this is an alternative multi-engine interface, not a replacement
- Terminal, Git, Kanban, or other non-chat tool features from ccguif
- Custom file tree component — IntelliJ's built-in Project view is sufficient

## Outstanding Questions

**Resolved:**
- JCEF is the chosen rendering technology for the chat panel. The chat UI will be an HTML/CSS/JS page rendered via JcefBrowser in a tool window or editor area. This decision is confirmed by the user and resolves the primary architectural question.

**Deferred to Planning:**
- JCEF vs alternatives (Swing with Markdown plugin, JavaFX) — cost/benefit analysis with memory, startup latency, and IPC overhead quantification.
- How each engine's structured data schema maps to the common chat message abstraction.
- Performance validation: JCEF rendering of long conversations (500+ messages, 100+ tool call sections), streaming update latency, memory consumption vs terminal view baseline.
- Whether the terminal view remains as a fallback or is fully replaced.

## Sources

- `plugins/agent-workbench/` — existing plugin with CLI bridge, session management, terminal-based chat
- `plugins/agent-workbench/codex/common/src/CodexWebSocketAppServerClient.kt` — reference for CLI process + WebSocket bridge pattern
- `plugins/agent-workbench/chat/src/AgentChatFileEditor.kt` — current terminal-based chat implementation
- `plugins/agent-workbench/sessions-toolwindow/` — existing session tree UI
- ccguif project at `/Users/yang/project/project/ccguif` — reference for rich chat UI design (React + Markdown rendering, streaming, code blocks)
