import re

file_path = "/Users/parthureddy/Documents/Food Delivery.nosync/CommunicationService/src/test/java/com/fooddelivery/chat/OpenApiGenerationTest.java"

with open(file_path, "r") as f:
    content = f.read()

# Add EntityManagerFactory and DataSource and Repositories mocks
mocks = """
    @MockBean
    private javax.sql.DataSource dataSource;

    @MockBean
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @MockBean
    private com.fooddelivery.chat.repository.SessionParticipantRepository sessionParticipantRepository;
    
    @MockBean
    private com.fooddelivery.chat.repository.ChatSessionRepository chatSessionRepository;
    
    @MockBean
    private com.fooddelivery.chat.repository.CallLogRepository callLogRepository;
    
    @MockBean
    private com.fooddelivery.chat.repository.ChatMessageRepository chatMessageRepository;
"""

if "private javax.sql.DataSource dataSource;" not in content:
    content = content.replace("private MockMvc mockMvc;", mocks + "\n    @Autowired(required = false)\n    private MockMvc mockMvc;")
    
    with open(file_path, "w") as f:
        f.write(content)
