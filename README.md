# Minecraft Server Plugin Boilerplate

This repository contains a minimal Paper plugin used as a starting point for your own projects. The build is configured for Java 17 but can be compiled with JDK 24.0.1 using Gradle's toolchain support.

## Requirements

- **JDK 24.0.1** installed and available in your `PATH`.
- **Git**
- **Gradle Wrapper** (`./gradlew`) included in this repo.
- **IntelliJ IDEA** (open the project as a Gradle project).

## Building

1. Clone the repository:
   ```bash
   git clone <repository_url>
   cd <repository_directory>
   ```
2. Build the plugin JAR:
   ```bash
   ./gradlew build
   ```
   Gradle will automatically download a Java 17 toolchain for compilation. The resulting JAR will appear at `build/libs/minecraft-server-1.0.jar`.

## Running a Local Paper Server

1. Download a Paper server JAR matching the plugin's API version (for example `paper-1.21.x.jar`) from [papermc.io](https://papermc.io/downloads/paper).
2. Place the JAR in an empty directory and start it once to generate the server files:
   ```bash
   java -jar paper-1.21.x.jar
   ```
   After it stops, edit `eula.txt` and change `eula=false` to `eula=true`.
3. Copy `build/libs/minecraft-server-1.0.jar` into the server's `plugins/` folder.
4. Start the server again:
   ```bash
   java -jar paper-1.21.x.jar
   ```
   You should see `MinecraftServer plugin has been enabled!` in the console.

## Joining from Minecraft

1. Launch a Minecraft client that matches the Paper version.
2. Go to *Multiplayer* → *Add Server* and set the address to `localhost` (port `25565`).
3. Join the server and type `/games` to open the plugin's menu:
   - **PVP 1v1** – join the duel queue.
   - **Parkour** – teleport to the parkour start.

## Using IntelliJ IDEA

1. Start IntelliJ and choose **Open** to load the project directory.
2. IntelliJ will import it as a Gradle project and download dependencies automatically.
3. Use the Gradle tool window to run `build` or run `./gradlew build` in the integrated terminal.

## Project Files

All files in `src/main/java` and `src/main/resources` are used by the plugin. There are no unused or temporary files in the repository.
