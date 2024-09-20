package org.example.server;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("protected")
public class ProtectedResource {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance userInfo(UserInfo userInfo);
    }

    private final UserEmailsService userEmailsService;

    public ProtectedResource() {
        userEmailsService = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://api.github.com"))
                .build(UserEmailsService.class);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String get(HttpHeaders headers) {
        MultivaluedMap<String, String> headersMap = headers.getRequestHeaders();

        String login = headersMap.getFirst("X-Forwarded-User");
        String email = headersMap.getFirst("X-Forwarded-Email");
        String accessToken = headersMap.getFirst("X-Forwarded-Access-Token");

        List<String> privateEmails = new ArrayList<>();
        for (UserEmail userEmail : userEmailsService.get(accessToken)) {
            privateEmails.add(userEmail.email());
        }

        UserInfo userInfo = new UserInfo(login, email, privateEmails);

        return Templates.userInfo(userInfo).render();
    }
}