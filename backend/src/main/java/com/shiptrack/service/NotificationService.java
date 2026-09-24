package com.shiptrack.service;

import com.shiptrack.dto.CommonDtos;
import com.shiptrack.model.Notification;
import com.shiptrack.model.NotificationType;
import com.shiptrack.model.Shipment;
import com.shiptrack.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean emailEnabled;
    private final boolean smsEnabled;

    public NotificationService(NotificationRepository notificationRepository,
                               SimpMessagingTemplate messagingTemplate,
                               ObjectProvider<JavaMailSender> mailSenderProvider,
                               @Value("${app.notifications.email-enabled:false}") boolean emailEnabled,
                               @Value("${app.notifications.sms-enabled:false}") boolean smsEnabled) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.mailSenderProvider = mailSenderProvider;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
    }

    public Notification send(String userId, NotificationType type, String title, String message, Shipment shipment) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .shipmentId(shipment != null ? shipment.getId() : null)
                .trackingNumber(shipment != null ? shipment.getTrackingNumber() : null)
                .build();

        Notification saved = notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, saved);

        if (shipment != null) {
            deliverEmail(shipment.getOwnerEmail(), title, message);
            if (shipment.getReceiver() != null) {
                deliverSms(shipment.getReceiver().getContactPhone(), title + " - " + message);
            }
        }
        return saved;
    }

    @Async("notificationExecutor")
    public void deliverEmail(String to, String subject, String body) {
        if (!emailEnabled || to == null || to.isBlank()) {
            log.debug("Email skipped (disabled or no address): {}", subject);
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) return;
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(to);
            mail.setSubject("ShipTrack Pro: " + subject);
            mail.setText(body);
            sender.send(mail);
        } catch (Exception ex) {
            log.warn("Email to {} could not be sent: {}", to, ex.getMessage());
        }
    }

    @Async("notificationExecutor")
    public void deliverSms(String phone, String body) {
        if (!smsEnabled || phone == null || phone.isBlank()) {
            log.debug("SMS skipped (disabled or no number): {}", body);
            return;
        }
        // Wire a Twilio client here; the account SID and auth token belong in environment variables.
        log.info("SMS to {}: {}", phone, body);
    }

    public Page<Notification> list(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long unreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public CommonDtos.ApiMessage markRead(String id, String userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> com.shiptrack.exception.ApiException.notFound("Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw com.shiptrack.exception.ApiException.forbidden("That notification belongs to another account");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
        return new CommonDtos.ApiMessage("Notification marked as read");
    }

    public CommonDtos.ApiMessage markAllRead(String userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return new CommonDtos.ApiMessage(unread.size() + " notifications marked as read");
    }

    public void delete(String id, String userId) {
        notificationRepository.findById(id)
                .filter(n -> n.getUserId().equals(userId))
                .ifPresent(notificationRepository::delete);
    }
}
