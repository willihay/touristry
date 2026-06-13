# Touristry Design Notes

This document is a design and architecture overview for Touristry. It is written for future maintainers, especially future me, and for anyone who wants to understand the project before forking it or contributing changes.

## Project shape

Touristry is a Fabric mod for Minecraft `1.21.11` and later.

The repository uses a branch-per-Minecraft-version workflow. In practice, the default branch is the current supported version branch, and longer-lived work should happen on version branches such as `mc-1.21.11` rather than on a separate evergreen branch.

The codebase is split by environment:

- `src/main` contains common gameplay code, registries, items, blocks, block entities, menus, networking payload definitions, config, stats, and advancement triggers.
- `src/client` contains client-only setup, screens, rendering and mixin code, JEI integration, Mod Menu integration, YACL integration, and datagen entrypoints/providers.

Entrypoints are declared in `src/main/resources/fabric.mod.json`:

- `Touristry` for common initialization
- `TouristryClient` for client initialization
- `TouristryDataGenerator` for Fabric datagen
- `JEIPlugin` for JEI integration
- `ModMenuIntegration` for Mod Menu

## High-level gameplay model

[placeholder]

## Bootstrap and registration layout

`Touristry.onInitialize()` is the main bootstrap path. It initializes:

- advancements
- stats
- data components
- items
- blocks
- block entities
- menus
- networking
- synced client config
- the creative tab
- commands

Server config is initialized when the overworld loads, then synced to players separately.

The code follows a `Mod*` registration pattern:

- `ModItems`
- `ModBlocks`
- `ModBlockEntities`
- `ModMenus`
- `ModAdvancements`
- `ModNetworks`
- `ModStats`
- and similar classes

Registered instances are stored in private static fields and exposed through `Supplier`s. This keeps initialization order explicit and avoids early static access problems.

## Tourist architecture

### State machine pattern: Pre-compute prerequisites in transition methods

Tourist AI uses a state machine (`TouristMind`) with states like `CHOOSING_EXPERIENCE`, `TRAVELING`, `EXPERIENCING`, etc. 

**Architectural rule**: Pre-compute prerequisites (such as `ServerLevel` or `TouristBeaconBlockEntity`) in the calling method (typically `transitionTo()`) rather than computing them inside each state handler method.

**Rationale**:

1. **Error handling before transition** — Validate that the beacon exists, the world is available, etc. before entering the state. If prerequisites fail, abort the transition gracefully instead of ending up in a partially-changed state.

2. **Testability** — State handler methods become pure logic functions with explicit dependencies. This makes them easy to unit test with mock data without constructing full Tourist entities.

3. **Clear contracts** — Method signatures document what data each state needs. Reduces hidden dependencies on `this.tourist` or `this.beaconTarget` class fields.

4. **Reusability** — State logic can be called from multiple contexts and isn't tightly coupled to specific class fields.

**Example pattern**:

```java
private void transitionTo(TouristState newState) {
    // Pre-compute prerequisites ONCE
    ServerLevel serverLevel = (ServerLevel) tourist.level();
    TouristBeaconBlockEntity beacon = beaconTarget != null 
        ? TourismManager.getBeaconBlockEntityByUUID(serverLevel, beaconTarget)
        : null;
    
    // Validate before transition
    if (newState.requiresBeacon() && beacon == null) {
        LOGGER.warn("Cannot transition to {} - beacon not found", newState);
        transitionTo(TouristState.WANDERING);
        return;
    }
    
    currentState = newState;
    
    // Pass to state handler
    switch (newState) {
        case CHOOSING_EXPERIENCE -> chooseExperienceOrWander(serverLevel, beacon);
        // ...
    }
}

private void chooseExperienceOrWander(ServerLevel serverLevel, TouristBeaconBlockEntity beacon) {
    // Pure logic - prerequisites guaranteed, no null checks needed
    List<SightseeingExperience> experiences = 
        TourismManager.getLoadedTouristExperiencesByBeaconUUID(serverLevel, beacon.getUUID());
    // ...
}
```

**Exception**: Trivial getters like `tourist.position()` or `tourist.getBoundingBox()` can be computed locally in state methods since they have no failure cases.

## Config architecture

[placeholder]

## Tourist beacon design

[placeholder]

## Source layout by responsibility

These files are good entry points when exploring the mod:

| Area | File                                                                                        |
| --- |---------------------------------------------------------------------------------------------|
| Common bootstrap | `src/main/java/org/bensam/touristry/Touristry.java`                                         |
| Item registration | `src/main/java/org/bensam/touristry/ModItems.java`                                          |
| Client config | `src/client/java/org/bensam/touristry/client/config/ModClientConfigManager.java`            |  
| Server config | `src/main/java/org/bensam/touristry/config/ModServerConfigManager.java`                     |
| Datagen entrypoint | `src/client/java/org/bensam/touristry/client/TouristryDataGenerator.java`                   |
| Advancement datagen | `src/client/java/org/bensam/touristry/client/datagen/advancement/AdvancementGenerator.java` |

## Practical design rules

These rules match the current architecture and are worth preserving unless there is a good reason to change them.

### 1. Keep client-only code in `src/client`

Screens, JEI, render helpers, Mod Menu hooks, YACL hooks, and client mixins belong in `src/client`.

### 2. Keep gameplay logic in `src/main`

Tourist behavior, registries, items, blocks, menus, stats, triggers, and server-safe networking declarations belong in `src/main`.

### 3. Use runtime config for gameplay balance

Avoid baked balance values or other static registration metadata.

### 4. Preserve identifier and registration patterns

Use `Identifier.fromNamespaceAndPath(Touristry.MOD_ID, ...)` and the existing `Mod*` registration style for new registered content.

## Build, test, and datagen notes

Useful commands from the repo root:

```powershell
.\gradlew.bat build --no-daemon
.\gradlew.bat runClient
.\gradlew.bat runDatagen
```

Notes:

- `test` currently exists but the repository has no `src/test` sources.
- `runDatagen` writes generated data under `src/main/generated/data`.
- generated datagen cache files under `src/main/generated/.cache` are artifacts from the datagen process and should not be treated as source.

## When adding a new tourist experience

Use this checklist:

- [placeholder]

## Why this structure exists

The mod has grown around a few goals:

- keep gameplay behavior centralized and understandable
- avoid duplicated rule definitions across UI, JEI, and gameplay layers
- let server owners control balance through config
- keep client-only integration isolated
- make new tourist experiences follow a repeatable pattern

If future changes preserve those goals, the project will stay easier to maintain.
