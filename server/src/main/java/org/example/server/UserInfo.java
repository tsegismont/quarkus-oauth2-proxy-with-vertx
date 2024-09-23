package org.example.server;

import io.quarkus.qute.TemplateData;

import java.util.List;

@TemplateData
public record UserInfo(String login, String email, List<String> privateEmails) {

    public String privateEmailsAsString() {
        return String.join(",", privateEmails);
    }
}
