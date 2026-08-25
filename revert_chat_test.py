import re

file_path = "/Users/parthureddy/Documents/Food Delivery.nosync/CommunicationService/src/test/java/com/fooddelivery/chat/OpenApiGenerationTest.java"

with open(file_path, "r") as f:
    content = f.read()

# Regex to remove the mock beans we added
content = re.sub(r'\s*@MockBean\s+private javax\.sql\.DataSource dataSource;', '', content)
content = re.sub(r'\s*@MockBean\s+private jakarta\.persistence\.EntityManagerFactory entityManagerFactory;', '', content)
content = re.sub(r'\s*@MockBean\s+private com\.fooddelivery\.chat\.repository\.[a-zA-Z]+Repository [a-zA-Z]+Repository;', '', content)
content = re.sub(r'\s*@MockBean\s+private com\.fooddelivery\.common\.repository\.[a-zA-Z]+Repository [a-zA-Z]+Repository;', '', content)
content = re.sub(r'\s*@MockBean\s+private com\.fooddelivery\.common\.outbox\.repository\.[a-zA-Z]+Repository [a-zA-Z]+Repository;', '', content)

with open(file_path, "w") as f:
    f.write(content)
