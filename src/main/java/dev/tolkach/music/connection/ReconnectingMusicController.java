package dev.tolkach.music.connection;

import dev.tolkach.music.MusicController;
import dev.tolkach.yandex.model.PlaybackState;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Function;

public class ReconnectingMusicController implements MusicController, AutoCloseable {

    private static final Duration HEALTH_CHECK_INTERVAL = Duration.ofSeconds(5);

    private static final long[] RECONNECT_DELAYS_SECONDS = {1, 2, 4, 8, 10};

    private final YandexMusicSessionFactory sessionFactory;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "yandex-music-runtime");

                thread.setDaemon(false);

                return thread;
            }
    );

    private YandexMusicSession session;

    private int reconnectAttempt;

    private boolean reconnectScheduled;

    private volatile boolean connected;

    private volatile boolean closing;

    public ReconnectingMusicController(YandexMusicSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void start() {
        executor.execute(this::connect);

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
        return invoke(controller -> controller.playTrack(title, artist));
    }

    @Override
    public PlaybackState playQuery(String query) {
        return invoke(controller -> controller.playQuery(query));
    }

    @Override
    public PlaybackState playAlbum(String title, String artist) {
        return invoke(controller -> controller.playAlbum(title, artist));
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
        invoke(controller -> {
            controller.resume();
            return null;
        });
    }

    @Override
    public void next() {
        invoke(controller -> {
            controller.next();
            return null;
        });
    }

    @Override
    public void previous() {
        invoke(controller -> {
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

    private <T> T invoke(Function<MusicController, T> operation) {
        if (closing) {
            throw new MusicUnavailableException("Music controller is shutting down");
        }

        CompletableFuture<T> future = new CompletableFuture<>();

        executor.execute(() -> {
            ensureConnection();

            if (session == null) {
                future.completeExceptionally(new MusicUnavailableException("Yandex Music is not available"));

                return;
            }

            try {
                T result = operation.apply(session.controller());

                future.complete(result);
            }
            catch (RuntimeException exception) {
                handleOperationFailure(exception, future);
            }
        });

        return join(future);
    }

    private void ensureConnection() {
        if (session == null) {
            connect();
        }

        if (session == null) {
            return;
        }

        if (session.ping()) {
            return;
        }

        System.out.println("Yandex Music connection is stale.");

        disconnect();

        connect();

        if (session == null) {
            scheduleReconnect();
        }
    }

    private <T> void handleOperationFailure(RuntimeException error, CompletableFuture<T> future) {
        if (session != null && !session.ping()) {
            System.out.println("Yandex Music connection lost during command.");

            disconnect();
            scheduleReconnect();

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

            reconnectAttempt = 0;

            System.out.println("Connected to Yandex Music.");
        }
        catch (RuntimeException exception) {
            connected = false;

            System.out.println("Yandex Music is unavailable: " + exception.getMessage());

            scheduleReconnect();
        }
    }

    private void healthCheck() {
        if (closing) {
            return;
        }

        try {
            if (session == null) {
                scheduleReconnect();
                return;
            }

            if (session.ping()) {
                return;
            }

            System.out.println("Yandex Music connection lost.");

            disconnect();

            scheduleReconnect();
        }
        catch (RuntimeException exception) {
            System.out.println("Yandex Music health check failed: " + exception.getMessage());

            disconnect();

            scheduleReconnect();
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

    private boolean isSessionAlive() {
        if (session == null) {
            return false;
        }

        try {
            return session.isAlive();
        }
        catch (RuntimeException exception) {
            return false;
        }
    }

    private void scheduleReconnect() {
        if (closing || session != null || reconnectScheduled) {
            return;
        }

        long delay = reconnectDelaySeconds();

        reconnectScheduled = true;

        System.out.println("Retrying Yandex Music connection in " + delay + "s...");

        executor.schedule(() -> {
            reconnectScheduled = false;

            connect();
            },
                delay,
                TimeUnit.SECONDS
        );
    }

    private long reconnectDelaySeconds() {
        int index = Math.min(reconnectAttempt, RECONNECT_DELAYS_SECONDS.length - 1);

        reconnectAttempt++;

        return RECONNECT_DELAYS_SECONDS[index];
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
            disconnect();
            future.complete(null);
        });

        future.join();

        executor.shutdownNow();
    }
}
