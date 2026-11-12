import { ImageExtractorService } from "./image-extractor.service";
import * as safeFetchModule from "../../common/http/safe-fetch";

describe("ImageExtractorService", () => {
  let service: ImageExtractorService;

  beforeEach(() => {
    service = new ImageExtractorService();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should detect direct image URL without fetching HTML", async () => {
    const result = await service.extract("https://images.unsplash.com/photo-nature.jpg");
    expect(result.totalFound).toBe(1);
    expect(result.images[0].url).toBe("https://images.unsplash.com/photo-nature.jpg");
    expect(result.images[0].format).toBe("jpg");
    expect(result.images[0].source).toBe("direct");
  });

  it("should extract Open Graph, Twitter cards, and img tags from HTML", async () => {
    const mockHtml = `
      <!DOCTYPE html>
      <html>
        <head>
          <title>Scenic Landscapes</title>
          <meta property="og:image" content="https://example.com/og-image.png" />
          <meta name="twitter:image" content="https://example.com/twitter-image.webp" />
          <script type="application/ld+json">
            {
              "@context": "https://schema.org",
              "@type": "ImageObject",
              "url": "https://example.com/jsonld-image.jpg",
              "width": 1920,
              "height": 1080
            }
          </script>
        </head>
        <body>
          <img src="/relative-banner.jpg" alt="Mountains" width="800" height="600" />
          <img src="https://example.com/1x1-tracker.png" width="1" height="1" />
        </body>
      </html>
    `;

    jest.spyOn(safeFetchModule, "assertPublicHttpUrl").mockReturnValue(new URL("https://example.com/gallery"));
    jest.spyOn(safeFetchModule, "safeFetch").mockResolvedValue({
      status: 200,
      url: "https://example.com/gallery",
      headers: { "content-type": "text/html; charset=utf-8" },
      body: null as any,
      text: async () => mockHtml,
    });

    const result = await service.extract("https://example.com/gallery");

    expect(result.siteTitle).toBe("Scenic Landscapes");
    expect(result.totalFound).toBeGreaterThanOrEqual(4);

    const urls = result.images.map((img) => img.url);
    expect(urls).toContain("https://example.com/og-image.png");
    expect(urls).toContain("https://example.com/twitter-image.webp");
    expect(urls).toContain("https://example.com/jsonld-image.jpg");
    expect(urls).toContain("https://example.com/relative-banner.jpg");

    // 1x1 tracker must be filtered out
    expect(urls).not.toContain("https://example.com/1x1-tracker.png");
  });

  it("should reject SSRF / private IP targets", async () => {
    await expect(service.extract("http://127.0.0.1/admin.html")).rejects.toThrow();
    await expect(service.extract("http://192.168.1.1/router.html")).rejects.toThrow();
  });
});
