package com.keyesit.graphcli;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphUserDeleter {
  private static final Logger log = LoggerFactory.getLogger(GraphUserDeleter.class);
  private final GraphServiceClient graph;

  public GraphUserDeleter(GraphServiceClient graph) {
    this.graph = graph;
  }

  public void deleteById(String userId) {
    log.info("EXEC_DELETE start id={}", userId);
    graph.users().byUserId(userId).delete();
    log.info("EXEC_DELETE success id={}", userId);
  }
}
