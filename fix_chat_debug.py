import re

file_path = "/Users/parthureddy/Documents/Food Delivery.nosync/CommunicationService/src/test/java/com/fooddelivery/chat/OpenApiGenerationTest.java"

with open(file_path, "r") as f:
    content = f.read()

# Add "debug=true" to the properties list
content = content.replace('"server.port=0"', '"server.port=0", "debug=true"')

with open(file_path, "w") as f:
    f.write(content)
