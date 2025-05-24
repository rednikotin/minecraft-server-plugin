# Minecraft Server Plugin Boilerplate

This project is a boilerplate for creating a Minecraft server plugin using the Paper API.

## Prerequisites

- Java Development Kit (JDK) - Version 17 or higher (this project uses Java 17 as specified in `build.gradle`).
- Git
- Gradle (Optional, as the project includes a Gradle wrapper `./gradlew`)

## Setup and Building

1.  **Clone the repository:**
    ```bash
    git clone <repository_url>
    cd <repository_directory>
    ```
2.  **Build the plugin:**
    Use the Gradle wrapper to build the plugin JAR file.
    ```bash
    ./gradlew build
    ```
    The compiled JAR file will be located in `build/libs/minecraft-server-1.0.jar`.

## Running the Plugin

1.  **Download Paper Server:**
    To run this plugin, you need a Paper server. Download the latest Paper server JAR from [papermc.io](https://papermc.io/downloads/paper).
2.  **Start the Paper Server:**
    Place the downloaded Paper server JAR in a new directory. Navigate to that directory in your terminal and run the server for the first time. This will generate server files, including an `eula.txt`.
    ```bash
    java -jar paper-server.jar nogui
    ```
    (Replace `paper-server.jar` with the actual filename of the Paper server you downloaded).
3.  **Agree to the EULA:**
    Open the generated `eula.txt` file and change `eula=false` to `eula=true`. Save the file.
4.  **Install the Plugin:**
    - Place the compiled plugin JAR (`build/libs/MinecraftServer-1.0.jar`) into the `plugins` folder inside your Paper server directory.
5.  **Run the Server Again:**
    ```bash
    java -jar paper-server.jar nogui
    ```
    You should see messages in the server console indicating that "MinecraftServer plugin has been enabled!".

## Customization

-   Modify `src/main/java/com/example/minecraftserver/Main.java` to add your custom plugin logic.
-   Adjust plugin details (name, version, description) in `src/main/resources/plugin.yml`.
-   Configure server properties by editing `server.properties` in your Paper server directory.

## Deployment (on a Linux VPS)

These steps guide you on deploying your Minecraft plugin to a Linux Virtual Private Server (VPS).

1.  **Choose a VPS Provider:**
    Select a VPS provider (e.g., Scaleway, DigitalOcean, Vultr, Linode, etc.). A server with at least 2GB of RAM is recommended for a small PaperMC server. For example, Scaleway's DEV1-S or PLAY2-PICO, or similar instances from other providers.

2.  **Initial Server Setup:**
    *   Connect to your VPS via SSH.
    *   Update your server's package list:
        ```bash
        sudo apt update && sudo apt upgrade -y
        ```
    *   Install Java (OpenJDK 17, as required by the plugin and PaperMC):
        ```bash
        sudo apt install openjdk-17-jre-headless -y
        ```
    *   Verify Java installation:
        ```bash
        java -version
        ```
    *   Create a directory for your Minecraft server:
        ```bash
        mkdir ~/minecraft_server
        cd ~/minecraft_server
        ```

3.  **Download PaperMC:**
    *   Go to the [PaperMC downloads page](https://papermc.io/downloads/paper) and copy the link for the desired Minecraft version (e.g., 1.21).
    *   Download PaperMC using `wget`:
        ```bash
        wget <link_to_papermc.jar> -O paper.jar
        ```
        (Replace `<link_to_papermc.jar>` with the actual download link).

4.  **First Server Run (EULA Agreement):**
    *   Run the PaperMC server for the first time to generate files:
        ```bash
        java -Xms1G -Xmx1G -jar paper.jar nogui
        ```
        (Adjust `Xms` and `Xmx` memory values as needed, e.g., `-Xms2G -Xmx2G` for 2GB RAM. A minimum of 1GB is shown for the first run, but more is better for actual play).
    *   This will stop, asking you to agree to the EULA. Open `eula.txt`:
        ```bash
        nano eula.txt
        ```
    *   Change `eula=false` to `eula=true`, then save and exit (Ctrl+X, then Y, then Enter in `nano`).

5.  **Upload Your Plugin:**
    *   You'll need to transfer your plugin JAR (`MinecraftServer-1.0.jar`) from your local machine to the VPS. Use `scp` (Secure Copy Protocol) for this. From your *local machine's* terminal:
        ```bash
        scp path/to/your/MinecraftServer-1.0.jar username@your_server_ip:~/minecraft_server/plugins/
        ```
        (Replace `path/to/your/MinecraftServer-1.0.jar`, `username`, and `your_server_ip` accordingly).
    *   On the server, if the `plugins` directory doesn't exist yet (it should after the first Paper run), create it:
        ```bash
        mkdir -p ~/minecraft_server/plugins
        ```
        Then ensure the `scp` command above targets this `plugins` folder.

6.  **Run the Server with the Plugin:**
    *   Start the server again. This time it will load your plugin:
        ```bash
        java -Xms2G -Xmx2G -jar paper.jar nogui
        ```
        (Adjust memory as appropriate for your VPS).
    *   You should see messages in the console indicating your plugin ("MinecraftServer") has been enabled.

7.  **Keeping the Server Running (Using Screen):**
    To keep the server running after you disconnect from SSH, use a terminal multiplexer like `screen`.
    *   Install screen:
        ```bash
        sudo apt install screen -y
        ```
    *   Start a new screen session:
        ```bash
        screen -S minecraft
        ```
    *   Navigate to your server directory (`cd ~/minecraft_server`) and start the server as in the previous step.
    *   Detach from the screen session by pressing `Ctrl+A` then `D`. The server will continue running.
    *   To reattach later: `screen -r minecraft`.

8.  **Basic Security (Firewall):**
    *   Configure a firewall (e.g., `ufw`) to allow SSH and Minecraft traffic.
        ```bash
        sudo ufw allow OpenSSH
        sudo ufw allow 25565/tcp  # Default Minecraft port
        sudo ufw enable
        sudo ufw status
        ```
    *   Ensure your VPS provider's network firewall (if any) also allows traffic on port 25565.

This provides a good starting point for deploying the plugin.
