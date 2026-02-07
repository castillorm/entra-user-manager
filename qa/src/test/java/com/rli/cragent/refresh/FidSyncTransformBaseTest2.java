package com.rli.cragents.refresh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.rli.connectors.changeevent.ChangeEvent;
import com.rli.connectors.changeevent.EventType;
import com.rli.scripts.fidsync.ou_source_o_examples_sync_ou_destination_o_examples.Transfo_ou_source_o_examples;
import testutil.TransformTestHarness;
import com.rli.cragents.refresh.PipelineIdentifier;
import org.junit.jupiter.api.Tag;
import com.rli.cragents.refresh.j; // pipeline meta type in your decompile

public class FidSyncTransformBaseTest2 {

  @Tag("current")
  @Test
  void baseOnetoManyTest2() throws Exception {
    ChangeEvent in = new ChangeEvent(EventType.INSERT);
    in.addAttribute("cn", "Adem");
    Transfo_ou_source_o_examples t = new Transfo_ou_source_o_examples();

    // Transfo_ou_source_o_examples t =
    // TransformTestHarness.newTestTransform(Transfo_ou_source_o_examples.class);
    List<ChangeEvent> out = t.transformToList(in);
    PipelineIdentifier pid = PipelineIdentifier.a("id");
    assertEquals("Ademtest1", out.get(0).getFirstValueAsString("cn"));
    assertEquals("Ademtest2", out.get(1).getFirstValueAsString("cn"));
    assertEquals("Ademtest3", out.get(2).getFirstValueAsString("cn"));
    assertEquals("Ademtest4", out.get(3).getFirstValueAsString("cn"));

    j meta = new j(
        "ou_source_o_examples",
        "ou_destination_o_examples",
        null,
        PipelineIdentifier.a("ou_source_o_examples_sync_ou_destination_o_examples"),
        false,
        false);
    t.setPipelineMeta(meta);
    assertNotNull(out);

    assertEquals(4, out.size());
  }

}
// j meta = new j(
// "ou_source_o_examples",
// "ou_destination_o_examples",
// null,
// pid,
// false,
// false);
