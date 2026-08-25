import re

file_path = "/Users/parthureddy/Documents/Food Delivery.nosync/CommunicationService/src/test/java/com/fooddelivery/chat/OpenApiGenerationTest.java"

with open(file_path, "r") as f:
    content = f.read()

mocks = """
    @MockBean
    private com.fooddelivery.common.repository.IIdempotencyKeyRepository idempotencyKeyRepository;
    
    @MockBean
    private com.fooddelivery.common.outbox.repository.OutboxEventRepository outboxEventRepository;
"""

if "IIdempotencyKeyRepository" not in content:
    content = content.replace("private MockMvc mockMvc;", mocks + "\n    @Autowired(required = false)\n    private MockMvc mockMvc;")
    
    with open(file_path, "w") as f:
        f.write(content)
