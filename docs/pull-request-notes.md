# Pull Request Notes and Conventions

This document is a short guide for contributors and future maintainers. It focuses on conventions that are simple, practical, and worth following in this repository.

## Before making changes

This repository uses a branch-per-Minecraft-version workflow. The default branch is the current supported Minecraft version branch, and pull requests should usually target the matching `mc-*` branch for the version they change.

Start by identifying which layer you are changing:

- common gameplay code in `src/main`
- client-only behavior in `src/client`
- generated data in `src/main/generated/data`
- static assets and translations in `src/main/resources`

Read the nearby code first and follow existing patterns before introducing a new abstraction.

## General conventions

### Keep changes aligned with existing architecture

Prefer the established patterns in this repository:

- `Mod*` registration classes
- `Supplier` accessors for registered instances
- identifiers built with `Identifier.fromNamespaceAndPath(Touristry.MOD_ID, ...)`
- runtime config for gameplay balance

### Avoid duplicating gameplay rules

- [placeholder]

### Keep client-only code out of common code

If it only exists for rendering, screens, JEI, Mod Menu, YACL, or client mixins, it should probably live in `src/client`.

### Preserve the server-authoritative config model

Gameplay balance should come from runtime config, not from hardcoded defaults scattered across item classes.

If you add a gameplay-affecting value:

1. add it to the config model
2. normalize it if needed
3. read it at runtime where behavior occurs

## Tourist experiences: expectations for changes

When modifying or adding a tourist experience:

- [placeholder]

If you add a new experience that participates in progression:

- add or update advancement datagen
- add matching language entries
- confirm the advancement text matches the actual trigger condition

## Advancements and datagen

Advancements are generated in code.

Guidelines:

- keep advancement ids, translation keys, and visible text in sync
- use custom triggers when the event is specific to the mod
- prefer separate triggers for clearly different gameplay events

Generated data:

- commit generated data under `src/main/generated/data` when it is the source-controlled output of datagen
- do not commit datagen cache files under `src/main/generated/.cache`

## Language and text changes

When editing `en_us.json`:

- make sure the title and description match the actual event or item
- watch for copy-paste mistakes
- keep terminology consistent across items, advancements, and messages
- additional translations into other languages are very welcome; contributors can submit them through GitHub issues even if they are not opening a full pull request

## Pull request scope

Smaller pull requests are easier to review and safer to merge.

Good pull requests usually:

- focus on one feature or one bug
- avoid unrelated cleanup
- update direct supporting assets and translations
- keep generated outputs in sync when datagen is involved
- target the correct Minecraft-version branch for the work, rather than assuming there is a permanent `main` branch

If a change is intentionally incomplete or staged for later work, say so clearly in the PR description.

## Suggested validation workflow

From the repo root:

```powershell
./gradlew.bat build --no-daemon
./gradlew.bat runDatagen
```

Use `runClient` when a change affects:

- screens
- rendering
- menu behavior
- client config UI
- Tourist visuals
- advancement behavior you want to test interactively

## Good habits for contributors

### Prefer small, readable helpers

If a piece of logic is repeated or hard to scan, a shared helper is usually better than another copy-paste block.

### Name things after gameplay meaning

Use names that describe the actual event or rule, not just the implementation detail.

Examples:

- [placeholder]

### Update related surfaces together

A gameplay change may require updates in several places:

- code
- config
- translations
- advancements
- JEI
- generated data

Try not to leave those out of sync.

## What reviewers should look for

When reviewing a PR in this repository, good questions include:

1. Does this follow the existing `Mod*` and tourist experience architecture?
2. Is gameplay logic duplicated anywhere it should not be?
3. Is client-only code staying in `src/client`?
4. Are config, translations, advancements, and generated data all aligned with the code change?
5. Are there obvious copy-paste mistakes in experience names, sources, or descriptions?

## Final note

The most important convention in this project is consistency with the existing design. A change that fits the current architecture is usually better than a clever new pattern that only appears once.
