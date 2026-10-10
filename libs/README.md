An optional local `CoreProtect-24.1.jar` can be placed here for compilation.
Without that file, Gradle uses API 24.1 from the official PlayPro Maven repository.

CoreProtect is a compile-only dependency and is not included in CPVisualizer.jar.
Local dependency JARs are excluded from Git and public source archives.
Use `-PcoreProtectFromMaven` to force the public Maven dependency.
