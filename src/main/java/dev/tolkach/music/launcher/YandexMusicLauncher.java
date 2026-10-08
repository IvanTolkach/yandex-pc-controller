package dev.tolkach.music.launcher;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class YandexMusicLauncher {

    private static final Duration CDP_GRACE_PERIOD = Duration.ofSeconds(2);

    private static final Duration PROCESS_STOP_TIMEOUT = Duration.ofSeconds(5);

    private final YandexMusicLaunchConfig config;
    private final YandexMusicExecutableLocator executableLocator;
    private final CdpEndpointProbe cdpProbe;

    public YandexMusicLauncher(YandexMusicLaunchConfig config, YandexMusicExecutableLocator executableLocator) {
        this.config = config;
        this.executableLocator = executableLocator;
        this.cdpProbe = new CdpEndpointProbe(config.cdpUrl());
    }

    public void ensureRunning() {
        if (cdpProbe.isAvailable()) {
            System.out.println("Yandex Music CDP is already available.");

            return;
        }

        Path executable = executableLocator.locate();

        List<ProcessHandle> runningProcess = findRunningProcesses(executable);

        if (!runningProcess.isEmpty()) {
            if (waitForCdp(CDP_GRACE_PERIOD)) {
                return;
            }

            System.out.println("Yandex Music is running without CDP. Restarting...");

            restart(executable, runningProcess);

            return;
        }

        startAndWait(executable);
    }

    private void restart(Path executable, List<ProcessHandle> runningProcesses) {
        stopProcess(runningProcesses);

        startAndWait(executable);
    }

    private void startAndWait(Path executable) {
        startProcess(executable);

        if (!waitForCdp(config.startupTimeout())) {
            throw new YandexMusicLaunchException("Yandex Music started, but CDP did not become available within " + config.startupTimeout().toSeconds() + " seconds");
        }

        System.out.println("Yandex Music started with CDP.");
    }

    private void stopProcess(List<ProcessHandle> processes) {
        System.out.println("Stopping existing Yandex Music process...");

        for (ProcessHandle process : processes) {
            if (process.isAlive()) {
                process.destroy();
            }
        }

        if (waitUntilStopped(processes, PROCESS_STOP_TIMEOUT)) {
            System.out.println("Yandex Music stopped.");

            return;
        }

        System.out.println("Yandex Music did not stop in time. Forcing process termination...");

        for (ProcessHandle process : processes) {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }

        if (!waitUntilStopped(processes, PROCESS_STOP_TIMEOUT)) {
            throw new YandexMusicLaunchException("Failed to stop existing Yandex Music process");
        }

        System.out.println("Yandex Music stopped.");
    }

    private boolean waitUntilStopped(List<ProcessHandle> processes, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            boolean anuAlive = processes.stream().anyMatch(ProcessHandle::isAlive);

            if (!anuAlive) {
                return true;
            }

            sleep(Duration.ofMillis(100));
        }

        return processes.stream().noneMatch(ProcessHandle::isAlive);
    }

    private List<ProcessHandle> findRunningProcesses(Path executable) {
        Path expected = executable.toAbsolutePath().normalize();

        return ProcessHandle
                .allProcesses()
                .filter(ProcessHandle::isAlive)
                .filter(process -> process.info()
                        .command()
                        .map(command -> isSameExecutable(expected, command))
                        .orElse(false)
                )
                .toList();
    }

    private void startProcess(Path executable) {
        int port = URI.create(config.cdpUrl()).getPort();

        if (port <= 0) {
            throw new YandexMusicLaunchException("Invalid CDP port: " + config.cdpUrl());
        }

        System.out.println("Starting Yandex Music: " + executable);

        ProcessBuilder processBuilder = new ProcessBuilder(executable.toString(), "--remote-debugging-port=" + port);

        Path directory = executable.getParent();

        if (directory != null) {
            processBuilder.directory(directory.toFile());
        }

        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

        try {
            processBuilder.start();
        }
        catch (IOException exception) {
            throw new YandexMusicLaunchException("Failed to start Yandex Music: " + executable, exception);
        }
    }

    private boolean waitForCdp(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            if (cdpProbe.isAvailable()) {
                System.out.println("Yandex Music CDP is ready.");

                return true;
            }

            sleep(config.probeInterval());
        }

        return cdpProbe.isAvailable();
    }

    private boolean isSameExecutable(Path expected, String command) {
        try {
            Path actual = Path.of(command).toAbsolutePath().normalize();

            return Files.isSameFile(expected, actual);
        }
        catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new YandexMusicLaunchException("Interrupted while waiting for Yandex Music", exception);
        }
    }
}
