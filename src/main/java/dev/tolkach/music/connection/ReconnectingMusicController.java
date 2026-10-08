package dev.tolkach.music.connection;

import dev.tolkach.music.MusicController;
import dev.tolkach.music.launcher.YandexMusicLauncher;
import dev.tolkach.yandex.model.PlaybackState;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Function;

public class ReconnectingMusicController implements MusicController, AutoCloseable {

    private static final Duration HEALTH_CHECK_INTERVAL = Duration.ofSeconds(5);

    private final YandexMusicSessionFactory sessionFactory;
    private final YandexMusicLauncher launcher;

    private boolean initialLaunchAttempt;
    private boolean restartAttemptedForCurrentAppRun;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "yandex-music-runtime");

                thread.setDaemon(false);

                return thread;
            }
    );

    private YandexMusicSession session;

    private volatile boolean connected;

    private volatile boolean closing;

    public ReconnectingMusicController(YandexMusicSessionFactory sessionFactory, YandexMusicLauncher launcher) {
        this.sessionFactory = sessionFactory;
        this.launcher = launcher;
    }

    public void start() {
        executor.execute(() -> {
            launchOnStartup();
            connect();
        });

        executor.scheduleWithFixedDelay(
                this::safeHealthCheck,
                HEALTH_CHECK_INTERVAL.toSeconds(),
                HEALTH_CHECK_INTERVAL.toSeconds(),
                TimeUnit.SECONDS
        );
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public PlaybackState playTrack(String title, String artist) {
        return invoke(true, controller -> controller.playTrack(title, artist));
    }

    @Override
    public PlaybackState playQuery(String query) {
        return invoke(true, controller -> controller.playQuery(query));
    }

    @Override
    public PlaybackState playAlbum(String title, String artist) {
        return invoke(true, controller -> controller.playAlbum(title, artist));
    }

    @Override
    public PlaybackState getPlaybackState() {
        return invoke(MusicController::getPlaybackState);
    }

    @Override
    public void pause() {
        invoke(controller -> {
            controller.pause();
            return null;
        });
    }

    @Override
    public void resume() {
        invoke(true, controller -> {
            controller.resume();
            return null;
        });
    }

    @Override
    public void next() {
        invoke(true, controller -> {
            controller.next();
            return null;
        });
    }

    @Override
    public void previous() {
        invoke(true, controller -> {
            controller.previous();
            return null;
        });
    }

    @Override
    public void volumeUp() {
        invoke(controller -> {
            controller.volumeUp();
            return null;
        });
    }

    @Override
    public void volumeDown() {
        invoke(controller -> {
            controller.volumeDown();
            return null;
        });
    }

    private void checkForManuallyStartedYandexMusic() {
        if (closing || session != null) {
            return;
        }

        boolean running;

        try {
            running = launcher.isRunning();
        }
        catch (RuntimeException exception) {
            System.err.println( "Failed to check Yandex Music process: " + exception.getMessage());

            return;
        }

        if (!running) {
            restartAttemptedForCurrentAppRun = false;
            launcher.forgetManagedInstance();
            return;
        }

        if (launcher.isCdpAvailable()) {
            return;
        }

        if (restartAttemptedForCurrentAppRun) {
            return;
        }

        restartAttemptedForCurrentAppRun = true;

        executor.schedule(this::restartIfStillRunningWithoutCdp, 2, TimeUnit.SECONDS);
    }

    private void restartIfStillRunningWithoutCdp() {
        if (closing || session != null) {
            return;
        }

        try {
            if (!launcher.isRunning()) {
                restartAttemptedForCurrentAppRun = false;
                return;
            }

            if (launcher.isCdpAvailable()) {
                connect();
                return;
            }

            launcher.restartRunningWithCdp();

            connect();
        }
        catch (RuntimeException exception) {
            System.err.println("Failed to restart Yandex Music with CDP: " + exception.getMessage());
        }
    }

    private void launchOnStartup() {
        if (initialLaunchAttempt) {
            return;
        }

        initialLaunchAttempt = true;

        try {
            launcher.ensureRunning();

            System.out.println("Initial Yandex Music startup completed.");
        }
        catch (RuntimeException exception) {
            System.err.println("Failed to prepare Yandex Music: " + exception.getMessage());
        }
    }

    private <T> T invoke(Function<MusicController, T> operation) {
        return invoke(false, operation);
    }

    private <T> T invoke(boolean launchIfNeeded, Function<MusicController, T> operation) {
        if (closing) {
            throw new MusicUnavailableException("Music controller is shutting down");
        }

        CompletableFuture<T> future = new CompletableFuture<>();

        executor.execute(() -> {
            try {
                ensureConnection(launchIfNeeded);

                if (session == null) {
                    throw new MusicUnavailableException("Yandex Music is not available");
                }

                T result = operation.apply(session.controller());

                future.complete(result);
            }
            catch (RuntimeException exception) {
                handleOperationFailure(exception, future);
            }
        });

        return join(future);
    }

    private void ensureConnection(boolean launchIfNeeded) {
        if (session != null) {
            if (session.ping()) {
                return;
            }

            System.out.println("Yandex Music connection is stale.");

            disconnect();
        }

        if (launcher.isCdpAvailable()) {
            connect();

            if (session == null) {
                throw new MusicUnavailableException("Failed to connect to Yandex Music");
            }

            return;
        }

        if (!launchIfNeeded) {
            checkForManuallyStartedYandexMusic();
            return;
        }

        System.out.println("Yandex Music is required by command. Preparing application...");

        try {
            launcher.ensureRunning();
        }
        catch (RuntimeException exception) {
            throw new MusicUnavailableException("Failed to start Yandex Music", exception);
        }

        if (!launcher.isCdpAvailable()) {
            throw new MusicUnavailableException("Yandex Music CDP is unavailable after startup");
        }

        connect();

        if (session == null) {
            throw new MusicUnavailableException("Failed to connect to Yandex Music after startup");
        }
    }

    private <T> void handleOperationFailure(RuntimeException error, CompletableFuture<T> future) {
        if (session != null && !session.ping()) {
            System.out.println("Yandex Music connection lost during command.");

            disconnect();

            future.completeExceptionally(new MusicUnavailableException("Yandex Music connection was lost", error));

            return;
        }

        future.completeExceptionally(error);
    }

    private <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        }
        catch (CompletionException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw exception;
        }
    }

    private void connect() {
        if (closing || session != null) {
            return;
        }

        try {
            System.out.println("Connecting to Yandex Music...");

            session = sessionFactory.connect();

            connected = true;

            System.out.println("Connected to Yandex Music.");
        }
        catch (RuntimeException exception) {
            connected = false;
            session = null;

            System.out.println("Yandex Music is unavailable: " + exception.getMessage());
        }
    }

    private void healthCheck() {
        if (closing) {
            return;
        }

        if (session != null) {
            if (session.ping()) {
                return;
            }

            System.out.println("Yandex Music connection lost.");

            disconnect();
        }

        checkForManuallyStartedYandexMusic();

        if (session == null && launcher.isCdpAvailable()) {
            connect();
        }
    }

    private void safeHealthCheck() {
        try {
            healthCheck();
        }
        catch (Throwable exception) {
            System.out.println("Unexpected Yandex Music health check error:");
            exception.printStackTrace();
        }
    }

    private void disconnect() {
        connected = false;

        if (session == null) {
            return;
        }

        try {
            session.close();
        }
        catch (RuntimeException ignored) {
        }
        finally {
            session = null;
        }
    }

    @Override
    public void close() {
        closing = true;

        CompletableFuture<Void> future = new CompletableFuture<>();

        executor.execute(() -> {
            try {
                disconnect();

                launcher.closeManagedInstance();
            }
            catch (RuntimeException exception) {
                System.err.println("Failed to shut down Yandex Music cleanly: " + exception.getMessage());
            }
            finally {
                future.complete(null);
            }
        });

        future.join();

        executor.shutdownNow();
    }
}
