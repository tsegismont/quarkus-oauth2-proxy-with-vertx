package org.example.server;

import java.util.List;

public record UserInfo(String login, String email, List<String> privateEmails) {
}
