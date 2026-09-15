# AGENTS.md — GZR main mod

The one authoritative rules file for the GZR project. It is self-contained: no parent-workspace or external global rules file outranks or extends it. Domain facts live in [AGENTS-reference.md](AGENTS-reference.md), read on demand — never loaded whole. User instructions in chat always beat this file.

## 1. Constitutions

These override everything below. Unless a task explicitly says otherwise, run the Four-Quadrant check on every user message.

### 1.1 Four-Quadrant Protocol

Don't just generate a literal answer. Before producing anything, sort what you know:

1. **Both of us know it.** Confirm goal, context, definition of done, boundaries — then just execute. No ritual restating, no re-asking what's already settled.
2. **Alex knows it, I don't.** Spot the preferences, standards and real-world constraints that only exist in his context. If they change the outcome, ask. If they don't, state the assumption out loud and keep moving.
3. **I know it, Alex doesn't.** Volunteer what he's missing: risks, alternatives, a better path. If his premise looks wrong, say so plainly and show the evidence.
4. **Neither of us knows.** Turn the unknown into a testable hypothesis. Smallest experiment that changes one variable, with an explicit success signal, failure signal, and what data to bring back.

This covers understanding, questions, design, implementation, verification and closeout. When solutions compete, pick the long-term optimum: correctness, stability, coverage of key scenarios and verifiability come first; brevity, minimal diff and cost come last. "Minimal scope" only stops unrelated expansion — it is never an excuse for a wrong or half-done fix.

### 1.2 Main / Sub-agent Protocol

The main agent owns goals, scope, priorities, decomposition, integration and final acceptance. Subagents execute exactly what was delegated.

- Every dispatch carries six things: goal, context, allowed paths, forbidden paths, delivery format, acceptance criteria.
- Subagents don't expand their own scope or change direction. Out-of-scope findings, design conflicts, anything needing Alex's call: report, never decide.
- A subagent report must state result, evidence (paths and line numbers), actual changes, verification, assumptions, blockers and leftovers. The main agent re-verifies against the live checkout and WIP before accepting anything.
- Designs Alex hasn't decided are undecided. No agent treats the wiki's pending-design notes or TODO lists as settled.
- Model tiers per environment — ZCode: main `GLM-5.3` (highest), sub `GLM-5.3-Flash` (highest); Codex: main `GPT-6-Astra Medium`, sub `GPT-5.6-Luna-Max`.
- Parallel work never shares write-files, and two Gradle runs never write the same output directory (retry on lock conflict, or serialize).

### 1.3 Reply Principle

Write for a reader who acts on the first line and the last line. (Adapted from [i-have-adhd](https://github.com/ayghri/i-have-adhd).)

- First line = something Alex can act on: the result, the command, the verdict.
- Multistep work gets numbered steps. A short path finished beats a complete path abandoned.
- End with exactly ONE concrete next action he can do in under two minutes.
- One tangent max per reply. Finish the first thing; offer the second as a separate question.
- Keep a one-line progress anchor each turn ("step 2 of 4, tests running"), not a re-narration.
- Estimate work in concrete units when predicting ("full build ≈3 min"), not "a bit".
- Make finished work visible in concrete terms — what now works, what's now true.
- Errors get stated matter-of-fact: cause, fix, done. No "uh oh".
- Lists aim for ≤5 items; group anything longer.
- No preamble, no recap of what he just said, no closing pleasantries.
- Chat in Chinese. Explain toolchain assuming a Java-only background — don't assume git/gradle internals knowledge.

Exceptions (format yields to the task): `explain`-type requests get the full walkthrough, still without filler; destructive actions confirm first; after three failed fix rounds stop coding and question the assumption with one diagnostic question; real ambiguity gets one short clarifying question instead of a guessed rewrite. Verification facts (what compiled, what tested, what awaits Alex) are information, not ceremony — state them inline where the work is described, never as a mandatory closing block.

## 2. Boundaries

- Main project root `C:\alex\code\gzr-mod-dev\guzhenren-template-1.21.1` is the only Git repo. Its parent workspace is intentionally not a repo; check this repository's `status`/`diff` at task start and preserve checkouts and uncommitted WIP.
- Wiki `..\project-wiki` has no version control. Back up touched files to Temp and verify SHA-256 before editing; edits stay precise and reviewable; before deleting anything check content, references, tracking and use, and keep a recoverable backup. Same ritual for this file and the reference before rule edits.
- Alex edits files live in IDEA. On an edit conflict, re-read and identify the source; his wording changes stay as-is.
- Model authority `C:\alex\code\blockbench\gzr-models\` is read-only; exports land in the mod.
- Numbers live in code: Gu (蛊) specs read from `ModItems.GuSpec`, characters from their enums and services. The dev wiki explains design and reasoning; the player wiki is a version snapshot. One primary number location per audience; everything else references it.
- Undecided design and TODO are Alex's calls — never implemented ahead, never judged as defects. Scope is mortals and rank 1..5 (一转..五转); nothing shaped for stage 3 and later (ascension 仙, immortal Gu, rank 6..9) gets built or counted as missing.
- Think like a Minecraft modder and a modpack player: gameplay flow, compatibility, maintenance cost. Review against Java types, state invariants, server authority and lifecycle.
- Tools: `rg` for text, IDEA's JetBrains MCP for Java symbols and references, and ast-grep for structural queries. If IDEA is unavailable, inspect disk and pinned dependency sources; do not start Serena for Codex GZR work. An index never overrides the disk. A new MCP or plugin counts only after four steps — installed, config valid, callable in session, actually effective — and no duplicates just for tool count.
- One-off probes, diagnostics, smoke tests and temporary conversion scripts live in an explicit temporary location. Delete them after their final use and result verification, before closing the same task. A script kept across tasks belongs under `tools/` with a clear name, documented purpose and reproducible verification. Before deleting legacy leftovers, check whether they are still the only working install or an active asset.
- Keep this file lean. Domain facts go to the reference or the wiki, not into a bigger autoload.

## 3. Map

| Path                      | What it is                                                                          |
|---------------------------|-------------------------------------------------------------------------------------|
| `./`                      | Main mod, id `guzhenren`, and the only Git repo                                     |
| `../project-wiki/开发向/` | Attached design wiki, entry 《蛊 模组设定 MOC》                                     |
| `../project-wiki/玩家向/` | Attached player snapshot; 《时间与时间戳总表》《TODO总表》 are the closeout indexes |
| `../assets/textures/`     | Alex's art staging; intentionally not attached to the Codex project                 |
| `../assets/test modpack/` | Old modpack mirror; attach only for a task that actually needs it, then remove it   |
| `tools/gen_template.py`   | Regenerates the GameTest `empty9x9x9.nbt` scenario template                         |

Obsidian note: files named like their title don't repeat that title as an H1 in the body. Wiki documents describe the present; version snapshots, undecided designs and rejected-idea records are anti-regression history — keep them.

## 4. Workflows

### 4.1 Commit — only when Alex explicitly asks

1. Group changes by independent purpose; each group carries its own tests, providers and generated resources.
2. Stage explicit files or reviewed `git apply --cached` patches — never whole shared files into the wrong group. Review with `git diff --cached`. Never simulate an intermediate state by reverting the working tree; export the index tree to a temp mod copy instead.
3. Offer 2..3 English title candidates in a selectable prompt and wait for his pick — never default, never pick for him. Then commit and push in one go.
4. Title shape: `UPDATE <english body>`. No AI footers, no session lines.
5. Every commit includes its datagen output. Hard deps referenced straight from `run/mods` need their CI download step, otherwise CI goes red while local stays green.
6. Don't "fix" the old-name remote redirect. No force push, no history rewrite; check remote-ahead before pushing.
7. Known traps: line-ending noise stays out of commits; Gradle lock conflicts between parallel runs mean retry or serialize; commit status and Actions checks are separate queries — an empty status is not "CI green". Report each commit SHA, title, push state and the real CI state.

### 4.2 New content — new mechanics, entities, items, tools, weapons

1. Read the domain quick-ref first (reference §8 items and Gu, §5/§9 storage and refinement, §10 cultivation/time/Epic Fight) and find the existing pattern in the same seam — e.g. tendable Gu follows `TendedGuItem` — before inventing structure.
2. Registry wiring follows the existing DeferredRegisters. An entity arrives as a full set: EntityType, attributes, spawn predicates, biome tag/modifier, renderer, model/texture/animation, lang, loot. No default spawn egg, taming or breeding.
3. Models import read-only from the blockbench authority; verify export shape, UV and alpha. GeckoLib stays on the project's pinned version. Custom renderers never bypass invisibility or team-glow branches.
4. Any provider change means `runData`, then review the `src/generated` diff.
5. Tests by layer: real world/tick/damage/NBT → L3 GameTest; pure JVM math/spec → L1 `pureTest`; anything touching registry objects → L2 `test`. Boundary tests cover failure paths, not just the happy path.
6. Sync numbers on the spot: dev reasoning to the dev wiki, player values to the player snapshot (a `GuSpec` change reaches the player table the same session). Check both closeout indexes after every wiki touch, even when nothing needed changing. Advancement triggers hang off the "used successfully" landing point — commands never count.

### 4.3 Modification — any change, including when new content missed expectations or structure isn't right

1. Reproduce and locate the root cause before touching code. Fix ordering per the quadrant protocol: correctness, stability, coverage, verifiability first; brevity, diff size, cost last.
2. Preserve WIP: start from `git status`/`diff`; on conflicts re-read and identify the source; Alex's live IDEA edits stay as-is.
3. A symptom Alex reported is already proven — diagnose directly, don't ask him to reproduce it again. `runClient` is always his.
4. One entity report expands by default to the whole family in the same seam.
5. Snapshot tests pin current truth. A red numeric assertion goes to Alex to adjudicate intent; it is not auto-"fixed".
6. After the fix: regression first where feasible (red before green), then verification for the layers actually touched; `runData` if providers moved.

### 4.4 Review — standards compliance, optimization, cleanup

1. Read `git diff HEAD` plus untracked files; build a change-coverage table by purpose; check the call context. Separate new defects from pre-existing adjacent ones from undecided design. Every finding states trigger, path/line, impact and a minimal fix.
2. Numeric review reads whole expressions: saturating addition doesn't protect a later multiplication; clamping before an add can flip the sign. Check zero, negative, MIN/MAX and the normal range — and don't copy the same wrong formula into a test.
3. Lifecycle review: goal interruption, pathing failure, target death, damage rejection, NBT restore, late tracking. Stopping an action must leave a reachable successor state.
4. Bulk reformat or rename is not cleanup. Deliberate signatures and kept behaviors follow the reference (§13 do-not-touch list; ⚠ KEPT stays). Files get deleted only after checking references, tracking, use and activity, with a hashed backup. Never run a whole-repo `git clean`; never touch `run/mods`, saves or fresh test evidence.
5. ast-grep answers structural queries (syntax match ≠ type proof). Library API doubt → the pinned dependency's sources. Report substantive findings, what actually changed, and what stayed unverified.

## 5. Language and style

Chat in Chinese; code, comments and commit messages in American English. Terms come from the language provider / `en_us.json` / wiki《原著词汇 与命名》— never invent bilingual pairs (known trap pairs in reference §2). Formatting follows `.editorconfig` and neighboring code; comments explain non-obvious reasons only; no bulk reformat, no project-wide IDEA "cleanup code". Substantive conventions (`@NotNull`/`@Nullable`, import ordering, Javadoc FQN rules) live in reference §2.

## 6. State

- Spirit spring (元泉, `spirit_spring`) first version committed and pushed: `3dce895`, 28 files. Texture is a tinted vanilla-water placeholder awaiting Alex's #4FC3F7 pass; design notes in 《蛊虫 与蛊材》.
- Pre-existing uncommitted WIP: `build.gradle` renames the legacy variable `vaultRoot`→`wikiRoot` (L2 green including the 6 wiki checks after the rename). Preserve this diff independently of the untracked Codex migration files added on 2026-09-14.
- 2026-09-14 rules pass: a dead side mod (zero-connected, references verified clean) was deleted along with its wiki pages; this file was rewritten in English and moved into the main repository as the self-contained rules entry; the reference was slimmed; wiki rewrite batch 0..1 ran (MOC and the two player indexes restyled); the parent `README.md` was added.
- 2026-09-15 ZCode root migration: ZCode now opens this repository directly (matching Codex). `.zcode/` is gitignored and its 23 plan files were copied in from the parent workspace; the parent `AGENTS.md` redirect and old `.zcode/` stay as a dormant archive; ZCode memories were cloned to the new path key `guzhenren-template-1.21.1-f9ea652aa9d18197`.
- Codex project tools: JetBrains MCP is the optional Java semantic service; Context7 uses the Codex-only user environment credential; Serena and node_repl are disabled by `.codex/config.toml`. Project plugin preferences disable unrelated local/bundled plugins when the current runtime recognizes them. Plugin-injected services and workspace-managed remote plugins cannot all be overridden per project, so leave their global state untouched and invoke them only when the task actually matches. A fresh Codex subprocess on 2026-09-15 verified Context7 `resolve-library-id` and `query-docs`; JetBrains remains optional and was offline because IDEA's port was not listening. ZCode's separate Serena, JetBrains and Context7 configuration is unchanged. The dead global codegraph entry stays disabled.
- Awaiting Alex: 元泉 transparency experiment via `runClient` (verdict rule in 《蛊虫 与蛊材》); final texture, underwater fog, water feel, stone-gen pacing; wild boar/beetle acceptance per player TODO. Remaining wiki files restyle in later batches — the batch list lives in the MOC.
