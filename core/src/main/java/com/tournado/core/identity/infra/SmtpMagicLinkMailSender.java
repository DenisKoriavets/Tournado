package com.tournado.core.identity.infra;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpMagicLinkMailSender implements MagicLinkMailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpMagicLinkMailSender(
            JavaMailSender mailSender,
            @Value("${spring.mail.from:noreply@tournado.kma.ua}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendMagicLink(String recipientEmail, String magicLinkUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject("Tournado — Посилання для входу");
            helper.setText(buildEmailBody(magicLinkUrl), false);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to compose magic link email", ex);
        }
    }

    private String buildEmailBody(String magicLinkUrl) {
        return "Вітаємо у Tournado!\n\n"
                + "Натисніть на одноразове посилання нижче, щоб увійти до свого акаунта:\n"
                + magicLinkUrl + "\n\n"
                + "Посилання дійсне протягом обмеженого часу та може бути використане лише один раз.\n";
    }
}
