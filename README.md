# JavaGameRepo

This is the source code for the game.

Requirements:

* Java 21 to compile the project
* Java 8 or newer to run the portable JAR
* A recent Gradle installation, or let the installer scripts place one on your `PATH`

The build scripts will automatically bootstrap Gradle 9.2.0 when they detect an older Gradle on your machine.

If you do not have Gradle installed yet, use one of the helper scripts in `scripts/`:

* `scripts/install-gradle.sh`
* `scripts/install-gradle.ps1`
* `scripts/install-gradle.bat`

## Build

Use the automation scripts in `scripts/`:

* `scripts/compile.sh`
* `scripts/compile.ps1`
* `scripts/compile.bat`

Each one builds a portable jar at `dist/xenoverse-portable.jar`. Gradle's
temporary files live in your user cache, so the scripts do not require `sudo`.

## Run

Use the matching run script for your platform:

* `scripts/run.sh`
* `scripts/run.ps1`
* `scripts/run.bat`

Each run script rebuilds the portable jar first, then launches it with `java -jar`.

On another Windows machine, open Command Prompt in the JAR's directory and run
`java -jar xenoverse-portable.jar`. Running it this way keeps any Java or graphics
driver error visible; double-clicking a JAR may only show "A Java Exception has
occurred" without the underlying message.

The compile scripts also create `xenoverse-portable.jar.sha256`. On Windows,
`certutil -hashfile xenoverse-portable.jar SHA256` can confirm that the copied
JAR matches the newly built artifact.

On Linux, the game automatically uses XWayland when `DISPLAY` is available to
avoid GLFW/EGL compatibility problems in some native Wayland sessions. Override
the selection when needed with `XENOVERSE_GLFW_PLATFORM=x11` or
`XENOVERSE_GLFW_PLATFORM=wayland`.

Run all scripts as your normal user. Using `sudo` for a project build can leave
generated files owned by root and is neither required nor supported.
