package com.tournado.core.identity.infra;

public interface MagicLinkMailSender {

    void sendMagicLink(String recipientEmail, String magicLinkUrl);
}
