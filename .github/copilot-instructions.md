# Copilot instructions for Touristry

## Build and test commands

- Full build: `.\gradlew.bat build --console=plain --no-daemon`
- Full test task: `.\gradlew.bat test --console=plain --no-daemon`
- Single test class: `.\gradlew.bat test --tests "org.bensam.touristry.YourTestClass" --console=plain --no-daemon`
- Single test method: `.\gradlew.bat test --tests "org.bensam.touristry.YourTestClass.yourTestMethod" --console=plain --no-daemon`
- Run the client dev environment: `.\gradlew.bat runClient`
- Run the dedicated server dev environment: `.\gradlew.bat runServer`
- Run datagen: `.\gradlew.bat runDatagen`

`test` is wired into Gradle, but the repository currently has no `src/test` sources, so it completes with `NO-SOURCE` until tests are added.

## High-level architecture

Touristry is a Fabric mod for Minecraft `1.21.11` using Java 21 and Fabric Loom. The repo follows a branch-per-Minecraft-version workflow; versioned work should target the matching `mc-*` branch, not an evergreen feature branch.

The codebase is intentionally split by environment:

- `src/main` contains common/bootstrap code, registries, commands, config definitions, and payload types that must remain safe on both client and server.
- `src/client` contains client-only initialization, config UI, packet receivers, Mod Menu integration, YACL integration, JEI integration, and datagen entrypoints/providers.

Entrypoints are declared in `src/main/resources/fabric.mod.json`:

- `org.bensam.touristry.Touristry` is the common bootstrap.
- `org.bensam.touristry.client.TouristryClient` is the client bootstrap.
- `org.bensam.touristry.client.TouristryDataGenerator` is the Fabric datagen entrypoint.
- JEI and Mod Menu integrations are separate optional entrypoints.

Common startup flows through `Touristry.onInitialize()`, which registers the mod subsystems in a fixed order: advancements, stats, components, items, blocks, block entities, menus, networking, synced client config, creative tab, then commands. Server config is not loaded at static init time; it is initialized when the overworld loads via `ServerWorldEvents.LOAD`, then synced to joining players through `ModServerConfigSync`.

Configuration is split across both sides:

- Server config lives under the world save at `data\touristry\server-config.json5` and is managed by `ModServerConfigManager`.
- Client config lives in Fabric's config directory as `touristry-client-config.json5` and is managed by `ModClientConfigManager`.
- The server sends authoritative server config to clients with `SyncServerConfigS2CPayload`.
- The client sends selected preferences back to the server with `SyncClientConfigC2SPayload`, currently for `verboseTooltips`.
- The `/tour config reload` and `/tour config reset` commands reload/reset server config and immediately resync connected players.

Optional integrations are isolated instead of leaking through common code. `ModMenuIntegration` only exposes the YACL config screen when YACL is actually loaded, and JEI support is contained in the JEI plugin entrypoint.

Datagen support is enabled in Gradle and wired in `fabric.mod.json`, but `TouristryDataGenerator` is currently empty. The design notes treat generated output under `src/main/generated/data` as datagen artifacts rather than hand-authored source.

## Key conventions

- Follow the environment split strictly: gameplay/common logic belongs in `src/main`; screens, mixins, JEI, Mod Menu, YACL, client packet handlers, and datagen wiring belong in `src/client`.
- New registries should follow the existing `Mod*` pattern (`ModItems`, `ModBlocks`, `ModMenus`, `ModNetworks`, etc.) with an `initialize()` method called from the bootstrap instead of ad hoc registration from unrelated classes.
- Use `Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "...")` for mod identifiers rather than hard-coded namespace strings.
- Registration helpers keep the registration steps explicit. For items and blocks, create the `ResourceKey`, set it on the properties, instantiate the object, then register it.
- Keep server/client config synchronization explicit. If a gameplay feature depends on config, wire both persistence and sync behavior instead of reading client config directly from common code.
- Preserve the existing error-handling approach in config managers: malformed config falls back to defaults in memory, but the bad file is left on disk so it can be fixed manually.
- Optional dependency behavior should stay guarded behind mod-presence checks or dedicated optional entrypoints rather than unconditional references from common bootstrap code.
- The repository is still scaffold-heavy in places; commented-out examples in `ModItems`, `ModBlocks`, `ModMenus`, `ModBlockEntities`, and related classes are the current template for how future content is expected to be added.
