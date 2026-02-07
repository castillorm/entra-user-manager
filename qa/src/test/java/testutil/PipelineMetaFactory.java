package testutil;

import java.lang.reflect.Constructor;

public final class PipelineMetaFactory {
  private static final String META_CLASS = "com.rli.cragents.refresh.j";
  private static final String COMPOUND_CLASS = "com.rli.util.jndi.vdap.compound.CompoundObject";
  private static final String PIPELINE_ID_CLASS = "com.rli.cragents.refresh.PipelineIdentifier";

  private static final Constructor<?> CTOR = resolveCtor();

  private PipelineMetaFactory() {
  }

  private static Constructor<?> resolveCtor() {
    try {
      Class<?> meta = Class.forName(META_CLASS);
      Class<?> compound = Class.forName(COMPOUND_CLASS);
      Class<?> pipelineId = Class.forName(PIPELINE_ID_CLASS);

      Constructor<?> c = meta.getDeclaredConstructor(
          String.class,
          String.class,
          compound,
          pipelineId,
          boolean.class,
          boolean.class);

      c.setAccessible(true);
      return c;
    } catch (Exception e) {
      throw new ExceptionInInitializerError("Failed resolving " + META_CLASS + " ctor: " + e);
    }
  }

  /**
   * Creates com.rli.cragents.refresh.j(String, String, CompoundObject,
   * PipelineIdentifier, boolean, boolean)
   *
   * @param compoundObjectOrNull may be null
   */
  public static Object newPipelineMeta(
      String sourceNaming,
      String destNaming,
      Object compoundObjectOrNull,
      Object pipelineIdentifier,
      boolean triggerBased,
      boolean registerGlobally) {

    try {
      // Optional runtime type checks with helpful errors
      Class<?>[] p = CTOR.getParameterTypes();
      if (compoundObjectOrNull != null && !p[2].isInstance(compoundObjectOrNull)) {
        throw new IllegalArgumentException("compoundObjectOrNull must be " + p[2].getName()
            + " but was " + compoundObjectOrNull.getClass().getName());
      }
      if (pipelineIdentifier == null || !p[3].isInstance(pipelineIdentifier)) {
        throw new IllegalArgumentException("pipelineIdentifier must be " + p[3].getName()
            + " but was " + (pipelineIdentifier == null ? "null" : pipelineIdentifier.getClass().getName()));
      }

      return CTOR.newInstance(
          sourceNaming,
          destNaming,
          compoundObjectOrNull,
          pipelineIdentifier,
          triggerBased,
          registerGlobally);

    } catch (Exception e) {
      throw new RuntimeException("Failed to create pipeline meta (" + META_CLASS + "): " + e, e);
    }
  }

  /**
   * Returns a minimal, known-good pipeline meta suitable for unit tests.
   * No naming contexts, no compound object, not trigger-based, not globally
   * registered.
   */
  public static Object newTestPipelineMeta() {
    try {
      // the only REQUIRED aspect on the api for this is that the pipeid
      // contains the string '_pipeline_' hence this minimal testing harness.
      String pipeId = "_pipeline_";

      Class<?> pidClass = Class.forName("com.rli.cragents.refresh.PipelineIdentifier");
      Object pid = pidClass
          .getMethod("a", String.class)
          .invoke(null, pipeId);

      return newPipelineMeta(
          "", // sourceNaming
          "", // destNaming
          null, // CompoundObject
          pid,
          false,
          false);

    } catch (Exception e) {
      throw new RuntimeException("Failed to create test PipelineMeta", e);
    }
  }

}
