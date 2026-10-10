# Testing

Automated tests cover duration parsing and overflow, filter selection, historical container inference, all-player API parameters, inclusive X/Z radius bounds, captured search centers, world merging with a shared result limit, exclusion of system actors, unresolved container types, and cancellation before API calls.

```sh
sh gradlew clean build -PcoreProtectFromMaven
```

On Windows use `gradlew.bat`. These tests use mocked API responses and do not replace database or game-client checks.

The 2026-10-10 Paper 26.3 build 169 runtime checks exercised real SQLite history, both plugin startups, searches across two worlds, container inference, rollback/restore, and private BlockDisplay creation. See [BUILD-REPORT.md](BUILD-REPORT.md).

## Manual test checklist

Use a disposable Paper 26.3 server, Java 25 and a compatible CoreProtect installation with API 12 enabled.

1. Join with two operators and one ordinary player. The ordinary player must not open `/cpv`.
2. Place, interact with, fill, empty and break a chest; leave a few seconds between actions. Verify each action filter after CoreProtect flushes its queue.
3. Repeat with a barrel and replace a chest with a barrel at the same position. Historical materials should match each period.
4. Start independent searches with both operators. Each sees only their own highlights. A third client joining later must not see them.
5. Log out one player and search their historical name with `/cpv player ExamplePlayer`.
6. Check world, all-world and radius scopes in two worlds, including negative coordinates and different heights. Radius bounds form an X/Z square.
7. Record more than 45 events. Check pagination, next/previous wrapping, details and selection. GUI shift-clicks, hotbar swaps and drags must not take items.
8. Check teleportation in spectator and ordinary modes, unavailable worlds, world borders, blocked destinations, cancelled teleports and revoked permissions.
9. During a search start another search, clear results, log out or reload configuration. Stale responses must not restore highlights or reopen menus.
10. Lower query and result limits. Check bounded requests, queue rejection and notices that results may be incomplete.
11. Check age colors, action-bar details, expiry, world changes, unloaded chunks and permission loss.
12. Restart the server. Visualization entities must not persist.
13. Remove CoreProtect or disable its API. CPVisualizer must disable cleanly.
14. Test invalid startup configuration and an invalid reload. A failed reload must retain previous valid settings.
15. Search `/cpv player all` and `/cpv block all` for two players and two materials; compare exact filters.
16. Check `/cpv blocks all`, `/cpv bloks all`, `*`, GUI selectors and tab completion. Exact selection must restore its filter.
17. Check the shared all-world result limit and the notice about loaded-world coverage in all-player block searches.
18. With no container block history, all-block searches should retain the transaction as an unknown container. A CHEST filter must exclude it; transaction items must not masquerade as container material.

Completion of these manual scenarios is not implied by a successful build or server-only integration test.
