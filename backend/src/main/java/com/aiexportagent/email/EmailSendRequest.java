package com.aiexportagent.email;

public record EmailSendRequest(String toEmail, String subject, String body) {
}
