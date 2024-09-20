package org.example.server;

import io.quarkus.rest.client.reactive.NotBody;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;

import java.util.List;

@Path("user/emails")
public interface UserEmailsService {

    @GET
    @ClientHeaderParam(name = "Authorization", value = "Bearer {accessToken}")
    List<UserEmail> get(@NotBody String accessToken);
}