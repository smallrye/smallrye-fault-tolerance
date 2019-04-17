package io.smallrye.faulttolerance.tracing;

import org.jboss.logging.Logger;

/**
 * @author Pavol Loffay
 */
public class TracingInstaller {

  private static final Logger LOGGER = Logger.getLogger(TracingInstaller.class);

  private TracingInstaller() {}

  /**
   * Install installs tracing if OpenTracing libraries are on classpath.
   */
  public static void install() {
    try {
      Class.forName("io.opentracing.Tracer");
      TracingConcurrencyStrategy.register(io.opentracing.util.GlobalTracer.get());
    } catch (ClassNotFoundException | LinkageError e) {
      LOGGER.debug("OpenTracing is not on classpath, skipping context propagation instrumentation");
    }
  }
}
