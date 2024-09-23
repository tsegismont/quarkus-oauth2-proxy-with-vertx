package org.example.proxy;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.providers.GithubAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.proxy.handler.ProxyHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.httpproxy.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RouterConfig {

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "client.id")
    String clientId;
    @ConfigProperty(name = "client.secret")
    String clientSecret;

    void init(@Observes Router router) {
        // We need cookies and sessions
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        OAuth2Auth authProvider = GithubAuth.create(vertx, clientId, clientSecret);
        // we now protect the resource under the path "/protected"
        router.route("/protected").handler(OAuth2AuthHandler.create(vertx, authProvider, "http://localhost:4180/oauth2/callback")
                // we now configure the oauth2 handler, it will setup the callback handler
                // as expected by your oauth2 provider.
                .setupCallback(router.route("/oauth2/callback"))
                // for this resource we require that users have the authority to retrieve the user emails
                .withScope("user:email"));

        // The protected resource
        router.route("/protected").handler(rc -> {
            Context context = vertx.getOrCreateContext();
            User user = rc.user();
            Session session = rc.session();
            JsonObject userInfo = session.get("userInfo");
            if (userInfo == null) {
                authProvider.userInfo(user).onComplete(ar -> {
                    if (ar.succeeded()) {
                        session.put("userInfo", ar.result());
                        context.putLocal("userInfo", ar.result());
                        context.putLocal("accessToken", user.get("access_token"));
                        rc.next();
                    } else {
                        session.destroy();
                        rc.fail(ar.cause());
                    }
                });
            } else {
                context.putLocal("userInfo", userInfo);
                context.putLocal("accessToken", user.get("access_token"));
                rc.next();
            }
        });

        HttpClient proxyClient = vertx.createHttpClient();
        HttpProxy httpProxy = HttpProxy.reverseProxy(proxyClient)
                .addInterceptor(new ProxyInterceptor() {
                    @Override
                    public Future<ProxyResponse> handleProxyRequest(ProxyContext pc) {
                        ProxyRequest proxyRequest = pc.request();
                        MultiMap headers = proxyRequest.headers();
                        headers.remove("Cookie");
                        Context context = vertx.getOrCreateContext();
                        JsonObject userInfo = context.getLocal("userInfo");
                        if (userInfo != null) {
                            headers.set("X-Forwarded-User", userInfo.getString("login"));
                            headers.set("X-Forwarded-Email", userInfo.getString("email"));
                            headers.set("X-Forwarded-Access-Token", context.<String>getLocal("accessToken"));
                        }
                        return pc.sendRequest();
                    }
                });
        router.route().handler(ProxyHandler.create(httpProxy, 8080, "localhost"));
    }
}
