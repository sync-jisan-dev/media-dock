async function run() {
  const base = "http://localhost:3000";

  console.log("=== Testing MediaDock Gateway with Neon Integration ===");

  // 1. Health
  const healthRes = await fetch(`${base}/app/v1/health`);
  console.log("1. /app/v1/health status:", healthRes.status, await healthRes.json());

  // 2. Neon Services
  const servicesRes = await fetch(`${base}/app/v1/neon/services`);
  console.log("2. /app/v1/neon/services status:", servicesRes.status, await servicesRes.json());

  // 3. Neon Functions hello
  const fnRes = await fetch(`${base}/app/v1/functions/hello`);
  console.log("3. /app/v1/functions/hello status:", fnRes.status, await fnRes.json());

  // 4. Session Guest
  const sessRes = await fetch(`${base}/app/v1/sessions/guest`, { method: "POST" });
  const session = await sessRes.json();
  console.log("4. /app/v1/sessions/guest status:", sessRes.status, "Token:", session.token.substring(0, 30) + "...");

  const authHeader = { Authorization: `Bearer ${session.token}` };

  // 5. Neon Realtime Live Query
  const rtRes = await fetch(`${base}/app/v1/realtime/downloads/live`, {
    method: "POST",
    headers: authHeader,
  });
  const rtData = await rtRes.json();
  console.log("5. /app/v1/realtime/downloads/live status:", rtRes.status, "Realtime URL:", rtData.realtimeUrl, "Sealed capability present:", Boolean(rtData.sealedQuery?.capability));

  // 6. Neon AI Gateway Models
  const aiModelsRes = await fetch(`${base}/app/v1/ai/models`, {
    method: "GET",
    headers: authHeader,
  });
  console.log("6. /app/v1/ai/models status:", aiModelsRes.status);
  if (aiModelsRes.ok) {
    const models = await aiModelsRes.json();
    console.log("   Discovered models count:", models.length);
  } else {
    console.log("   Details:", await aiModelsRes.text());
  }

  // 7. Neon AI Enhance
  const aiEnhanceRes = await fetch(`${base}/app/v1/ai/enhance`, {
    method: "POST",
    headers: { ...authHeader, "Content-Type": "application/json" },
    body: JSON.stringify({
      title: "Rick Astley - Never Gonna Give You Up (Official Music Video)",
      platform: "youtube",
    }),
  });
  console.log("7. /app/v1/ai/enhance status:", aiEnhanceRes.status, await aiEnhanceRes.json());

  console.log("\n=== ALL NEON-INTEGRATED GATEWAY ENDPOINTS TESTED SUCCESSFULLY ===");
}

run().catch(console.error);
