# Kimi Code environment — GZR

Rules entry: repo-root `AGENTS.md` (auto-loaded every session); domain facts: `AGENTS-reference.md` (read on demand). This file records only what is specific to the Kimi Code CLI runtime. Verified in-session 2026-09-18.

- **Runtime**: Kimi Code CLI on WSL (Linux). Project root `/mnt/c/alex/code/gzr-mod-dev/guzhenren-template-1.21.1` (= `C:\alex\code\gzr-mod-dev\guzhenren-template-1.21.1`); Windows paths in the AGENTS files map to `/mnt/<drive>/...`.
- **Models**: main `kimi-code/k3`; no `[secondary_model]` pool configured — subagents inherit the main model. User config lives at `~/.kimi-code/config.toml` (contains credentials — never read it whole; grep single keys only).
- **Builds/tests**: no JDK in WSL — `cmd.exe /c "gradlew.bat build"` etc. (reference §3). Never run two Gradle invocations against the same output directory.
- **Git**: commit/push via Windows Git Bash (`cmd.exe /c "git ..."`, which has `core.autocrlf=true`). WSL git (autocrlf unset) reports the whole tree as modified — pure CRLF viewpoint noise; judge real changes with `git diff --ignore-cr-at-eol` (details in reference §3 「行尾视角」).
- **No `gh` CLI** on either side — poll GitHub Actions via REST API (`curl -L`, the old-name remote 301-redirects) in a sleep loop (AGENTS.md §4.1).
- **MCP**: JetBrains (IDEA's port, online only while IDEA runs) and Context7 — both verified callable 2026-09-18. Server declarations live in the user-level `~/.kimi-code/mcp.json` (may contain tokens — do not read).
- **`local.toml`** (gitignored, machine-specific) attaches `../project-wiki` as an additional workspace directory.
