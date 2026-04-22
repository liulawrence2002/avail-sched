package com.goblinscheduler.service;

import com.goblinscheduler.model.Event;
import com.goblinscheduler.model.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FRIENDLY_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d 'at' h:mm a");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromAddress;
    private final String appUrl;
    private final boolean enabled;

    public EmailService(JavaMailSender mailSender,
                        TemplateEngine templateEngine,
                        @Value("${goblin.mail.from}") String fromAddress,
                        @Value("${goblin.app-url}") String appUrl,
                        @Value("${goblin.mail.enabled}") boolean enabled) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = fromAddress;
        this.appUrl = appUrl;
        this.enabled = enabled;
    }

    public void sendParticipantWelcome(Event event, Participant participant) {
        if (!enabled || participant.getEmail() == null || participant.getEmail().isBlank()) {
            return;
        }

        Context ctx = new Context();
        ctx.setVariable("eventTitle", event.getTitle());
        ctx.setVariable("eventDescription", event.getDescription());
        ctx.setVariable("participantName", participant.getDisplayName());
        ctx.setVariable("eventLink", appUrl + "/e/" + event.getPublicId());
        ctx.setVariable("availabilityLink", appUrl + "/e/" + event.getPublicId());
        ctx.setVariable("appUrl", appUrl);

        sendEmail(participant.getEmail(), "You're invited: " + event.getTitle(), "welcome", ctx);
    }

    public void sendEventFinalized(Event event, Participant participant) {
        if (!enabled || participant.getEmail() == null || participant.getEmail().isBlank()) {
            return;
        }

        var slotLocal = event.getFinalSlotStart().atZone(ZoneId.of(event.getTimezone()));
        String formattedTime = slotLocal.format(FRIENDLY_FORMATTER);

        Context ctx = new Context();
        ctx.setVariable("eventTitle", event.getTitle());
        ctx.setVariable("participantName", participant.getDisplayName());
        ctx.setVariable("finalTime", formattedTime);
        ctx.setVariable("timezone", event.getTimezone());
        ctx.setVariable("location", event.getLocation());
        ctx.setVariable("meetingUrl", event.getMeetingUrl());
        ctx.setVariable("eventLink", appUrl + "/e/" + event.getPublicId());
        ctx.setVariable("icsLink", appUrl + "/api/events/" + event.getPublicId() + "/final.ics");
        ctx.setVariable("appUrl", appUrl);

        sendEmail(participant.getEmail(), "Finalized: " + event.getTitle(), "finalized", ctx);
    }

    public void sendReminder(Event event, Participant participant) {
        if (!enabled || participant.getEmail() == null || participant.getEmail().isBlank()) {
            return;
        }

        Context ctx = new Context();
        ctx.setVariable("eventTitle", event.getTitle());
        ctx.setVariable("participantName", participant.getDisplayName());
        ctx.setVariable("eventLink", appUrl + "/e/" + event.getPublicId());
        ctx.setVariable("appUrl", appUrl);

        String deadlineText = "";
        if (event.getDeadline() != null) {
            var dl = event.getDeadline().atZone(ZoneId.of(event.getTimezone()));
            deadlineText = dl.format(FRIENDLY_FORMATTER);
        }
        ctx.setVariable("deadline", deadlineText);

        sendEmail(participant.getEmail(), "Reminder: " + event.getTitle() + " needs your availability", "reminder", ctx);
    }

    private void sendEmail(String to, String subject, String templateName, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);

            String htmlContent = templateEngine.process("email/" + templateName, context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
