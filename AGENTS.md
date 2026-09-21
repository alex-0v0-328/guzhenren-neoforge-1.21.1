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
- Model tiers per environment — ZCode: main `GLM-5.3` (highest), sub `GLM-5.3-Flash` (highest); Codex: main `GPT-6-Astra Medium`, sub `GPT-5.6-Luna-Max`; Kimi Code (WSL): main default `kimi-code/k3-256k` (Alex hand-picks 1M `kimi-code/k3` for foreseeably huge tasks), subagent pool default `kimi-code/kimi-for-coding-highspeed` via `[secondary_model]` — environment notes in `.kimi-code/`.
- Plans carry a subagent-allocation section; infrastructure plans additionally state how current WIP is included and the onboarding rule for future work; new-feature plans end with a read-only deep self-check sweep (fix real findings, log false positives).
- Parallel work never shares write-files, and two Gradle runs never write the same output directory (retry on lock conflict, or serialize). Before rewriting the §6 State section, check recent commits (`git log` / `git show --stat`) for a parallel session's entries and merge — never clobber.

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

Exceptions (format yields to the task): `explain`-type requests get the full walkthrough, still without filler; destructive actions confirm first; after three failed fix rounds stop coding and question the assumption with one diagnostic question; real ambiguity gets one short clarifying question instead of a guessed rewrite. Verification facts (what compiled, what tested, what awaits Alex) are information, not ceremony — state them inline where the work is described, never as a mandatory closing block. Before hands-on work on a new mechanism or fix, ask two scoping questions up front — what counts as done, and how to treat numbers/mechanics Alex did not specify; never silently default (Alex, 2026-08-28). A rejected plan is a correction entry point, not a verdict: extract the fix and re-propose; two rejections in a row are normal. If a question to Alex goes unanswered (dismissed or timed out), stop and wait — dismissal is never assent, and the recommended option must not run on its own (Alex, 2026-08-30). When Alex later sends a corrected full spec, the newest message supersedes and the old implementation is deleted.

## 2. Boundaries

- Main project root `C:\alex\code\gzr-mod-dev\guzhenren-template-1.21.1` is the only Git repo. Its parent workspace is intentionally not a repo; check this repository's `status`/`diff` at task start and preserve checkouts and uncommitted WIP.
- Wiki `..\project-wiki` has no version control. Back up touched files to Temp and verify SHA-256 before editing; edits stay precise and reviewable; before deleting anything check content, references, tracking and use, and keep a recoverable backup. Backups are in-task safety nets — once the task's changes are verified and landed, delete them rather than hoarding (Alex, 2026-09-20). Same ritual for this file and the reference before rule edits.
- Alex edits files live in IDEA. On an edit conflict, re-read and identify the source; his wording changes stay as-is.
- Model authority `C:\alex\code\blockbench\gzr-models\` is read-only; exports land in the mod.
- Numbers live in code: Gu (蛊) specs read from `ModItems.GuSpec`, characters from their enums and services. The dev wiki explains design and reasoning; the player wiki is a version snapshot. One primary number location per audience; everything else references it.
- Undecided design and TODO are Alex's calls — never implemented ahead, never judged as defects. Scope is mortals and rank 1..5 (一转..五转); nothing shaped for stage 3 and later (ascension 仙, immortal Gu, rank 6..9) gets built or counted as missing. First explicit exception, commissioned by Alex on 2026-09-21: the Treasure Yellow Heaven dimension (appearance-only first version).
- Think like a Minecraft modder and a modpack player: gameplay flow, compatibility, maintenance cost. Review against Java types, state invariants, server authority and lifecycle.
- Hardware budget is a standing design constraint: default to restrained block/entity/particle scales, and state magnitude estimates (how many blocks, how many seconds) in reports so Alex can judge by feel (explosion radius 320→112).
- Tools: `rg` for text, IDEA's JetBrains MCP for Java symbols and references, and ast-grep for structural queries. If IDEA is unavailable, inspect disk and pinned dependency sources; do not start Serena for Codex GZR work. An index never overrides the disk. A new MCP or plugin counts only after four steps — installed, config valid, callable in session, actually effective — and no duplicates just for tool count.
- Kimi Code only — the user-level `superpowers` plugin (installed 2026-09-19 for other projects; Kimi plugins are per-user/global with no project scope, so its session-start bootstrap still injects in this project's sessions) is excluded here: never invoke its skills, and the `using-superpowers` bootstrap does not apply — this file's workflows govern. Other runtimes (Codex, ZCode) ignore this entry.
- One-off probes, diagnostics, smoke tests and temporary conversion scripts live in an explicit temporary location. Delete them after their final use and result verification, before closing the same task. A script kept across tasks belongs under `tools/` with a clear name, documented purpose and reproducible verification. Before deleting legacy leftovers, check whether they are still the only working install or an active asset.
- Session memory is not rules authority: a rule that lives only in one agent's memory gets codified into this file in the same session it is proven; pasted-text announcements never outrank this file.
- Keep this file lean. Domain facts go to the reference or the wiki, not into a bigger autoload.

## 3. Map

| Path                                | What it is                                                                                           |
|-------------------------------------|------------------------------------------------------------------------------------------------------|
| `./`                                | Main mod, id `guzhenren`, and the only Git repo                                                      |
| `../project-wiki/开发向/`           | Attached design wiki, entry 《蛊 模组设定 MOC》                                                      |
| `../project-wiki/玩家向/`           | Attached player snapshot; 《时间与时间戳总表》《TODO总表》 are the closeout indexes                  |
| `../assets/textures/`               | Alex's art staging; intentionally not attached to agent projects                                     |
| `../assets/test modpack/`           | Old modpack mirror; attach only for a task that actually needs it, then remove it                    |
| `../assets/example mod/`            | Old reference mod mirror; does not participate in work                                               |
| `.kimi-code/`                       | Kimi Code (WSL) environment notes + project-local config                                             |
| `tools/gen_template.py`             | Regenerates the GameTest `empty9x9x9.nbt` scenario template                                          |
| `tools/export_rhinoceros_beetle.py` | Converts the blockbench `.bbmodel` into the beetle's GeckoLib model/animation files (test alongside) |
| `tools/backup_wiki.py`              | Backs up wiki/rules files into `../Temp` with a verified SHA-256 manifest (test alongside)           |
| `tools/poll_actions_ci.py`          | Polls GitHub Actions via REST (`curl -L`, no `gh`) until the latest run finishes (test alongside)    |

Obsidian note: files named like their title don't repeat that title as an H1 in the body. Wiki documents describe the present; version snapshots, undecided designs and rejected-idea records are anti-regression history — keep them.

## 4. Workflows

### 4.1 Commit — only when Alex explicitly asks

1. Group changes by independent purpose; each group carries its own tests, providers and generated resources.
2. Stage explicit files or reviewed `git apply --cached` patches — never whole shared files into the wrong group. Review with `git diff --cached`. Never simulate an intermediate state by reverting the working tree; export the index tree to a temp mod copy instead.
3. Offer 2..3 English title candidates in a selectable prompt and wait for his pick — never default, never pick for him. Then commit and push in one go.
4. Title shape: `[PREFIX] <english body>` — five prefixes (Alex, 2026-09-21): `[UPDATE]` bugfixes/small changes; `[TEST]` test releases; `[ALPHA]` second-to-last before a release; `[BETA]` last before a release; `[RELEASE]` official release. Branch stays `main`; mod_version bumps are decided per release by Alex. No AI footers, no session lines.
5. Every commit includes its datagen output. Hard deps referenced straight from `run/mods` need their CI download step, otherwise CI goes red while local stays green.
6. Don't "fix" the old-name remote redirect. No force push, no history rewrite; check remote-ahead before pushing.
7. Known traps: line-ending noise stays out of commits (mechanics in reference §3); Gradle lock conflicts between parallel runs mean retry or serialize; commit status and Actions checks are separate queries — an empty status is not "CI green". There is no `gh` CLI on this machine — poll the Actions REST API with `curl -L` (the old-name remote 301-redirects) in a sleep loop; the full build+GameTest+runData chain takes ≈1.5–3 min. Report each commit SHA, title, push state and the real CI state.

Exception — CI red: when Alex brings a GitHub Actions failure, the loop locate → fix → commit → push → poll Actions to green runs without waiting for an explicit commit request — a red CI left unpushed counts as unfixed (2026-09-04 GeckoLib incident). The title-candidate step 3 still applies.

### 4.2 New content — new mechanics, entities, items, tools, weapons

1. Read the domain quick-ref first (reference §8 items and Gu, §5/§9 storage and refinement, §10 cultivation/time/Epic Fight) and find the existing pattern in the same seam — e.g. tendable Gu follows `TendedGuItem` — before inventing structure.
2. Registry wiring follows the existing DeferredRegisters. An entity arrives as a full set: EntityType, attributes, spawn predicates, biome tag/modifier, renderer, model/texture/animation, lang, loot. No default spawn egg, taming or breeding.
3. Models import read-only from the blockbench authority; verify export shape, UV and alpha. GeckoLib stays on the project's pinned version. Custom renderers never bypass invisibility or team-glow branches. Art division: Alex hand-draws final textures; the agent ships exact asset specs (vanilla reference, dimensions, mcmeta, target color, placement) plus a valid placeholder that passes runData validation, so finals drop in at the same path.
4. Any provider change means `runData`, then review the `src/generated` diff.
5. Tests by layer: real world/tick/damage/NBT → L3 GameTest; pure JVM math/spec → L1 `pureTest`; anything touching registry objects → L2 `test`. Boundary tests cover failure paths, not just the happy path.
6. Sync numbers on the spot: dev reasoning to the dev wiki, player values to the player snapshot (a `GuSpec` change reaches the player table the same session). Check both closeout indexes after every wiki touch, even when nothing needed changing. Advancement triggers hang off the "used successfully" landing point — commands never count. After touching any pinned wiki file, run `gradlew.bat test -PwikiDir=<abs path to project-wiki>` as the final gate — plain `test` skips `WikiConsistencyTest`. Registering a new item syncs three places: `WikiConsistencyTest.groups()`, the player table row, and the homepage count line. Wiki sync for the change runs the §4.5 impact chain.

### 4.3 Modification — any change, including when new content missed expectations or structure isn't right

1. Reproduce and locate the root cause before touching code. Fix ordering per the quadrant protocol: correctness, stability, coverage, verifiability first; brevity, diff size, cost last.
2. Preserve WIP: start from `git status`/`diff`; on conflicts re-read and identify the source; Alex's live IDEA edits stay as-is.
3. A symptom Alex reported is already proven — diagnose directly, don't ask him to reproduce it again. `runClient` is always his.
4. One entity report expands by default to the whole family in the same seam.
5. Snapshot tests pin current truth. A red numeric assertion goes to Alex to adjudicate intent; it is not auto-"fixed".
6. After the fix: regression first where feasible (red before green), then verification for the layers actually touched; `runData` if providers moved. Wiki sync for the change runs the §4.5 impact chain.

### 4.4 Review — standards compliance, optimization, cleanup

1. Read `git diff HEAD` plus untracked files; build a change-coverage table by purpose; check the call context. Separate new defects from pre-existing adjacent ones from undecided design. Every finding states trigger, path/line, impact and a minimal fix.
2. Numeric review reads whole expressions: saturating addition doesn't protect a later multiplication; clamping before an add can flip the sign. Check zero, negative, MIN/MAX and the normal range — and don't copy the same wrong formula into a test.
3. Lifecycle review: goal interruption, pathing failure, target death, damage rejection, NBT restore, late tracking. Stopping an action must leave a reachable successor state.
4. Bulk reformat or rename is not cleanup. Deliberate signatures and kept behaviors follow the reference (§13 do-not-touch list; ⚠ KEPT stays). Files get deleted only after checking references, tracking, use and activity, with a hashed backup. Never run a whole-repo `git clean`; never touch `run/mods`, saves or fresh test evidence.
5. ast-grep answers structural queries (syntax match ≠ type proof). Library API doubt → the pinned dependency's sources. Report substantive findings, what actually changed, and what stayed unverified.
6. A "confirm X exists/works" request is a full-chain verification: trace both the write end and the read end and answer with line-number evidence — docs and a method's own Javadoc can lie (`REFINED_AT` cooldown, unreachable `openSecondary` branch). Wiki count claims are re-counted, never trusted.

### 4.5 Wiki sync — every code change runs the impact chain (Alex, 2026-09-21)

1. Read the current `git diff` first; never re-scan or regenerate the wiki wholesale. Trace only affected knowledge: Git Diff → Changed Code → Affected System → Affected Concept → Affected Wiki → Affected Design → Affected Lore → Affected Decision.
2. Classify the diff: added / removed / modified code; API, registry, data, event, networking, gameplay, resource, architecture changes.
3. Impact analysis before any wiki edit. Report: Changed Code / Affected Wiki Pages / Outdated Information / New Information / Affected Design / Affected Lore / Implementation Gap / Potential Conflict / Needs Review.
4. Editing discipline: keep existing content; touch only affected parts; never regenerate whole pages; never delete decision history or unimplemented Design; never bend Lore to match code; never invent lore. Unconfirmable → `[NEEDS REVIEW]`, left for Alex.
5. After the edits re-check broken/outdated links, source paths, class names, registry names and cross-domain relationships, then close with: Updated Pages / Unchanged Pages / Needs Review / Implementation Gaps / Conflicts. The §4.2.6 `-PwikiDir` gate still applies as the final test.

## 5. Language and style

Chat in Chinese; code, comments and commit messages in American English. Terms come from the language provider / `en_us.json` / wiki《原著词汇 与命名》— never invent bilingual pairs (known trap pairs in reference §2). Formatting follows `.editorconfig` and neighboring code; comments explain non-obvious reasons only; no bulk reformat, no project-wide IDEA "cleanup code". Substantive conventions (`@NotNull`/`@Nullable`, import ordering, Javadoc FQN rules) live in reference §2.

## 6. State

- Spirit spring (元泉, `spirit_spring`) first version committed and pushed: `3dce895`, 28 files. Texture is a tinted vanilla-water placeholder awaiting Alex's #4FC3F7 pass; design notes in 《蛊虫 与蛊材》.
- The 2026-09-14/15 WIP batch (build.gradle `wikiRoot` rename, GameTest template generator, rules-entry move, Codex tooling migration, `.zcode` gitignore) landed as commits `1357c0f`..`28e3950`.
- 2026-09-14 rules pass: a dead side mod (zero-connected, references verified clean) was deleted along with its wiki pages; this file was rewritten in English and moved into the main repository as the self-contained rules entry; the reference was slimmed; wiki rewrite batch 0..1 ran (MOC and the two player indexes restyled); the parent `README.md` was added.
- 2026-09-15 ZCode root migration + deep clean (Alex signed off): ZCode now opens this repository directly (matching Codex); `.zcode/` is gitignored and its plan archive was emptied. Parent-workspace dead weight removed — the `AGENTS.md` redirect, old `.zcode/` and `.opencode/`, and a stale 1.8 GB `.gradle-user-home`. ZCode memories live only under path key `guzhenren-template-1.21.1-f9ea652aa9d18197` (three stale keys deleted after a byte-identical clone check; hashed Temp backup `gzr-deepclean-20260915`). Repo leftovers `.superpowers/`, `bin/`, `.eclipse/` and `tools/__pycache__/` removed as well.
- Codex project tools: JetBrains MCP is the optional Java semantic service; Context7 uses the Codex-only user environment credential; Serena and node_repl are disabled by `.codex/config.toml`. Project plugin preferences disable unrelated local/bundled plugins when the current runtime recognizes them. Plugin-injected services and workspace-managed remote plugins cannot all be overridden per project, so leave their global state untouched and invoke them only when the task actually matches. A fresh Codex subprocess on 2026-09-15 verified Context7 `resolve-library-id` and `query-docs`; on 2026-09-18 both were re-verified callable in-session — JetBrains resolved the project path and listed open editors, Context7 `resolve-library-id` returned NeoForge docs — so JetBrains is online whenever IDEA is running, optional otherwise. ZCode's separate Serena, JetBrains and Context7 configuration is unchanged. The dead global codegraph entry stays disabled. Since 2026-09-20 the agent tooling dirs (`.codex/`, `.agents/`, `.kimi-code/`, `.zcode/`) are gitignored and untracked: local files stay on disk, GitHub carries mod code only (Alex).
- 2026-09-15 code↔wiki alignment audit + fix (scope approved by Alex): three-domain subagent sweep re-verified by the main agent; ~17 drifts closed across 13 wiki pages (liquor-worm hunger numbers, human-strength channel cap 360k, refine-charge ladder wording, hunger-warning threshold, 102→103 items, three creative tabs, `Aperture` 13 active fields, 58 GameTests / 12 registers / 12 providers (counts as of that day), spirit-spring `block/` note, dragonpill cricket jump +0.2 per code, modpack snapshot 54/55); unreachable `HALF_ZOMBIE_REGEN_RATE` removed from `ApertureEssenceService`; full pre-edit wiki backup at `Temp/gzr-wiki-full-backup-20260915-pre-audit-fix/` with SHA-256 manifest.
- 2026-09-15 wiki four-block alignment (Alex's model): player page 《空窍&肉体&脑海&魂魄》 split into 空窍/魂魄/肉体/脑海 block pages in that order (storage stays inside the 空窍 block; cross-block sleep/death and path/qi/strength entries moved to the overview page); dev-side `魂魄.md` promoted to its own top-level `魂魄/` block, MOC regrouped, 《世界模型 三域》 rewritten and renamed 《世界模型 四块》 (player reads four blocks, code keeps five packages); `WikiConsistencyTest` BODY_PAGE split into APERTURE_PAGE/MIND_PAGE. Backup `Temp/gzr-wiki-full-backup-20260915-pre-fourblock-split/` with SHA-256 manifest.
- 2026-09-18 deep rules audit (Kimi Code session; sources: live code, wiki, Codex+ZCode session histories — Claude Code has zero GZR content; its stray WSL `~/.claude` was removed per Alex). Drifts fixed: reference item total 102→103 (+spirit-spring BlockItem), ModCriteriaTriggers ordinal (12th of 12 holders / 13 registers, `ModRecipes` owns two — as of that day; particles later made it 13/14), `build.gradle` test inputs now track the split 空窍/脑海 pages, §3 map + parent README refreshed. Wiki terminology unified — 块 = player reading (four blocks), 包 = code reading (five packages), 域 retired (retirement note in 《世界模型 四块》); 9 pages touched. New rules: §4.1 CI-red exception + REST polling (no `gh`), §1.3 question-timeout discipline, §1.2 plan-document requirements, §4.2.6 `-PwikiDir` gate + three-place item sync, §4.4 full-chain verification, §2 hardware budget, §4.2.3 art division, reference §3 GameTest fixture traps + CRLF viewpoint trap. Kimi environment documented in `.kimi-code/`. Backups: `Temp/gzr-agents-audit-fix-20260918/` with SHA-256 manifest.
- Awaiting Alex: 元泉 transparency experiment via `runClient` (verdict rule in 《蛊虫 与蛊材》); final texture, underwater fog, water feel, stone-gen pacing; wild boar/beetle acceptance per player TODO. Wiki restyle batches 0..7 (41 pages) all completed 2026-09-14 per the MOC.
- 2026-09-21 Treasure Yellow Heaven batch (first stage-3-shaped content, explicitly commissioned by Alex; commit prefix `[TEST]`, branch main, mod_version stays 1.0.0): first custom dimension `guzhenren:treasure_yellow_heaven` — void flat worldgen (zero layers), yellow sky/fog #F4D35E (`ModPalette.TREASURE_YELLOW_HEAVEN` + biome colors + `client/dimension/TreasureYellowHeavenEffects`), clouds/sun/moon/day-night kept, no precipitation; fixed spawn (0.5,64,0.5); mayfly granted inside and restored on exit; void-damage immunity + below-min_y rescue teleport. `/guworld enter <anchored dimension>|exit` (permission 2; separate world-environment root, allow-list `ModDimensions.ANCHORED_DIMENSIONS` — entries carry rank, TYH = rank 8 grotto-heaven). Traveller's Titles compat needs no dependency: dimension title reads `travelerstitles.guzhenren.treasure_yellow_heaven` (+`.color` = f4d35e), biome title falls back to the vanilla `biome.*` key — all provided. Reusable core for future Blessed Land / Grotto-Heaven: `DimensionTravelService` + `DimensionReturnData` (12th attachment `dimension_return`, serialized not synced, cleared on death/reset via `PlayerDataService`). Guards in `event/dimension/TreasureYellowHeavenGuardEvents` cancel break/place/interact/attack (incl. EF via LivingIncomingDamageEvent source check)/toss; all six action payloads gated in `ModPayloads.inTyh`; B-key screen blocked client-side. Tests: L2 26 classes/99, L3 4 classes/68 — trap: GameTestServer builds its world from the FLAT preset so datapack-dimension ServerLevels never exist in L3; only branch/reverse-sanity coverage is possible, real cross-dimension travel awaits Alex runClient (player TODO entry). Versioning rule recorded in §4.1: five commit prefixes `[UPDATE]/[TEST]/[ALPHA]/[BETA]/[RELEASE]`.
- 2026-09-19/20 shockwave-ring batch (committed 2026-09-20 as three commits — feature / agent-tooling removal / docs; the dated iteration narrative lives in 《测试集》's 2026-09-19/20 entries): commits `aed2dec`/`78af111` pushed the 2026-09-18 audit first (CI green); dash distance ×3→×4.5 as `CrashGuEffect.DASH_COORD_SCALE` (moved out of the EF bridge so pure/L2 tests can pin it); first particle types — 13th DeferredRegister `ModParticles` + 13th provider `ModParticleDescriptionProvider`: `shockwave_ring` (dash trail, self-hidden from the dasher's own first person) and `impact_ring` (punch trail), both playing Alex's five hand-drawn rings (`textures/particle/ring_*x*.png`) small→large through the additive two-sided `ADDITIVE_GLOW` render type (after the first runClient round: faint alpha blend → additive glow; a `tick()` off-by-one crashed the first dash — frame index and guard must read `age` on the same side of the increment). `particle/RingConeEmitter` (PlayerTickEvent, per-effect burst maps) plants rings: the dash trail drops one every `RingTrailSpacing.SPACING` (2 blocks) of measured travel (EF locks movement during dodges, so per-tick drops clustered), the punch trail one per tick along the punch ray from the victim's hitbox center (8 rings ≈ 16 blocks); a dash never restarts inside its window (double-trigger fix), a punch restarts per hit, and the two trails coexist. Ring mechanics (`client/particle/RingParticle` + `RingGeometry`): vertical plane ⊥ motion (FACING_MOTION; spawn velocity doubles as the facing normal — zero trips the ground fallback, hence the 0.03 b/t carrier), 30° min opening toward the camera when edge-on (`MIN_OPENING_DEGREES`), frame sizes lerp between ticks, lifetime `FRAME_COUNT + RING_LINGER_TICKS` (3) holds the bloom. Punch hook in `EpicFightServerEvents` via `LivingIncomingDamageEvent` — EF `ComboAttackEvent` exposes no target — gated to EF mode + FIST category + attack panel ≥16 (`BodyAttackService.IMPACT_RING_ATTACK_THRESHOLD`); dash ring yaw corrects EF model yaw → motion yaw (backward combos flip 180, `DashRingYawTest`). Pure-visual, no AoE (the real shockwave design stays undecided). Tests: L1 27/134, L2 25/96. Wiki synced per round (力道更新 / 测试集 / 架构总览 / TODO总表). Agent tooling dirs (`.codex`/`.agents`/`.kimi-code/`.zcode`) gitignored and removed from the index (Alex: GitHub carries mod code only); local files stay. Awaiting Alex runClient: even spacing, F5 rear+backward / front+forward visibility, punch tail length, first-person self-hide.
