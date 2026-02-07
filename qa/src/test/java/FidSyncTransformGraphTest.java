import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.rli.connectors.changeevent.ChangeEvent;
import com.rli.connectors.changeevent.EventType;
import com.rli.scripts.fidsync.ou_source_o_examples_sync_o_graphs.Transfo_ou_source_o_examples;
import testutil.TransformTestHarness;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FidSyncTransformGraphTest {
  @Tag("current")
  @Test
  void baseGraphTest() throws Exception {
    ObjectMapper om = new ObjectMapper();
    ChangeEvent in = om.readValue(
        new String(Files.readAllBytes(Paths.get("resources/aduser.json")), java.nio.charset.StandardCharsets.UTF_8),
        ChangeEvent.class);
    Transfo_ou_source_o_examples t = TransformTestHarness.newTestTransform(Transfo_ou_source_o_examples.class);
    List<ChangeEvent> out = t.transformToList(in);
    assertNotNull(out);
    assertEquals(1, out.size());
  }

}
