# Changelog

## 1.1.1 - 2026-10-10

- Target Paper 26.3 and Java 25, with Paper API pinned to `26.3.build.169-beta`.
- Translate all CPVisualizer menus, messages and public documentation to English.
- Replace personal player examples and Java package names with neutral names.
- Add the MIT License for CPVisualizer.
- Support `-PcoreProtectJar` for testing against a separately built compatibility port.
- Preserve all-player/all-block searches, API 12 integration and private highlights.
- Verify both plugins on Paper 26.3 with real SQLite queries, cross-world searches, container history and rollback/restore.
- Pass 14 automated tests against both supported CoreProtect dependency forms.

## 1.1.0 - 2026-10-08

- Search all materials using `/cpv block all`, `/cpv blocks all` or `*`.
- Search all recorded online and offline players with `/cpv player all`.
- Add all-player and all-block GUI selectors and tab completion.
- Support the `/cpv bloks all` alias.
- Exclude CoreProtect system actors from all-player searches.
- Adapt global block searches to API 12: positive radius, loaded-world queries and a shared result limit.
- Retain unresolved historical containers with unknown material in all-block results.
- Make discovered players available to the player selector.
- Use the official CoreProtect Maven API artifact in public builds.
- Add tests for global searches and publication documentation.

## 1.0.0 - 2026-10-02

- Initial release: exact player/block searches, inventory GUI, private highlights, navigation, limits, cancellation and historical container inference.
