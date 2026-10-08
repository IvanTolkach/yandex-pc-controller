package dev.tolkach;

import dev.tolkach.gateway.client.GatewayClient;
import dev.tolkach.music.commands.*;
import dev.tolkach.music.connection.ReconnectingMusicController;
import dev.tolkach.music.connection.YandexMusicSessionFactory;
import dev.tolkach.music.launcher.YandexMusicExecutableLocator;
import dev.tolkach.music.launcher.YandexMusicLaunchConfig;
import dev.tolkach.music.launcher.YandexMusicLauncher;
import dev.tolkach.music.protocol.*;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class Main {

    private static final String CDP_URL = "http://127.0.0.1:9222";

    private static final String GATEWAY_URL = "ws://127.0.0.1:8080/ws/agent";

    private static final String DEVICE_ID = "my-pc";

    static void main() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        YandexMusicLaunchConfig launchConfig = new YandexMusicLaunchConfig(CDP_URL, Duration.ofSeconds(20), Duration.ofMillis(500));

        YandexMusicLauncher launcher = new YandexMusicLauncher(launchConfig, new YandexMusicExecutableLocator());

        YandexMusicSessionFactory sessionFactory = new YandexMusicSessionFactory(CDP_URL);

        try (ReconnectingMusicController musicController = new ReconnectingMusicController(sessionFactory, launcher);
             GatewayClient gatewayClient = createGatewayClient(musicController)) {

            musicController.start();

            gatewayClient.connect();

            System.out.println("Desktop agent is running.");
            System.out.println("Gateway: " + GATEWAY_URL);
            System.out.println("Device id: " + DEVICE_ID);
            System.out.println("Press ENTER to shutdown.");
            System.in.read();
        }
        catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private static GatewayClient createGatewayClient(ReconnectingMusicController controller) {
        MusicCommandHandler commandHandler = new MusicCommandHandler(controller);

        MusicCommandDispatcher dispatcher = new MusicCommandDispatcher(new MusicCommandMapper(), commandHandler);

        return new GatewayClient(GATEWAY_URL, DEVICE_ID, dispatcher);
    }
}
