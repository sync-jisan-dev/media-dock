import { Injectable, Logger } from "@nestjs/common";

export interface FunctionInvocationResult {
  slug: string;
  url: string;
  status: number;
  data: any;
}

@Injectable()
export class NeonFunctionsService {
  private readonly logger = new Logger(NeonFunctionsService.name);
  private readonly branchId = process.env.NEON_BRANCH_ID;
  private readonly hostSuffix = process.env.NEON_FUNCTION_HOST_SUFFIX;

  isConfigured(): boolean {
    return Boolean(this.branchId && this.hostSuffix);
  }

  getFunctionUrl(slug: string): string {
    if (!this.branchId || !this.hostSuffix) {
      throw new Error("Neon Functions is not configured (missing NEON_BRANCH_ID or NEON_FUNCTION_HOST_SUFFIX).");
    }
    return `https://${this.branchId}-${slug}${this.hostSuffix}/`;
  }

  async invokeFunction(slug: string, path = ""): Promise<FunctionInvocationResult> {
    const baseUrl = this.getFunctionUrl(slug);
    const fullUrl = path ? `${baseUrl.replace(/\/$/, "")}/${path.replace(/^\//, "")}` : baseUrl;

    this.logger.log(`Invoking Neon Function [${slug}] at: ${fullUrl}`);
    const res = await fetch(fullUrl, {
      method: "GET",
    });

    let data: any;
    const contentType = res.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
      data = await res.json();
    } else {
      data = await res.text();
    }

    return {
      slug,
      url: fullUrl,
      status: res.status,
      data,
    };
  }
}
