package net.orbis.zakum.core.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Shared async pool for Zakum.
 *
 * Reload safety:
 * - keep a single pool reference
 * - shut it down on plugin disable
 */
public final class Async {

  private Async() {}

  public static ExecutorService newSharedPool(Logger log) {
    int threads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);

    ThreadFactory tf = new ThreadFactory() {
      private final AtomicInteger idx = new AtomicInteger();

      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("zakum-async-" + idx.incrementAndGet());
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((th, ex) ->
          log.warning("Uncaught in " + th.getName() + ": " + ex.getMessage())
        );
        return t;
      }
    };

    return Executors.newFixedThreadPool(threads, tf);
  }
}
