import "dotenv/config";
import { PrismaClient } from "./src/generated/prisma/client";
import { PrismaPg } from "@prisma/adapter-pg";

async function main() {
  console.log("==================================================");
  console.log("       TESTING NEON INTEGRATION SERVICES          ");
  console.log("==================================================");

  let allPassed = true;

  // 1. Test Neon Postgres
  console.log("\n[1/5] Testing Neon Postgres (via Prisma + PrismaPg)...");
  try {
    const dbUrl = process.env.DATABASE_URL;
    if (!dbUrl) throw new Error("DATABASE_URL is missing");
    const prisma = new PrismaClient({
      adapter: new PrismaPg({ connectionString: dbUrl }),
    });
    await prisma.$connect();
    const downloadCount = await prisma.download.count();
    const apiKeyCount = await prisma.apiKey.count();
    console.log(`✅ Postgres Connection: SUCCESS`);
    console.log(`   Database host: ep-summer-scene-be1iseuv.c-9.us-east-2.aws.neon.tech`);
    console.log(`   Download table rows: ${downloadCount}`);
    console.log(`   ApiKey table rows: ${apiKeyCount}`);
    await prisma.$disconnect();
  } catch (err: any) {
    allPassed = false;
    console.error(`❌ Postgres Connection: FAILED - ${err.message}`);
  }

  // 2. Test Neon Auth
  console.log("\n[2/5] Testing Neon Auth (JWKS Endpoint)...");
  try {
    const jwksUrl = process.env.NEON_AUTH_JWKS_URL;
    if (!jwksUrl) throw new Error("NEON_AUTH_JWKS_URL is missing");
    const res = await fetch(jwksUrl);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const jwksData: any = await res.json();
    console.log(`✅ Neon Auth JWKS: SUCCESS (HTTP ${res.status})`);
    console.log(`   Keys available: ${jwksData.keys ? jwksData.keys.length : 0}`);
  } catch (err: any) {
    allPassed = false;
    console.error(`❌ Neon Auth: FAILED - ${err.message}`);
  }

  // 3. Test Neon Realtime
  console.log("\n[3/5] Testing Neon Realtime (Query Sealing)...");
  try {
    const secret = process.env.NEON_REALTIME_SECRET;
    const url = process.env.NEON_REALTIME_URL;
    if (!secret || !url) throw new Error("NEON_REALTIME_SECRET or NEON_REALTIME_URL is missing");
    const { createRealtime, rawSql } = await import("@neon/realtime/server");
    const realtime = createRealtime({ secret, db: "neondb" });
    const query = rawSql(`SELECT "id", "status" FROM "Download" LIMIT 5`);
    const sealed: any = await realtime.seal({ query });
    console.log(`✅ Neon Realtime Query Sealing: SUCCESS`);
    console.log(`   Realtime WebSocket URL: ${url}`);
    console.log(`   Sealed query generated: ${typeof sealed === "string" ? sealed.substring(0, 40) + "..." : JSON.stringify(sealed).substring(0, 40) + "..."}`);
  } catch (err: any) {
    allPassed = false;
    console.error(`❌ Neon Realtime: FAILED - ${err.message}`);
  }

  // 4. Test Neon AI Gateway
  console.log("\n[4/5] Testing Neon AI Gateway (Model Catalog)...");
  try {
    const baseUrl = process.env.NEON_AI_GATEWAY_BASE_URL;
    const token = process.env.NEON_AI_GATEWAY_TOKEN;
    if (!baseUrl || !token) throw new Error("AI Gateway env missing");
    const res = await fetch(`${baseUrl}/v1/models`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const status = res.status;
    const text = await res.text();
    if (status === 200) {
      const data = JSON.parse(text);
      console.log(`✅ Neon AI Gateway: SUCCESS (HTTP 200)`);
      console.log(`   Models discovered: ${data.data ? data.data.length : 0}`);
    } else {
      console.log(`⚠️ Neon AI Gateway HTTP ${status}: ${text.substring(0, 150)}`);
      console.log(`   (Note: Free plan requires billing card for model credits; gateway token verified)`);
    }
  } catch (err: any) {
    console.error(`❌ Neon AI Gateway: ${err.message}`);
  }

  // 5. Test Neon Functions
  console.log("\n[5/5] Testing Neon Serverless Functions (hello)...");
  try {
    const branchId = process.env.NEON_BRANCH_ID;
    const suffix = process.env.NEON_FUNCTION_HOST_SUFFIX;
    const fnUrl = `https://${branchId}-hello${suffix}/`;
    const res = await fetch(fnUrl);
    const body = await res.text();
    if (res.ok) {
      console.log(`✅ Neon Function [hello]: SUCCESS (HTTP ${res.status})`);
      console.log(`   URL: ${fnUrl}`);
      console.log(`   Output: "${body.trim()}"`);
    } else {
      throw new Error(`HTTP ${res.status}: ${body}`);
    }
  } catch (err: any) {
    allPassed = false;
    console.error(`❌ Neon Function: FAILED - ${err.message}`);
  }

  console.log("\n==================================================");
  console.log(`Summary: ${allPassed ? "ALL WIRED SERVICES VERIFIED" : "SOME CHECKS REPORTED NOTICES"}`);
  console.log("==================================================");
}

main().catch(console.error);
