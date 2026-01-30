package com.keyesit.graphcli;

import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphGuestInviter {
  private static final Logger log = LoggerFactory.getLogger(GraphGuestInviter.class);
  private final GraphServiceClient graph;

  public GraphGuestInviter(GraphServiceClient graph) {
    this.graph = graph;
  }

  public Invitation invite(String email, String redirectUrl, boolean sendInvitationMessage) {
    log.debug("EXEC_INVITE start email={} redirectUrl={} sendMessage={}", email, redirectUrl, sendInvitationMessage);

    Invitation inv = new Invitation();
    inv.setInvitedUserEmailAddress(email);
    inv.setInviteRedirectUrl(redirectUrl);
    inv.setSendInvitationMessage(sendInvitationMessage);

    Invitation created = graph.invitations().post(inv);

    log.debug("EXEC_INVITE success email={} invitedUserId={} redeemUrl={}",
        safe(created.getInvitedUserEmailAddress()),
        created.getInvitedUser() != null ? safe(created.getInvitedUser().getId()) : "",
        safe(created.getInviteRedeemUrl()));

    return created;
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }
}
