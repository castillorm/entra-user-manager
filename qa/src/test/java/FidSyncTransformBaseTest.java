
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.rli.connectors.changeevent.ChangeEvent;
import com.rli.connectors.changeevent.EventType;
import com.rli.scripts.fidsync.ou_source_o_examples_sync_ou_destination_o_examples.Transfo_ou_source_o_examples;
import testutil.TransformTestHarness;
import org.junit.jupiter.api.Tag;

public class FidSyncTransformBaseTest {

  @Tag("current")
  @Test
  void baseOnetoManyTest() throws Exception {
    ChangeEvent in = new ChangeEvent(EventType.INSERT);
    in.addAttribute("cn", "Adem");
    Transfo_ou_source_o_examples t = TransformTestHarness.newTestTransform(Transfo_ou_source_o_examples.class);
    List<ChangeEvent> out = t.transformToList(in);
    assertEquals("Ademtest1", out.get(0).getFirstValueAsString("cn"));
    assertEquals("Ademtest2", out.get(1).getFirstValueAsString("cn"));
    assertEquals("Ademtest3", out.get(2).getFirstValueAsString("cn"));
    assertEquals("Ademtest4", out.get(3).getFirstValueAsString("cn"));
    assertNotNull(out);
    assertEquals(4, out.size());
  }

}
