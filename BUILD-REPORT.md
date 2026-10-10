# Validation report: Paper 26.3

Date: 2026-10-10. CPVisualizer 1.1.1 and unofficial CoreProtect 24.1-paper26.3.1.

## Builds and automated tests

- Java 25, Gradle 9.1.0, Maven 3.9.11; pinned Paper API `26.3.build.169-beta`.
- CoreProtect built successfully with Maven. The upstream v24.1 tag contains no Java unit tests; runtime checks were performed separately.
- CPVisualizer passed 14 automated tests against the ported CoreProtect JAR and the official Maven API artifact.
- The final publication build translates menus and messages to English and uses a neutral Java package. It is rebuilt and retested before uploading.

## Actual Paper server checks

Both plugins were verified on a disposable Paper 26.3 build 169 server with SQLite:

- Plugin startup, CoreProtect API 12 and the explicit 26.3 adapter.
- History writes and reads through CPVisualizer's actual CoreProtectHook.
- All-player/all-block filters, exact player/material filters, exclusion of system actors, and merging two loaded worlds.
- A transaction of three diamonds in a chest, resolved as historical container material CHEST.
- Rollback and restore of STONE, SULFUR_SPIKE, POTENT_SULFUR and CHEST.
- BlockDisplay creation and removal with public visibility and persistence disabled.
- Clean shutdown and database persistence across restarts.

Result: `PORT_SMOKE_PASS`. Event data was recorded through the API and server objects without connected game clients.

## Verification limits

Two-client GUI and private-marker rendering, events triggered by real players, player teleportation, MySQL, WorldEdit and Folia were not tested. See [TESTING.md](TESTING.md).

This CoreProtect build preserves v24.1 behavior and database format. New entity tracking from upstream master, including cushion history, is not backported.

## Historical 1.1.0 build

The Paper 26.2 release passed the same 14 automated tests against both a local CoreProtect 24.1 dependency and the official Maven API artifact. Its original JAR remains unchanged.
