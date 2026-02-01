
package com.keyesit.graphcli;

import com.microsoft.graph.invitations.InvitationsRequestBuilder;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.graph.users.item.UserItemRequestBuilder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AppGraphTest {

  // ---------- INVITE ----------

  @Test
  void invite_postsInvitation() {
    GraphServiceClient graph = mock(GraphServiceClient.class);
    InvitationsRequestBuilder invitations = mock(InvitationsRequestBuilder.class);

    when(graph.invitations()).thenReturn(invitations);

    Invitation created = new Invitation();
    created.setInvitedUserEmailAddress("test@contoso.com");
    when(invitations.post(any())).thenReturn(created);

    GraphGuestInviter inviter = new GraphGuestInviter(graph);
    Invitation result = inviter.invite(
        "test@contoso.com",
        "https://myapps.microsoft.com",
        true);

    verify(invitations, times(1)).post(any());
    assertEquals("test@contoso.com", result.getInvitedUserEmailAddress());
  }

  // ---------- DELETE ----------

  @Test
  void delete_callsGraphDelete() {
    GraphServiceClient graph = mock(GraphServiceClient.class);
    UsersRequestBuilder users = mock(UsersRequestBuilder.class);
    UserItemRequestBuilder user = mock(UserItemRequestBuilder.class);

    when(graph.users()).thenReturn(users);
    when(users.byUserId("user-id")).thenReturn(user);

    GraphUserDeleter deleter = new GraphUserDeleter(graph);
    deleter.deleteById("user-id");

    verify(user, times(1)).delete();
  }

  // ---------- SEARCH ----------

  @Test
  void search_callsUsersGet() throws Exception {
    GraphServiceClient graph = mock(GraphServiceClient.class);
    UsersRequestBuilder users = mock(UsersRequestBuilder.class);

    when(graph.users()).thenReturn(users);

    UserCollectionResponse emptyPage = new UserCollectionResponse();
    emptyPage.setValue(Collections.emptyList());

    when(users.get(any())).thenReturn(emptyPage);

    AppConfig cfg = loadConfig(
        "[operation]\n" +
            "mode=search\n" +
            "\n" +
            "[search]\n" +
            "query=alice\n" +
            "maxResults=5\n" +
            "\n" +
            "[invite]\n" +
            "redirectUrl=\n" +
            "sendInvitationMessage=true\n");

    GraphUserFinder finder = new GraphUserFinder(graph);
    finder.find(cfg);

    verify(users, times(1)).get(any());
  }

  // ---------- helpers ----------

  private static AppConfig loadConfig(String ini) throws Exception {
    Path tmp = Files.createTempFile("config", ".ini");
    Files.write(tmp, ini.getBytes(StandardCharsets.UTF_8));
    IniConfig cfg = IniConfig.load(tmp);
    return AppConfig.fromIni(cfg);
  }
}
