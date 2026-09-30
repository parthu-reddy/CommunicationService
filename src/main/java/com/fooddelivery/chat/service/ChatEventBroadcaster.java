package com.fooddelivery.chat.service;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Delivers chat events only through user destinations. The canonical roster is resolved at send
 * time, so a user removed from an order can no longer receive subsequent events through an old
 * shared-topic subscription.
 */
@Service
public class ChatEventBroadcaster {

    private static final String CHAT_QUEUE_PREFIX = "/queue/chat/";

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatSessionAccessService accessService;
    private final ChatSupportSubscriptionRegistry supportSubscriptions;

    public ChatEventBroadcaster(SimpMessageSendingOperations messagingTemplate,
                                ChatSessionAccessService accessService,
                                ChatSupportSubscriptionRegistry supportSubscriptions) {
        this.messagingTemplate = messagingTemplate;
        this.accessService = accessService;
        this.supportSubscriptions = supportSubscriptions;
    }

    /** Broadcast a persisted chat message after the surrounding transaction commits. */
    public void broadcastMessage(UUID sessionId, Object payload) {
        dispatchAfterCommit(() -> sendToRecipients(sessionId, CHAT_QUEUE_PREFIX + sessionId, payload));
    }

    /** Typing indicators are intentionally ephemeral and are sent immediately. */
    public void broadcastTyping(UUID sessionId, Object payload) {
        sendToRecipients(sessionId, CHAT_QUEUE_PREFIX + sessionId + "/typing", payload);
    }

    private void dispatchAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private void sendToRecipients(UUID sessionId, String destination, Object payload) {
        Set<String> recipientIds = new LinkedHashSet<>(accessService.canonicalParticipantIds(sessionId));
        recipientIds.addAll(supportSubscriptions.moderatorIds(sessionId));
        recipientIds.forEach(userId -> messagingTemplate.convertAndSendToUser(userId, destination, payload));
    }
}
