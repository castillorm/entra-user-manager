package testutil;

import com.rli.cragents.refresh.PipelineIdentifier;
import com.rli.cragents.refresh.SyncPipelineTransformation;
import com.rli.cragents.refresh.j;
import com.rli.util.jndi.vdap.compound.CompoundObject;
import java.lang.reflect.Constructor;

public final class TransformTestHarness {
  public static <T extends SyncPipelineTransformation> T newTestTransform(Class<T> transfoClass) {
    try {
      // 1) construct the transform
      Constructor<T> ctor = transfoClass.getDeclaredConstructor();
      ctor.setAccessible(true);
      T t = ctor.newInstance();
      // 2) create minimal pipeline meta (j) via reflection
      j meta = newMinimalPipelineMeta();
      // 3) inject via the public setter
      t.setPipelineMeta(meta);
      return t;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create/inject test transform: " + transfoClass.getName(), e);
    }
  }

  private static j newMinimalPipelineMeta() {
    try {
      Constructor<?> jConstructorFunction = j.class.getDeclaredConstructor(
          String.class,
          String.class,
          CompoundObject.class,
          PipelineIdentifier.class,
          boolean.class,
          boolean.class);
      jConstructorFunction.setAccessible(true);
      return (j) jConstructorFunction.newInstance(
          "",
          "",
          null,
          PipelineIdentifier.a("_pipeline_"),
          false,
          false);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create minimal pipeline meta", e);
    }
  }
}
