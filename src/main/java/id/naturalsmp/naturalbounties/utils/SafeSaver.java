package me.naturalsmp.NaturalBounties.utils;

import java.util.concurrent.*;

public final class SafeSaver {
    private static final ExecutorService SAVE_EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NaturalBounties-Save");
        t.setDaemon(true);
        return t;
    });

    public static void saveWithTimeout(Runnable saveTask, long timeoutMillis) throws TimeoutException, ExecutionException, InterruptedException {
        Future<?> f = SAVE_EXEC.submit(saveTask);
        try {
            f.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            f.cancel(true); // request interrupt
            throw e;
        }
    }

    public static void shutdown() {
        SAVE_EXEC.shutdownNow();
    }
}
