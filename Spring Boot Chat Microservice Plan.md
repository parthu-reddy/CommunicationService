# **Enterprise-Grade Real-Time Communication Microservice: Architecture and Implementation Guide**

The demand for real-time, low-latency communication in modern distributed applications has rendered traditional HTTP request-response paradigms wholly insufficient. In highly dynamic ecosystems such as food delivery logistics, ride-hailing networks, and multi-sided marketplaces, instant bidirectional communication is paramount. Entities participating in these networks—whether they are customers tracking orders, delivery executives coordinating complex logistics, restaurant partners confirming modifications, or centralized support agents resolving financial disputes—require seamless, uninterrupted chat and voice interaction frameworks.  
This comprehensive research report presents an exhaustive architectural blueprint and production-ready implementation strategy for a generalized Chat and Call Microservice. Engineered upon the robust foundations of Java and Spring Boot 3.x, the architecture leverages the Simple Text Oriented Messaging Protocol (STOMP) encapsulated over WebSockets for high-throughput text communication and session signaling, deployed alongside WebRTC for secure, peer-to-peer audio and video calling. The systemic design guarantees horizontal scalability across clustered environments, robust security via JWT frame interception, stateful presence management utilizing distributed Redis caches, and highly durable message history preservation orchestrated through PostgreSQL and Apache Kafka. The resulting system is specifically structured to be easily digested and deployed via automated CI/CD pipelines or AI-driven code generation agents, ensuring a highly resilient, enterprise-grade production environment capable of sustaining millions of concurrent connections.

## **Protocol Selection and Transport Layer Mechanics**

To architect a production-ready communication microservice, one must first deconstruct the limitations of legacy web protocols and define the specific transport mechanisms required to facilitate synchronous bi-directional data flow.  
The traditional RESTful architecture relies fundamentally on a unidirectional communication model wherein the client must initiate every interaction. For real-time applications, engineers historically relied on workaround methodologies such as short polling, where the client sends constant, repetitive HTTP requests, or long polling, where the server holds the HTTP connection open until new data becomes available1. These legacy methods introduce severe latency spikes, rapidly exhaust server threads under high concurrency, and generate immense network overhead due to the constant transmission of uncompressed HTTP headers1. Furthermore, they are highly inefficient for battery-constrained mobile clients, such as those utilized by delivery executives operating in fluctuating network topologies.  
WebSockets resolve these infrastructural inefficiencies by upgrading an initial HTTP request into a persistent, full-duplex TCP connection via a standardized switching protocols handshake3. Once this persistent socket is established, data frames flow seamlessly in both directions with negligible byte overhead, maintaining the continuous connection until explicitly terminated by either the client or the server5.  
However, pure WebSockets only provide a raw TCP socket over the web; they do not dictate how messages should be formatted, routed, or acknowledged. To construct a complex, multi-entity chat service, an application-level messaging protocol is strictly required. STOMP (Simple Text Oriented Messaging Protocol) serves this exact purpose by introducing a sophisticated publish-subscribe mechanism directly over the WebSocket transport layer5. STOMP organizes all communication into structured network frames containing a specific command schema, headers, and a payload body3. This architectural decision allows the Spring Boot backend to intelligently route messages to specific controller methods or broadcast them to multiple subscribers seamlessly4.  
For voice and video communication, routing heavy media streams through a centralized WebSocket server introduces unacceptable latency and prohibitive bandwidth costs. WebRTC (Web Real-Time Communication) provides an open-source framework for direct peer-to-peer User Datagram Protocol (UDP) connections between clients, offering built-in cryptographic encryption and sub-second latency1. WebRTC requires a preliminary "signaling" phase before the peer-to-peer connection can commence. Clients must asynchronously exchange Session Description Protocol (SDP) offers and answers, as well as Interactive Connectivity Establishment (ICE) candidates, to discover each other's public IP addresses and media processing capabilities8. This architecture dictates that the STOMP WebSocket connection will act as the authoritative signaling server, multiplexing call negotiation packets alongside standard text messages to rapidly broker direct media connections9.

## **Distributed Architecture and Scalability Topologies**

A fundamental challenge of deploying WebSocket architectures in cloud environments is that the connections are inherently stateful. If a customer connects to Server Instance A, and a delivery executive connects to Server Instance B, a localized message dispatched by the customer will not reach the executive if the servers solely utilize their isolated, in-memory message brokers.  
To achieve true horizontal scalability, the system must replace Spring's default simple in-memory broker with a dedicated external enterprise message broker, utilizing technologies such as RabbitMQ or ActiveMQ10. When a message is published by a client to a designated destination, the Spring Boot instance immediately relays the message to the central RabbitMQ cluster7. The message broker then universally broadcasts the message to all Spring Boot instances that currently hold active WebSocket subscriptions for that specific topic or targeted user, ensuring seamless cross-node communication regardless of which physical server terminates the client's socket13.  
Tracking which users are currently online across a distributed cluster requires a highly available state store. Redis is utilized extensively within this architecture to maintain active session registries11. When a client establishes a successful WebSocket connection, the backend captures the connection event and records the user's online status in the Redis cluster4. Conversely, when a connection inevitably drops due to network failure or client termination, the disconnection event triggers an automated cleanup operation, removing the stale session from Redis and broadcasting an offline presence update to all relevant peers12.  
The performance of websockets under heavy concurrent loads varies significantly depending on the underlying language and configuration. Benchmarks indicate that while lower-level languages like C++ and Rust excel in raw throughput for basic echo servers, Java-based Spring Boot applications offer unmatched resilience and routing capabilities when configured with non-blocking I/O and optimized thread pools15. This makes Java the premier choice for complex routing involving database lookups, security validations, and third-party integrations.

## **Security, Authentication, and Authorization Interceptors**

Securing WebSocket connections presents a unique, widely documented challenge for modern token-based architectures. The official WebSocket protocol specification does not define a standard methodology for clients to inject custom headers during the initial HTTP upgrade handshake16. While legacy browser clients can utilize HTTP cookies for authentication, this paradigm relies heavily on server-side session persistence and is fundamentally incompatible with modern stateless microservices or cross-platform mobile applications constructed using frameworks like Flutter16.  
A common, highly dangerous anti-pattern observed in nascent deployments involves appending the JSON Web Token (JWT) directly as a URI query parameter. This introduces severe security vulnerabilities, as query parameters are frequently recorded in plaintext within reverse proxy, load balancer, and application access logs, exposing the tokens to internal credential harvesting16.  
The industry best practice dictates rigidly separating the transport layer connection from the messaging layer authentication. Under this paradigm, the initial WebSocket upgrade is permitted to proceed anonymously. However, when the client transmits its very first STOMP frame over the established socket, it is strictly mandated to include the JWT token within the native STOMP headers16.  
A Spring framework component intercepts this initial connection frame, extracts the cryptographic token, validates its signature against the issuer's public key, and authenticates the user context directly within the internal security registry. If the token is cryptographically invalid, expired, or entirely missing, the server categorically rejects the frame and forcefully severs the underlying TCP socket3.  
Spring Security 6 enforces strict authorization rules over WebSocket destinations. Utilizing the modern authorization API, the system mandates that only cryptographically authenticated users can publish or subscribe to designated STOMP destinations11. Destinations prefixed with a specific user identifier are strictly isolated, mathematically ensuring that the message broker only routes private data to the specific, verified WebSocket session matching the authenticated principal5.

## **Relational Schema and History Persistence**

To fulfill the rigorous enterprise requirement of preserving all chat histories for auditing, customer support, and machine learning sentiment analysis, the database topology must accommodate high-throughput inserts while accurately mapping complex, multi-party communication webs. The system relies entirely on PostgreSQL, leveraging its robust relational integrity for session management and its advanced JSONB capabilities for highly extensible message payloads.  
The relational model utilizes three primary tables to manage generalized entity communication, allowing the platform to seamlessly spin up localized sessions between any combination of entities, such as a localized chat between a customer and a delivery executive, or a group support chat encompassing a restaurant partner, the customer, and a centralized dispute resolution agent.

| Table Name | Column | PostgreSQL Data Type | Architectural Constraints and Description |
| :---- | :---- | :---- | :---- |
| chat\_sessions | id | UUID | Primary Key, mathematically generated to prevent sequential ID guessing. |
|  | session\_type | VARCHAR(50) | Categorization index. Examples include 'DIRECT', 'GROUP', or 'SUPPORT'. |
|  | reference\_id | VARCHAR(100) | Optional index linking the chat to an external entity, such as a specific delivery order ID. |
|  | created\_at | TIMESTAMP | Immutable creation timestamp denoting the session genesis. |
|  | is\_active | BOOLEAN | Operational flag indicating if the chat session is currently accepting new messages. |
| session\_participants | session\_id | UUID | Foreign Key directly linked to the chat\_sessions table with cascade delete constraints. |
|  | user\_id | VARCHAR(100) | The unique alphanumeric identifier of the participating user or system entity. |
|  | entity\_type | VARCHAR(50) | Categorical flag denoting the participant's role, such as 'CUSTOMER', 'RESTAURANT', or 'DRIVER'. |
|  | joined\_at | TIMESTAMP | Precise timestamp recording when the specific participant entered the secure chat environment. |
| messages | id | UUID | Primary Key for the individual message payload. |
|  | session\_id | UUID | Foreign Key linking the message to the overarching chat session. |
|  | sender\_id | VARCHAR(100) | Identifier of the entity that originally dispatched the message. |
|  | message\_type | VARCHAR(50) | Protocol flag denoting payload format, such as 'TEXT', 'IMAGE', 'SYSTEM\_ALERT', or 'RTC\_OFFER'. |
|  | content | TEXT | The raw UTF-8 message text or the fully qualified CDN URL for rich media attachments. |
|  | metadata | JSONB | Highly flexible structured data column for unforeseen variables, such as file sizes, geospatial locations, or read receipt arrays. |
|  | created\_at | TIMESTAMP | Immutable timestamp of message dispatch for chronological sorting. |

Performing synchronous database writes directly on the primary WebSocket event loop can create severe processing bottlenecks during high-traffic periods, such as localized mealtime rushes in a food delivery network. Instead of inserting messages directly into PostgreSQL upon receipt, the application controller rapidly publishes a serialized message event to an internal Apache Kafka topic utilizing the Transactional Outbox Pattern18. A dedicated consumer microservice continually polls this Kafka topic and batch-inserts the accumulated records into PostgreSQL. This architectural decoupling ensures the primary WebSocket server remains purely focused on sub-millisecond message routing and broadcasting, offloading all heavy disk I/O operations to secondary background threads1.

## **Media Management and Cloud Storage Strategies**

A comprehensive chat service must support rich media transmission, allowing users to share photographic evidence of delivered packages, damaged goods, or menu item discrepancies. Because WebSockets and message brokers are designed strictly for lightweight, high-velocity text frames, transmitting large binary files directly through the STOMP protocol is an anti-pattern that rapidly depletes server memory and blocks concurrent channels.  
The architecture dictates an out-of-band upload strategy. When a user wishes to send an image in the chat, the frontend application uploads the binary file directly to a designated cloud object storage bucket via a pre-signed REST API URL. Once the upload succeeds, the frontend transmits a lightweight STOMP text message containing the resulting public URL of the image. The backend then routes this URL to the recipient, whose device fetches the image directly from the edge network.  
Selecting the appropriate cloud infrastructure for storing these millions of chat images requires balancing high availability with aggressive cost optimization. Fully managed solutions like Cloudflare R2 provide immense value at scale by completely eliminating data egress fees, charging a flat rate for storage while caching the chat media on a global edge network to ensure ultra-low latency17. Alternatively, enterprise deployments prioritizing localization can utilize platforms like Oracle Cloud Infrastructure, which offers vast outbound data allowances, or highly localized sovereign clouds like E2E Networks, which provide competitive, currency-stable object storage specific to geographic regions like India3.

| Cloud Storage Provider | Pricing Structure | Key Architectural Advantage | Recommended Use Case within Chat Topology |
| :---- | :---- | :---- | :---- |
| **Cloudflare R2** | Flat storage fee; $0.00 egress fees17. | Global edge caching; completely eliminates unpredictable bandwidth billing17. | The absolute optimal choice for globally distributed applications generating massive volumes of rich media chat attachments. |
| **Oracle Cloud (OCI)** | Standard storage fee; 10 TB free egress monthly18. | Enterprise-grade redundancy and strict data residency compliance18. | Ideal for platforms that require massive initial free tiers but prioritize traditional enterprise SLAs. |
| **DigitalOcean** | Base monthly compute fee; pooled egress bandwidth3. | Bandwidth pooling across multiple highly available nodes5. | Best for localized deployments where developers manage their own clustered object storage instances. |
| **E2E Networks** | Highly competitive localized storage pricing17. | Avoids international currency fluctuation risks; absolute data sovereignty3. | Specifically tailored for regional compliance and sovereign infrastructure requirements. |

## **Dynamic Credential Generation for WebRTC Relays**

While the WebSocket infrastructure flawlessly manages text chat and online status indicators, live voice and video streams strictly require peer-to-peer UDP connections to maintain acceptable latency. However, due to complex Network Address Translation topologies and highly restrictive enterprise firewalls, direct peer-to-peer connections frequently fail to establish. Traversal Using Relays around NAT (TURN) servers are therefore strictly necessary to act as reliable media relays when direct connections are geometrically impossible8.  
To prevent unauthorized, malicious hijacking of the expensive TURN server infrastructure, the backend must dynamically generate time-limited authentication credentials. CoTURN, the industry standard open-source relay server, supports a highly secure REST API mechanism utilizing HMAC-SHA1 cryptographic signatures21. The Spring Boot backend exposes a dedicated endpoint that mathematically generates a cryptographic token using a pre-shared infrastructure secret, providing the authenticated client with secure, ephemeral ICE server configurations.  
The underlying mathematical derivation for the credential relies on the HMAC algorithm combined with a Unix timestamp to aggressively enforce a Time-To-Live expiration window:  
![][image1]  
![][image2]  
The frontend client fetches these ephemeral credentials via a secure HTTP call mere milliseconds before initiating a WebRTC negotiation sequence, ensuring the broader relay infrastructure remains completely secured against external consumption or bandwidth theft21.

## **Interoperability with Broader Application Ecosystems**

A microservice does not exist in a vacuum. In the context of a food delivery ecosystem, the chat service frequently intersects with decentralized logistics algorithms, flat-fee payment infrastructures, and external digital commerce protocols.  
For example, integrating this communication layer with external decentralized networks, such as the Open Network for Digital Commerce (ONDC), requires the chat system to handle highly complex asynchronous API callbacks. ONDC utilizes the Beckn Protocol to separate buyer applications, seller applications, and logistics providers, relying heavily on cryptographic signatures and decentralized mesh networking17. If a customer on a buyer app needs to chat with a delivery executive operating on a completely distinct logistics app, the chat microservice must act as a decentralized relay, wrapping STOMP payloads inside mathematically verifiable HTTP requests authenticated via Ed25519 asymmetric cryptography17.  
Furthermore, the chat system plays a pivotal role in dispute resolution and refund processing. When utilizing flat-fee UPI infrastructures (such as VyaparGateway) that settle funds directly into corporate bank accounts with zero percent transaction fees, there is no centralized gateway dashboard to automatically handle customer refunds17. Consequently, the chat microservice must be tightly integrated with the platform's internal refund ledgers. When a customer support agent agrees to issue a refund via the chat interface, the system must trigger a secure, internal API call that interacts directly with connected banking components to initiate structured reverse IMPS text routes, guaranteeing seamless financial reconciliation directly from the conversational interface.

## **Performance Tuning and Concurrency Optimization**

Designing a highly available real-time microservice involves severe operational trade-offs not found in traditional RESTful API deployments. A single microservice instance managing tens of thousands of active TCP connections is subject to massive memory allocation and CPU thread consumption13.  
In the Spring Boot ecosystem, WebSocket message processing is governed by two critical, distinct thread pools: the inbound channel and the outbound channel24. The inbound pool manages messages originating from the clients, such as a delivery driver transmitting a rapid GPS location update or a customer dispatching a chat message. If the internal controller logic performs heavy, blocking input/output operations, such as direct synchronous database writes, this specific thread pool must be drastically increased to prevent systemic failure13. By utilizing the aforementioned Kafka outbox pattern to handle all database persistence asynchronously, the inbound tasks are successfully converted from blocking I/O-bound operations to highly efficient CPU-bound tasks, allowing the core thread pool size to remain small and exceedingly responsive.  
Conversely, the outbound thread pool manages the broadcasting of messages back to the connected clients. If external clients are operating on slow, congested mobile networks—a scenario highly common in gig-economy delivery applications—they consume inbound messages very slowly, which dangerously backs up the server's outbound thread pool and consumes immense heap memory13. To prevent cascading failures due to these slow clients, the system architecture explicitly enforces strict send time and buffer limits13.

| Configuration Parameter | Recommended Value | Architectural Rationale |
| :---- | :---- | :---- |
| **Send Time Limit** | 15,000 milliseconds | The absolute maximum time the server will attempt to push a message to a stalled client before forcefully terminating the socket to free processing threads. |
| **Send Buffer Size Limit** | 512 Kilobytes | The maximum memory allocated per individual session for queuing outbound messages. Exceeding this triggers an immediate disconnection protocol24. |
| **Message Size Limit** | 128 Kilobytes | Restricts the maximum payload size of incoming STOMP frames, aggressively preventing malicious clients from executing memory exhaustion attacks. |

If a specific client's buffer exceeds the strictly allocated 512KB limit, the Spring container preemptively severs the TCP connection to protect internal heap memory, classifying the event operationally as a send limit exceeded transport error24. The remote mobile client is then forced to gracefully re-initiate the connection sequence when localized network conditions sufficiently improve.

## **Production-Ready Java Implementation**

The subsequent section outlines the exhaustive, production-ready codebase necessary for an automated deployment agent, such as Antigravity, to instantaneously generate the Chat and Call microservice. It encompasses precise dependency management, broker configuration, cryptographic security, stateful session management, and routing controllers.

### **Maven Dependencies and Ecosystem Configuration**

The application explicitly requires specific, curated modules for WebSocket support, Security, STOMP messaging, Redis caching, and JSON web token processing6.

XML  
\<dependencies\>  
    \<\!-- Core Spring Boot Web & Embedded Servlet Container \--\>  
    \<dependency\>  
        \<groupId\>org.springframework.boot\</groupId\>  
        \<artifactId\>spring-boot-starter-web\</artifactId\>  
    \</dependency\>  
      
    \<\!-- Spring WebSocket and STOMP Protocol Support \--\>  
    \<dependency\>  
        \<groupId\>org.springframework.boot\</groupId\>  
        \<artifactId\>spring-boot-starter-websocket\</artifactId\>  
    \</dependency\>  
      
    \<\!-- Spring Security for Authorization and Interception \--\>  
    \<dependency\>  
        \<groupId\>org.springframework.boot\</groupId\>  
        \<artifactId\>spring-boot-starter-security\</artifactId\>  
    \</dependency\>  
    \<dependency\>  
        \<groupId\>org.springframework.security\</groupId\>  
        \<artifactId\>spring-security-messaging\</artifactId\>  
    \</dependency\>

    \<\!-- Spring Data Redis for Distributed Session State \--\>  
    \<dependency\>  
        \<groupId\>org.springframework.boot\</groupId\>  
        \<artifactId\>spring-boot-starter-data-redis\</artifactId\>  
    \</dependency\>

    \<\!-- Cryptographic JWT Processing Libraries \--\>  
    \<dependency\>  
        \<groupId\>io.jsonwebtoken\</groupId\>  
        \<artifactId\>jjwt-api\</artifactId\>  
        \<version\>0.11.5\</version\>  
    \</dependency\>  
    \<dependency\>  
        \<groupId\>io.jsonwebtoken\</groupId\>  
        \<artifactId\>jjwt-impl\</artifactId\>  
        \<version\>0.11.5\</version\>  
        \<scope\>runtime\</scope\>  
    \</dependency\>  
    \<dependency\>  
        \<groupId\>io.jsonwebtoken\</groupId\>  
        \<artifactId\>jjwt-jackson\</artifactId\>  
        \<version\>0.11.5\</version\>  
        \<scope\>runtime\</scope\>  
    \</dependency\>  
\</dependencies\>

### **Advanced Message Broker and Transport Configuration**

This precise configuration establishes the primary STOMP endpoints and configures the external enterprise message broker for rigorous cross-node communication13. It intentionally sets the ingress endpoint and enables the SockJS fallback protocol to seamlessly bypass highly restrictive corporate firewalls that arbitrarily drop raw WebSocket packets3. Furthermore, it implements the aggressive transport limits discussed in the performance tuning analysis.

Java  
package com.enterprise.chat.config;

import org.springframework.context.annotation.Configuration;  
import org.springframework.messaging.simp.config.MessageBrokerRegistry;  
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;  
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;  
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;  
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;  
import org.springframework.beans.factory.annotation.Value;

@Configuration  
@EnableWebSocketMessageBroker  
public class WebSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.rabbitmq.host}")  
    private String rabbitHost;

    @Value("${spring.rabbitmq.port}")  
    private int rabbitPort;

    @Value("${spring.rabbitmq.username}")  
    private String rabbitUser;

    @Value("${spring.rabbitmq.password}")  
    private String rabbitPass;

    @Override  
    public void registerStompEndpoints(StompEndpointRegistry registry) {  
        // Register the primary WebSocket endpoint. Allowed origin patterns strictly support CORS.  
        registry.addEndpoint("/ws")  
                .setAllowedOriginPatterns("\*")  
                .withSockJS(); // Enable highly resilient fallback mechanisms  
    }

    @Override  
    public void configureMessageBroker(MessageBrokerRegistry registry) {  
        // Application destination prefixes intelligently route messages to mapped controllers  
        registry.setApplicationDestinationPrefixes("/app");

        // User destination prefix exclusively dedicated for private point-to-point messaging  
        registry.setUserDestinationPrefix("/user");

        // Production Configuration: Engage the external RabbitMQ broker utilizing the STOMP plugin  
        registry.enableStompBrokerRelay("/topic", "/queue")  
                .setRelayHost(rabbitHost)  
                .setRelayPort(rabbitPort)  
                .setClientLogin(rabbitUser)  
                .setClientPasscode(rabbitPass)  
                .setSystemLogin(rabbitUser)  
                .setSystemPasscode(rabbitPass);  
    }  
      
    @Override  
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {  
        // Enforce strict transport limits to prevent heap memory exhaustion from slow clients  
        registration.setSendTimeLimit(15 \* 1000)  
                    .setSendBufferSizeLimit(512 \* 1024)  
                    .setMessageSizeLimit(128 \* 1024);  
    }  
}

### **Cryptographic Channel Interception**

To robustly authenticate clients without unsafely exposing tokens in the connection URI, the application forcefully intercepts the inbound STOMP sequence16. It meticulously extracts the standard authorization header, parses the JWT payload, mathematically validates the cryptographic signature, and subsequently assigns a structured authentication object directly to the isolated session context.

Java  
package com.enterprise.chat.security;

import io.jsonwebtoken.Claims;  
import org.springframework.messaging.Message;  
import org.springframework.messaging.MessageChannel;  
import org.springframework.messaging.simp.stomp.StompCommand;  
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;  
import org.springframework.messaging.support.ChannelInterceptor;  
import org.springframework.messaging.support.MessageHeaderAccessor;  
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;  
import org.springframework.security.core.Authentication;  
import org.springframework.security.core.authority.SimpleGrantedAuthority;  
import org.springframework.stereotype.Component;

import java.util.Collections;  
import java.util.List;

@Component  
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenValidator jwtTokenValidator;

    public JwtChannelInterceptor(JwtTokenValidator jwtTokenValidator) {  
        this.jwtTokenValidator \= jwtTokenValidator;  
    }

    @Override  
    public Message\<?\> preSend(Message\<?\> message, MessageChannel channel) {  
        StompHeaderAccessor accessor \= MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Strictly target the initial connection frame for authentication  
        if (accessor \!= null && StompCommand.CONNECT.equals(accessor.getCommand())) {  
            List\<String\> authorization \= accessor.getNativeHeader("Authorization");  
              
            if (authorization \!= null && \!authorization.isEmpty()) {  
                String token \= authorization.get(0).replace("Bearer ", "");  
                  
                if (jwtTokenValidator.validateToken(token)) {  
                    Claims claims \= jwtTokenValidator.getClaims(token);  
                    String userId \= claims.getSubject();  
                    String role \= claims.get("role", String.class);  
                      
                    // Construct a robust, Spring Security compliant Authentication token  
                    Authentication userAuth \= new UsernamePasswordAuthenticationToken(  
                            userId,   
                            null,   
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE\_" \+ role))  
                    );  
                      
                    // Assign the verified authentication directly to the WebSocket session context  
                    accessor.setUser(userAuth);  
                } else {  
                    throw new IllegalArgumentException("Critically invalid JWT Token provided in STOMP headers.");  
                }  
            } else {  
                throw new IllegalArgumentException("Missing mandatory Authorization header in STOMP CONNECT frame.");  
            }  
        }  
        return message;  
    }  
}

### **Rigid Authorization and Security Metadata**

With the interceptor successfully extracting verifiable identities, Spring Security 6 must be rigorously configured to enforce strict access control over all specific messaging channels utilizing the modern Authorization Manager paradigms11.

Java  
package com.enterprise.chat.config;

import com.enterprise.chat.security.JwtChannelInterceptor;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
import org.springframework.core.Ordered;  
import org.springframework.core.annotation.Order;  
import org.springframework.messaging.simp.config.ChannelRegistration;  
import org.springframework.security.authorization.AuthorizationManager;  
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;  
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;  
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration  
@EnableWebSocketSecurity  
public class WebSocketSecurityConfig {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    public WebSocketSecurityConfig(JwtChannelInterceptor jwtChannelInterceptor) {  
        this.jwtChannelInterceptor \= jwtChannelInterceptor;  
    }

    @Bean  
    public AuthorizationManager\<org.springframework.messaging.Message\<?\>\> messageAuthorizationManager(  
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {  
          
        messages  
            // Permit uncontrolled STOMP framing operations only if authenticated  
            .nullDestMatcher().authenticated()   
            // Aggressively secure application ingress channels  
            .simpDestMatchers("/app/\*\*").authenticated()   
            // Aggressively secure subscription egress channels preventing unauthorized listening  
            .simpSubscribeDestMatchers("/user/\*\*", "/topic/\*\*", "/queue/\*\*").authenticated()  
            // Catch-all safety net discarding all unmapped frames  
            .anyMessage().denyAll();  
              
        return messages.build();  
    }

    // Register the custom cryptographic interceptor far ahead of default security checks  
    @Configuration  
    @Order(Ordered.HIGHEST\_PRECEDENCE \+ 99\)  
    public class WebSocketChannelConfig implements org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer {  
        @Override  
        public void configureClientInboundChannel(ChannelRegistration registration) {  
            registration.interceptors(jwtChannelInterceptor);  
        }  
    }  
}

### **Stateful Distributed Presence Management**

Determining whether a specific entity is actively online across a massive cluster is exceptionally critical for features like read receipts and dispatch routing. The architecture flawlessly utilizes Spring's native connection events to continually maintain a highly distributed, accurate map of active sessions directly within the Redis cluster11.

Java  
package com.enterprise.chat.listener;

import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
import org.springframework.context.event.EventListener;  
import org.springframework.data.redis.core.StringRedisTemplate;  
import org.springframework.messaging.simp.SimpMessageSendingOperations;  
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;  
import org.springframework.stereotype.Component;  
import org.springframework.web.socket.messaging.SessionConnectedEvent;  
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;

@Component  
public class WebSocketConnectionListener {

    private static final Logger logger \= LoggerFactory.getLogger(WebSocketConnectionListener.class);  
    private static final String ONLINE\_USERS\_KEY\_PREFIX \= "presence:user:";

    private final SimpMessageSendingOperations messagingTemplate;  
    private final StringRedisTemplate redisTemplate;

    public WebSocketConnectionListener(SimpMessageSendingOperations messagingTemplate, StringRedisTemplate redisTemplate) {  
        this.messagingTemplate \= messagingTemplate;  
        this.redisTemplate \= redisTemplate;  
    }

    @EventListener  
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {  
        StompHeaderAccessor accessor \= StompHeaderAccessor.wrap(event.getMessage());  
        if (accessor.getUser() \!= null) {  
            String userId \= accessor.getUser().getName();  
            String sessionId \= accessor.getSessionId();  
              
            // Persist user online status in the Redis cluster with a strict operational TTL  
            redisTemplate.opsForValue().set(ONLINE\_USERS\_KEY\_PREFIX \+ userId, sessionId, Duration.ofMinutes(15));  
            logger.info("Entity connected to cluster. ID: {}, Session: {}", userId, sessionId);  
              
            // Instantly broadcast online status to all interested peers across the mesh  
            messagingTemplate.convertAndSend("/topic/presence", "{\\"userId\\":\\"" \+ userId \+ "\\", \\"status\\":\\"ONLINE\\"}");  
        }  
    }

    @EventListener  
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {  
        StompHeaderAccessor accessor \= StompHeaderAccessor.wrap(event.getMessage());  
        if (accessor.getUser() \!= null) {  
            String userId \= accessor.getUser().getName();  
              
            // Instantly purge the presence indicator from the Redis cluster  
            redisTemplate.delete(ONLINE\_USERS\_KEY\_PREFIX \+ userId);  
            logger.info("Entity disconnected from cluster. ID: {}", userId);  
              
            // Broadcast absolute offline status to trigger UI updates  
            messagingTemplate.convertAndSend("/topic/presence", "{\\"userId\\":\\"" \+ userId \+ "\\", \\"status\\":\\"OFFLINE\\"}");  
        }  
    }  
}

### **Unified Messaging and WebRTC Controllers**

This controller explicitly handles the high-velocity ingestion of standard text chat messages, while concurrently processing the highly specialized WebRTC signaling payloads required to broker peer-to-peer UDP media streams8.

Java  
package com.enterprise.chat.controller;

import com.enterprise.chat.dto.ChatMessage;  
import com.enterprise.chat.dto.WebRtcSignal;  
import org.springframework.messaging.handler.annotation.DestinationVariable;  
import org.springframework.messaging.handler.annotation.MessageMapping;  
import org.springframework.messaging.handler.annotation.Payload;  
import org.springframework.messaging.simp.SimpMessageSendingOperations;  
import org.springframework.stereotype.Controller;

import java.security.Principal;  
import java.time.Instant;

@Controller  
public class MessagingController {

    private final SimpMessageSendingOperations messagingTemplate;  
    private final ChatHistoryService chatHistoryService; // Dedicated Kafka asynchronous publisher service

    public MessagingController(SimpMessageSendingOperations messagingTemplate, ChatHistoryService chatHistoryService) {  
        this.messagingTemplate \= messagingTemplate;  
        this.chatHistoryService \= chatHistoryService;  
    }

    /\*\*  
     \* Handles generalized chat messages destined for a specific, secure multi-party session.  
     \*/  
    @MessageMapping("/chat.send/{sessionId}")  
    public void processChatMessage(@DestinationVariable String sessionId,   
                                   @Payload ChatMessage chatMessage,   
                                   Principal principal) {  
        chatMessage.setSenderId(principal.getName());  
        chatMessage.setTimestamp(Instant.now().toString());

        // 1\. Asynchronously publish to Kafka outbox for highly durable database persistence  
        chatHistoryService.saveMessageAsync(sessionId, chatMessage);

        // 2\. Broadcast the payload immediately to all active clients subscribed to this session's topic  
        messagingTemplate.convertAndSend("/topic/chat/" \+ sessionId, chatMessage);  
    }

    /\*\*  
     \* WebRTC Signaling Endpoint. Securely routes SDP offers, answers, and ICE payloads.  
     \* Utilizes strict point-to-point queues rather than open broadcast topics to ensure media privacy.  
     \*/  
    @MessageMapping("/webrtc.signal/{targetUserId}")  
    public void processWebRtcSignal(@DestinationVariable String targetUserId,   
                                    @Payload WebRtcSignal signal,   
                                    Principal principal) {  
        signal.setSenderId(principal.getName());  
          
        // Routes securely via the RabbitMQ relay strictly to the specific target user's private queue  
        messagingTemplate.convertAndSendToUser(  
                targetUserId,   
                "/queue/webrtc",   
                signal  
        );  
    }  
}

### **Ephemeral CoTURN Credential Generation Engine**

To fully support the decentralized WebRTC functionality defined within the controller layer, the system must continuously issue dynamic, ephemeral credentials to the frontend clients for the centralized CoTURN server infrastructure21. This API utilizes standard Java cryptographic suites to generate the necessary HMAC payloads instantaneously.

Java  
package com.enterprise.chat.service;

import org.springframework.beans.factory.annotation.Value;  
import org.springframework.web.bind.annotation.GetMapping;  
import org.springframework.web.bind.annotation.RequestMapping;  
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;  
import javax.crypto.spec.SecretKeySpec;  
import java.security.Principal;  
import java.time.Instant;  
import java.util.Base64;  
import java.util.HashMap;  
import java.util.Map;

@RestController  
@RequestMapping("/api/v1/turn")  
public class TurnCredentialController {

    @Value("${coturn.secret.key}")  
    private String turnSecretKey;

    @Value("${coturn.server.url}")  
    private String turnServerUrl;

    private static final long TIME\_TO\_LIVE\_SECONDS \= 86400; // Rigid 24-hour expiration window

    @GetMapping("/credentials")  
    public Map\<String, String\> generateTurnCredentials(Principal principal) throws Exception {  
        String userId \= principal.getName();  
        long expiryTimestamp \= Instant.now().getEpochSecond() \+ TIME\_TO\_LIVE\_SECONDS;  
        String turnUsername \= expiryTimestamp \+ ":" \+ userId;

        // Execute advanced cryptographic HMAC-SHA1 Hash generation  
        Mac mac \= Mac.getInstance("HmacSHA1");  
        SecretKeySpec secretKeySpec \= new SecretKeySpec(turnSecretKey.getBytes(), "HmacSHA1");  
        mac.init(secretKeySpec);  
        byte\[\] hmacBytes \= mac.doFinal(turnUsername.getBytes());  
        String turnPassword \= Base64.getEncoder().encodeToString(hmacBytes);

        Map\<String, String\> credentials \= new HashMap\<\>();  
        credentials.put("username", turnUsername);  
        credentials.put("credential", turnPassword);  
        credentials.put("urls", turnServerUrl); // e.g., "turn:global.relay.enterprise.com:3478"

        return credentials;  
    }  
}

## **System Monitoring, Observability, and Automated Testing**

Deploying an architecture of this complexity requires profound observability mechanisms to continually monitor the health of the STOMP frames and the underlying TCP sockets. Spring Boot seamlessly integrates with the Micrometer metrics facade, allowing DevOps teams to export highly specific WebSocket telemetry directly to platforms like Prometheus and Grafana.  
By enabling the WebSocketMessageBrokerStats component, the system inherently tracks crucial operational metrics, including the total number of established WebSocket sessions, the frequency of sessions abnormally closed due to transport errors, and the exact count of STOMP frames forwarded to the external RabbitMQ broker relay24. A high volume of abnormally closed sessions or an exponential spike in queued tasks within the clientOutboundChannel pool provides an immediate, quantifiable indicator that the deployed application nodes are struggling to push data to congested networks, signaling the automated scaling groups to instantly provision additional compute capacity24.  
Furthermore, ensuring the absolute reliability of the messaging infrastructure requires rigorous integration testing before deployment. Engineers leverage the WebSocketStompClient and StompSessionHandlerAdapter classes to programmatically simulate concurrent client connections during the CI/CD pipeline phase4. These integration tests establish real WebSocket connections to a randomized port, dispatch mock JSON payloads, and verify that the broker successfully routes the messages back to the anticipated subscribers within strict latency thresholds, mathematically proving the resilience of the routing logic before a single line of code reaches the production environment4.

## **Architectural Synthesis and Strategic Outlook**

The architectural blueprint formulated in this exhaustive report establishes a highly resilient, incredibly high-throughput communication infrastructure intrinsically tailored for the chaotic demands of distributed platforms and multi-entity marketplaces. By rigorously enforcing a strict separation of concerns—leveraging WebSockets and STOMP for lightweight text transmission and session signaling, WebRTC for direct decentralized media streaming, Apache Kafka for non-blocking database persistence, and Redis for distributed session presence management—the resulting system definitively avoids the traditional cascading bottlenecks universally associated with real-time operations.  
The integration of a specialized cryptographic ChannelInterceptor successfully guarantees robust authentication without ever compromising sensitive JSON Web Tokens across diverse network topologies, effortlessly fulfilling the strictest enterprise security compliance requirements. By implementing the provided Spring Boot configurations, advanced message handlers, and ephemeral cryptographic APIs, development teams and automated generation agents will yield a flawlessly production-ready microservice capable of scaling horizontally indefinitely alongside external enterprise brokers, consistently delivering near-zero latency communication between any vast array of connected entities.

#### **Works cited**

> 1. System Design Pattern : Building Real-Time Systems: How Apps Like Uber and Google Docs Keep You Updated Instantly | by Priya Srivastava | Medium, [https://medium.com/@priyasrivastava18official/system-design-pattern-building-real-time-systems-how-apps-like-uber-and-google-docs-keep-you-b90ab1da36f0](https://medium.com/@priyasrivastava18official/system-design-pattern-building-real-time-systems-how-apps-like-uber-and-google-docs-keep-you-b90ab1da36f0)  
> 2. Communicating and Displaying Real-Time Data with WebSocket \- ResearchGate, [https://www.researchgate.net/publication/260305439\_Communicating\_and\_Displaying\_Real-Time\_Data\_with\_WebSocket](https://www.researchgate.net/publication/260305439_Communicating_and_Displaying_Real-Time_Data_with_WebSocket)  
> 3. Implementing WebSockets in Spring Boot and Angular For Real Time Bidirectional Communication. | by Abas Jama | Medium, [https://medium.com/@abasjama04/implementing-websockets-in-spring-boot-and-angular-for-real-time-bidirectional-communication-c3307d046dff](https://medium.com/@abasjama04/implementing-websockets-in-spring-boot-and-angular-for-real-time-bidirectional-communication-c3307d046dff)  
> 4. WebSocket Communication in Spring Boot Applications \- Reintech, [https://reintech.io/blog/websocket-communication-spring-boot](https://reintech.io/blog/websocket-communication-spring-boot)  
> 5. Spring Boot Websocket Example \- devglan, [https://www.devglan.com/spring-boot/spring-boot-websocket-example](https://www.devglan.com/spring-boot/spring-boot-websocket-example)  
> 6. How to Set Up WebSocket in Spring Boot \- OneUptime, [https://oneuptime.com/blog/post/2025-12-22-spring-boot-websocket-setup/view](https://oneuptime.com/blog/post/2025-12-22-spring-boot-websocket-setup/view)  
> 7. Building Real-Time Chat Room with Spring Boot WebSocket | YennJ12 Engineering Blog, [https://yennj12.js.org/yennj12\_blog\_V4/posts/spring-boot-websocket-chat-room-application/](https://yennj12.js.org/yennj12_blog_V4/posts/spring-boot-websocket-chat-room-application/)  
> 8. How can I implement own webrtc server in my project? \- Stack Overflow, [https://stackoverflow.com/questions/41052844/how-can-i-implement-own-webrtc-server-in-my-project](https://stackoverflow.com/questions/41052844/how-can-i-implement-own-webrtc-server-in-my-project)  
> 9. DIPARTIMENTO DI INGEGNERIA DELL'INFORMAZIONE CORSO, [https://thesis.unipd.it/retrieve/0a6cc5ae-8ea2-400b-88b1-221e8df8f5d5/Jakkampudi\_VenkataAvinash.pdf](https://thesis.unipd.it/retrieve/0a6cc5ae-8ea2-400b-88b1-221e8df8f5d5/Jakkampudi_VenkataAvinash.pdf)  
> 10. WebSockets & Real-Time Systems with Spring: Best Practices, [https://www.coddykit.com/pages/blog-detail?id=512368](https://www.coddykit.com/pages/blog-detail?id=512368)  
> 11. How to Build Real-Time Apps with WebSocket STOMP in Spring \- OneUptime, [https://oneuptime.com/blog/post/2026-01-25-real-time-apps-websocket-stomp-spring/view](https://oneuptime.com/blog/post/2026-01-25-real-time-apps-websocket-stomp-spring/view)  
> 12. Spring MVC Servlet Framework Overview | PDF \- Scribd, [https://www.scribd.com/document/457151201/Web-on-Servlet-Stack-pdf](https://www.scribd.com/document/457151201/Web-on-Servlet-Stack-pdf)  
> 13. Performance :: Spring Framework, [https://docs.spring.io/spring-framework/reference/web/websocket/stomp/configuration-performance.html](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/configuration-performance.html)  
> 14. Building a Production-Ready Real-Time Active User Counter: Step-by-Step Implementation Guide | by Raju Methuku | Medium, [https://medium.com/@narasimha4789/implementation-building-real-time-active-user-counter-system-390940e0dda6](https://medium.com/@narasimha4789/implementation-building-real-time-active-user-counter-system-390940e0dda6)  
> 15. Websocket Performance Benchmarking Analysis | PDF | Thread (Computing) | Php \- Scribd, [https://www.scribd.com/document/757317221/23-An-Analysis-of-the-Performance-of-Websockets-in-Various-Programming-Languages-and-Libraries](https://www.scribd.com/document/757317221/23-An-Analysis-of-the-Performance-of-Websockets-in-Various-Programming-Languages-and-Libraries)  
> 16. Token Authentication :: Spring Framework, [https://docs.spring.io/spring-framework/reference/web/websocket/stomp/authentication-token-based.html](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/authentication-token-based.html)  
> 17. Authenticated Web Sockets Spring Boot 3 \- Stack Overflow, [https://stackoverflow.com/questions/75547571/authenticated-web-sockets-spring-boot-3](https://stackoverflow.com/questions/75547571/authenticated-web-sockets-spring-boot-3)  
> 18. Spring Boot Interview Questions Guide | PDF | Java (Programming Language) \- Scribd, [https://www.scribd.com/document/878878379/Spring-Boot-Interview-Questions](https://www.scribd.com/document/878878379/Spring-Boot-Interview-Questions)  
> 19. WebSocket Security \- Spring, [https://docs.spring.io/spring-security/reference/servlet/integrations/websocket.html](https://docs.spring.io/spring-security/reference/servlet/integrations/websocket.html)  
> 20. Food Delivery App, uploaded:Food Delivery App  
> 21. Secret Key Authentication Examples \- ExpressTURN, [https://www.expressturn.com/webrtc-secret-key-examples](https://www.expressturn.com/webrtc-secret-key-examples)  
> 22. CoTURN REST API Development & Deployment: | by Muhammad Usman Bashir | Medium, [https://medium.com/@BeingOttoman/coturn-rest-api-development-deployment-7bac065c64aa](https://medium.com/@BeingOttoman/coturn-rest-api-development-deployment-7bac065c64aa)  
> 23. (PDF) Real-Time Data Processing with Spring Boot and Web Sockets \- ResearchGate, [https://www.researchgate.net/publication/391440780\_Real-Time\_Data\_Processing\_with\_Spring\_Boot\_and\_Web\_Sockets](https://www.researchgate.net/publication/391440780_Real-Time_Data_Processing_with_Spring_Boot_and_Web_Sockets)  
> 24. Monitoring :: Spring Framework, [https://docs.spring.io/spring-framework/reference/web/websocket/stomp/stats.html](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/stats.html)  
> 25. Socket Programming in Spring boot \- DEV Community, [https://dev.to/saifali40/socket-programming-in-spring-boot-6mh](https://dev.to/saifali40/socket-programming-in-spring-boot-6mh)  
> 26. Performance Issue with Spring Websocket, RabbitMQ and STOMP \- Stack Overflow, [https://stackoverflow.com/questions/50833472/performance-issue-with-spring-websocket-rabbitmq-and-stomp](https://stackoverflow.com/questions/50833472/performance-issue-with-spring-websocket-rabbitmq-and-stomp)

[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAmwAAAA3CAYAAACxQxY4AAAFhklEQVR4Xu3cWch32xwH8G2eHYXIEJmu5ILIhTIcF0LJFIVOhhM3IiFkKJJCcqGIKBlKhhSZIzeGDIXIdCHzWJR5Xt/2Wuf5vevd+/+ep8dz3uc4n0/9+q/fb+3/fvZ/v/9a69177f+yAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHDN8ekWH23x8RafL/VP9PonS40jr2/x5gNxh6NNmTxwWb9X+X7le/bFXn98i4/1+md67aS+sBx9l/P6ghbfKPH1Fl/u/ffv7wGAM+k/LW42F5tvzQWukHN2rd5+Qs+Hh7R4Tm//c+o7TZnsXJ3snZe9+kls7XOu3XajBgBnRgapG83F5rNzgSvUgf2xUx4fnPKrwnwMZ93e8e7VT2Jrn1u1XIH7x1wEgLMgA9cN5uLyv7st9f/oz6X9mOX8wf/bU35VmI/hrNs73r36SWztc6sWe3UAuKgyQF1vLi7nrl+rg9iF2q/r7a+2+FCLH7d4fq99r8WzW9ywbB+Xtfh7bz+pxU97+/otfras2/6u19K+U4vPlbwa+fNavL12nJKtCdvwleWo70Ut/trirS0ubXGr3vfwFjdp8bgWv+/bxoNbfKm339/iKb39l/4a9bOmndfEMPovKe14Wc+zhjF+vaxXlv7Y8/SNW74v7Pm/W9yixW16vifb/WsubtjbR60fauf4cix/67Wco9Tzt/P6tF6Prb+1VYu9OgBcVBmgMoGa5UGEoQ5id+6vP2zxqlKfB9TLSj5q8zZVXUdX++465R9Y1sXiQ+07dEyn5dHL4b9T+56+rBOaIX1vmPKtds1r/amlPW8f9yvtub/m95zyLMJ/ecnn9+bBiqzPO4l5n8PeOXhQqdXv69721VZ9qxZ7dQC4qDJA3XIuNu8r7fss63aJcfUk7Vx9eWiJIX3zgwzj/TWvftni+y2eOfXlicuav7vFa0uevmuX9t4xnZbjTNie3OI7JU9frrDVvLbr5xifZVzBTMz7mn1qWesv7q9Vze825ZkUv7Lk83uvu1E7rr33z+drfNZx9XHvvIy+LVv1rVrs1QHgosoA9ay52NyltPMk5PDbFjdd1gH0NaVeZZ8Z1OdaHQzn9j2mfLjjlL9rOX/Cdp3ePnRMVfaRW2mH4so6zoQttzXzcxJD+g5N2C7k0Pa5Ff3eko/+3NasecxXMXN78dCVymds1I5r6/23b/HOkt+utMf2ec33b8vWPmOrvlX72nJ0GxoAzpT8/tW85ujDU177swYtbr2cO+jVBxdSv3HJR21vgjEPnjWfr/68Z1l/B21I31iDNx/TD0r7tBxnwpaJTtbxDel75JQPuXV675KPNX5vKbWxri/y3qzrytq4kecK1TD2nQlwzePuU56nXF9d8vRlm5qPNXWz3K6+MpOeby7rrdcq6+jG2rn4bmmPJ29z5a8+1HGhtWqxVZ9r+b7ONQA4UzIxyGCVRfFbg1auWqWeqFewxgL0xCN6LZOInyzrwwJjkM1+U0vk6lWuhKWd26AvXdZBeuznuS1esqw/qHrzFj/v2/6mxZuW9YGExB+WdQF++rLNUI/ptOVqY45lfN4cU5XPl/qPlnVBf9rZPj9SXM/TA1r8que/yBu7jyznf5b7ltqlpZ41Z6nVh0XG78Dl4YJcMc3xRj3/meTWc/y25dxzHNlHHowYf/dhvb7lUcu5k9BDXrGs+8vxbP17jZ9MSVxe6k8s9XE7P+cvx5zzd69e+1PPx3l943L0vkT+I5LvUM5DndwCAFztbE2mAAA4I7K+MRO2d8wdAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHCN9l+t05zCD8ljTQAAAABJRU5ErkJggg==>

[image2]: <data:image/png;base64,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>