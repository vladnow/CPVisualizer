# Publishing CPVisualizer

Version: **1.1.0**. Target: Paper 26.2, Java 25, CoreProtect 24.1 / API 12.

## Distribution pages

- [GitHub repository](https://github.com/vladnow/CPVisualizer)
- [GitHub release 1.1.0](https://github.com/vladnow/CPVisualizer/releases/tag/v1.1.0)
- [Hangar project](https://hangar.papermc.io/vladnow/CPVisualizer)
- [Hangar version 1.1.0](https://hangar.papermc.io/vladnow/CPVisualizer/versions/1.1.0)

The Hangar download was retrieved and its SHA-256 matched the GitHub release JAR:
`32a58bf2f010779b2873ddc77f1cf978caa4f704b759b122de377ca06420ddb8`.

The current README uses English documentation and neutral player examples.
The release's updated versioned source archive includes that README. The original
tag and original release assets remain historical snapshots.

## Packaging

Publish only CPVisualizer's JAR and the public source archive. Exclude local
CoreProtect JARs, build output, Gradle caches, Git data, and credentials.
Web uploads do not automatically honor `.gitignore`.

CoreProtect is installed separately. Public checkouts compile against the official
`net.coreprotect:coreprotect:24.1` Maven artifact when no local JAR is present.
Paper, CoreProtect, and test libraries are not bundled in the plugin JAR.

Use Admin Tools and Addon categories where available. Select only Paper 26.2,
and mark CoreProtect as a required dependency. Do not claim Folia compatibility
or completed in-game testing. Preserve the result-limit and loaded-world
restrictions described in README.md.

No project license has been selected. Hangar lists the license as Unspecified.

## Other platforms

SpigotMC and CurseForge require an authenticated author account before uploading.
No authenticated session was available for either platform during this publication.

Modrinth's current policy does not allow public publication of projects primarily
generated with AI. This implementation was generated from project requirements
using AI, so it has not been submitted for public discovery on Modrinth.
Do not omit or misrepresent the development process in a submission.

- [Modrinth AI policy](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai)
- [Hangar resource guidelines](https://hangar.papermc.io/guidelines)
- [Hangar publishing documentation](https://docs.papermc.io/misc/hangar-publishing/)

Run the server checklist in TESTING.md before relying on the plugin for live
investigations. Automated builds and tests do not replace a two-client server test.
