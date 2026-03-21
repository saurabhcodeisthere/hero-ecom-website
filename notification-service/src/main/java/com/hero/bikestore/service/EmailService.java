package com.hero.bikestore.service;

import com.hero.bikestore.dto.OrderNotificationEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Handles the actual email delivery.
 *
 * Responsibilities:
 *   1. Accept a template name + event data
 *   2. Render the Thymeleaf HTML template with the event as context
 *   3. Build a MIME message (HTML capable)
 *   4. Send via JavaMailSender (SMTP — Mailtrap in dev, real SMTP in prod)
 *
 * WHY separate from handlers?
 * ────────────────────────────
 * SRP: each handler knows WHAT to send (subject, template name).
 * EmailService knows HOW to send (SMTP, MIME, Thymeleaf rendering).
 * These are two different reasons to change — kept in separate classes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    /**
     * Renders a Thymeleaf template and sends it as an HTML email.
     *
     * @param to           recipient email address
     * @param subject      email subject line
     * @param templateName path to Thymeleaf template (e.g. "email/order-placed")
     *                     maps to src/main/resources/templates/email/order-placed.html
     * @param event        the order event — passed as "event" variable inside the template
     */
    public void sendHtmlEmail(String to, String subject, String templateName, OrderNotificationEvent event) {
        try {
            // Step 1: Build Thymeleaf context — "event" becomes available in the HTML template
            Context context = new Context();
            context.setVariable("event", event);

            // Step 2: Render HTML from template
            String htmlContent = templateEngine.process(templateName, context);

            // Step 3: Build MIME message (multipart = true enables HTML + attachments)
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);  // true = send as HTML

            // Step 4: Send
            mailSender.send(message);
            log.info("Email sent successfully to={} subject={}", to, subject);

        } catch (MessagingException e) {
            log.error("Failed to send email to={} subject={} error={}", to, subject, e.getMessage());
            throw new RuntimeException("Email delivery failed for recipient: " + to, e);
        }
    }
}
