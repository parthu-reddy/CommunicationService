const http = require('http');

async function testChat() {
    console.log("Logging in as Customer...");
    const loginRes = await fetch("http://140.245.234.137:8080/api/v1/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            phone: "+919876543210",
            password: "password123"
        })
    });
    
    if (!loginRes.ok) {
        console.error("Login failed!", await loginRes.text());
        return;
    }
    const loginData = await loginRes.json();
    const token = loginData.data.token;
    console.log("Token obtained!");

    console.log("\nFetching orders...");
    const ordersRes = await fetch("http://140.245.234.137:8080/api/v1/customers/me/orders", {
        headers: { "Authorization": `Bearer ${token}` }
    });
    
    if (!ordersRes.ok) {
        console.error("Failed to fetch orders", await ordersRes.text());
        return;
    }
    const ordersData = await ordersRes.json();
    const orders = ordersData.data.content;
    
    if (orders.length === 0) {
        console.log("No orders found to test chat.");
        return;
    }
    
    const order = orders[0];
    console.log("Using Order ID:", order.id);

    console.log("\nAttempting to initialize chat session...");
    const chatRes = await fetch("http://140.245.234.137:8080/api/v1/chat/sessions", {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${token}`,
            "Content-Type": "application/json",
            "X-Calling-Service": "CustomerApplication"
        },
        body: JSON.stringify({
            id: "",
            orderId: order.id,
            participants: [
                {
                    userId: order.deliveryExecutiveId,
                    entityType: "DELIVERY",
                    displayName: "Rider"
                },
                {
                    userId: order.restaurantId,
                    entityType: "RESTAURANT",
                    displayName: "Restaurant"
                },
                {
                    userId: loginData.data.user.id,
                    entityType: "CUSTOMER",
                    displayName: loginData.data.user.name
                }
            ].filter(p => p.userId != null)
        })
    });
    
    console.log(`Chat Response Status: ${chatRes.status}`);
    const chatData = await chatRes.text();
    console.log(`Chat Response Body: ${chatData}`);
}

testChat().catch(console.error);
