# Publishing CPVisualizer

Current release: **1.1.1**, targeting Paper 26.3, Java 25 and CoreProtect API 12.
The historical 1.1.0 release targets Paper 26.2.

## Public package

Publish the plugin JAR, source archive, English documentation and checksums.
Exclude local CoreProtect dependencies, build/cache folders, local filesystem paths, credentials and server/player data. CoreProtect is installed separately and is not bundled inside CPVisualizer.

Public checkouts use the official `net.coreprotect:coreprotect:24.1` Maven API dependency. CPVisualizer is MIT licensed; CoreProtect retains its own license.

Select Paper 26.3 only for this release, choose Admin Tools and Addon categories where available, and mark CoreProtect as required. Do not claim Folia compatibility or completed two-client visual testing.

## Development disclosure

The implementation and documentation were generated with AI from project requirements and reviewed through builds, automated tests and server integration checks. The plugin itself does not call an AI service.

Public discovery on Modrinth is not permitted for this implementation under its current AI-content policy. Other submissions must accurately answer any required development disclosures.

- [Modrinth AI policy](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai)
- [Hangar guidelines](https://hangar.papermc.io/guidelines)

Actual upload URLs and moderation states are recorded separately after publication. A prepared archive or successful build is not evidence of remote publication.
