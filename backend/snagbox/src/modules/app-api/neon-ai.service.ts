import { Injectable, Logger } from "@nestjs/common";

export interface AiModelInfo {
  id: string;
  name?: string;
  owned_by?: string;
  enabled?: boolean;
}

export interface MetadataEnhancement {
  title: string;
  cleanFilename: string;
  suggestedTags: string[];
  category: string;
  summary?: string;
}

@Injectable()
export class NeonAiService {
  private readonly logger = new Logger(NeonAiService.name);
  private readonly baseUrl = process.env.NEON_AI_GATEWAY_BASE_URL;
  private readonly token = process.env.NEON_AI_GATEWAY_TOKEN;

  isConfigured(): boolean {
    return Boolean(this.baseUrl && this.token);
  }

  getAiConfig() {
    return {
      enabled: this.isConfigured(),
      baseUrl: this.baseUrl || null,
    };
  }

  async listModels(): Promise<AiModelInfo[]> {
    if (!this.baseUrl || !this.token) {
      throw new Error("Neon AI Gateway is not configured (missing NEON_AI_GATEWAY_BASE_URL or NEON_AI_GATEWAY_TOKEN).");
    }

    const res = await fetch(`${this.baseUrl}/v1/models`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${this.token}`,
      },
    });

    if (!res.ok) {
      const errText = await res.text();
      this.logger.warn(`Neon AI Gateway returned HTTP ${res.status}: ${errText}`);
      throw new Error(`Neon AI Gateway error (${res.status}): ${errText}`);
    }

    const data: any = await res.json();
    const list: AiModelInfo[] = (data.data || []).map((m: any) => ({
      id: m.id,
      name: m.name || m.id,
      owned_by: m.owned_by,
      enabled: m.enabled ?? true,
    }));
    return list;
  }

  async enhanceMediaMetadata(title: string, platform?: string): Promise<MetadataEnhancement> {
    if (!this.baseUrl || !this.token) {
      // Fallback heuristics when AI Gateway is not configured
      const clean = title.replace(/[^a-zA-Z0-9_\-\. ]/g, "").trim().replace(/\s+/g, "_");
      return {
        title,
        cleanFilename: clean,
        suggestedTags: [platform || "media", "download"],
        category: "General",
      };
    }

    try {
      const prompt = `Analyze this media title from platform "${platform || "web"}": "${title}".
Provide a JSON object with:
1. "cleanFilename": a safe, clean filename without special characters (use underscores instead of spaces, max 60 chars).
2. "suggestedTags": array of 3-5 relevant lowercase tags.
3. "category": a high-level category (e.g., Music, Gaming, Tech, Education, Entertainment, News, Other).
4. "summary": a one-sentence clean description.
Respond ONLY with raw valid JSON, no markdown formatting.`;

      const res = await fetch(`${this.baseUrl}/v1/chat/completions`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${this.token}`,
        },
        body: JSON.stringify({
          model: "gpt-5-mini",
          messages: [{ role: "user", content: prompt }],
          temperature: 0.3,
        }),
      });

      if (!res.ok) {
        const err = await res.text();
        this.logger.warn(`AI Gateway completion failed (${res.status}): ${err}. Using heuristic fallback.`);
        const clean = title.replace(/[^a-zA-Z0-9_\-\. ]/g, "").trim().replace(/\s+/g, "_");
        return {
          title,
          cleanFilename: clean,
          suggestedTags: [platform || "media"],
          category: "General",
        };
      }

      const json: any = await res.json();
      const rawContent = json.choices?.[0]?.message?.content || "{}";
      const cleaned = rawContent.replace(/^```json/i, "").replace(/```$/i, "").trim();
      const parsed = JSON.parse(cleaned);

      return {
        title,
        cleanFilename: parsed.cleanFilename || title.replace(/\s+/g, "_"),
        suggestedTags: Array.isArray(parsed.suggestedTags) ? parsed.suggestedTags : [platform || "media"],
        category: parsed.category || "General",
        summary: parsed.summary,
      };
    } catch (err: any) {
      this.logger.warn(`AI metadata enhancement encountered error: ${err.message}. Using fallback.`);
      const clean = title.replace(/[^a-zA-Z0-9_\-\. ]/g, "").trim().replace(/\s+/g, "_");
      return {
        title,
        cleanFilename: clean,
        suggestedTags: [platform || "media"],
        category: "General",
      };
    }
  }
}
