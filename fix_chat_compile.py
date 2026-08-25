import re

file_path = "/Users/parthureddy/Documents/Food Delivery.nosync/CommunicationService/src/test/java/com/fooddelivery/chat/OpenApiGenerationTest.java"

with open(file_path, "r") as f:
    content = f.read()

# Replace multiple @Autowired on mockMvc
content = re.sub(r'(@Autowired\(required = false\)\s*)+private MockMvc mockMvc;', r'@Autowired(required = false)\n    private MockMvc mockMvc;', content)
content = re.sub(r'(@Autowired\s*)+private MockMvc mockMvc;', r'@Autowired(required = false)\n    private MockMvc mockMvc;', content)

with open(file_path, "w") as f:
    f.write(content)
