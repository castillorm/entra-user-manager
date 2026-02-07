package testutil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

// Radiant doesn't expose public API for the entire setup process of a Transfrom
// This file is the minima scaffolding required to setup a transfrom.
public final class TransformTestHarness {
  private static final String DECLARING_CLASS = "com.rli.cragents.refresh.SyncPipelineTransformation";
  // The most notable private data that is required to setup is: 'pipelineMeta'
  // by default this value is null, and there is no public API to set it.
  private static final String FIELD_NAME = "pipelineMeta";
  private static final Field PIPELINE_META_FIELD = resolvePipelineMetaField();

  private TransformTestHarness() {
  }

  private static Field resolvePipelineMetaField() {
    try {
      Class<?> declaring = Class.forName(DECLARING_CLASS);
      Field f = declaring.getDeclaredField(FIELD_NAME);
      f.setAccessible(true);
      return f;
    } catch (Exception e) {
      throw new ExceptionInInitializerError("Failed to resolve " + DECLARING_CLASS + "#" + FIELD_NAME + ": " + e);
    }
  }

  public static <T> T newTestTransform(Class<T> transfoClass) {
    try {
      Constructor<T> ctor = transfoClass.getDeclaredConstructor();
      ctor.setAccessible(true);
      T t = ctor.newInstance();
      Object meta = PipelineMetaFactory.newTestPipelineMeta();
      PIPELINE_META_FIELD.set(t, meta);
      return t;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create/inject test transform: " + transfoClass.getName(), e);
    }
  }
}
