package com.fooddelivery.chat.service;

import com.fooddelivery.chat.entity.CallLog;
import com.fooddelivery.chat.enums.CallStatus;
import com.fooddelivery.chat.repository.CallLogRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class CallLogService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CallLogService.class);
    private final CallLogRepository callLogRepository;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void processOffer(UUID sessionId, String callerId, String calleeId) {
        CallLog callLog = CallLog.builder().sessionId(sessionId).callerId(callerId).calleeId(calleeId).status(CallStatus.RINGING).build();
        callLogRepository.save(callLog);
        log.info("Call initiated: session={}, caller={}", sessionId, callerId);
    }

    @Transactional
    public void processAnswer(UUID sessionId, String responderId) {
        Optional<CallLog> activeCallOpt = callLogRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId);
        if (activeCallOpt.isPresent()) {
            CallLog call = activeCallOpt.get();
            if (call.getStatus() == CallStatus.RINGING) {
                call.setStatus(CallStatus.IN_PROGRESS);
                call.setStartTime(Instant.now());
                callLogRepository.save(call);
                log.info("Call answered: session={}, responder={}", sessionId, responderId);
            }
        }
    }

    @Transactional
    public void processHangup(UUID sessionId, String senderId, String reason) {
        Optional<CallLog> activeCallOpt = callLogRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId);
        if (activeCallOpt.isPresent()) {
            CallLog call = activeCallOpt.get();
            if (call.getStatus() == CallStatus.IN_PROGRESS) {
                call.setEndTime(Instant.now());
                long duration = Duration.between(call.getStartTime(), call.getEndTime()).getSeconds();
                call.setDurationSeconds((int) duration);
                call.setStatus(CallStatus.COMPLETED);
                callLogRepository.save(call);
                log.info("Call completed: session={}, duration={}s", sessionId, duration);
                // Automatically dispatch system message
                var msg = chatMessageService.saveMessage(sessionId, call.getCallerId(), "[SYSTEM_CALL_ENDED duration=" + duration + "]", "TEXT");
                messagingTemplate.convertAndSend("/topic/chat/" + sessionId, msg);
            } else if (call.getStatus() == CallStatus.RINGING) {
                // Determine if missed or declined based on who hung up
                if (senderId.equals(call.getCallerId())) {
                    call.setStatus(CallStatus.MISSED);
                    log.info("Call missed: session={}", sessionId);
                    var msg = chatMessageService.saveMessage(sessionId, call.getCallerId(), "[SYSTEM_MISSED_CALL]", "TEXT");
                    messagingTemplate.convertAndSend("/topic/chat/" + sessionId, msg);
                } else {
                    call.setStatus(CallStatus.DECLINED);
                    log.info("Call declined: session={}", sessionId);
                    var msg = chatMessageService.saveMessage(sessionId, call.getCallerId(), "[SYSTEM_MISSED_CALL]", "TEXT");
                    messagingTemplate.convertAndSend("/topic/chat/" + sessionId, msg);
                }
                call.setEndTime(Instant.now());
                callLogRepository.save(call);
            }
        }
    }

    @java.lang.SuppressWarnings("all")
    public CallLogService(final CallLogRepository callLogRepository, final ChatMessageService chatMessageService, final SimpMessagingTemplate messagingTemplate) {
        this.callLogRepository = callLogRepository;
        this.chatMessageService = chatMessageService;
        this.messagingTemplate = messagingTemplate;
    }
}
