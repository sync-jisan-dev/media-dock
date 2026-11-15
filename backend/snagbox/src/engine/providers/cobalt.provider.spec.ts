import { Readable } from "stream";
import * as safeFetchModule from "../../common/http/safe-fetch";
import { CobaltProvider } from "./cobalt.provider";

function config(url = "https://cobalt.example.com", key = "secret") {
  return { cobaltApiUrl: url, cobaltApiKey: key } as never;
}

describe("CobaltProvider", () => {
  afterEach(() => jest.restoreAllMocks());

  it("stays disabled until an operator-owned instance is configured", () => {
    expect(new CobaltProvider(config("", "")).supports()).toBe(false);
    expect(new CobaltProvider(config("https://api.cobalt.tools", "")).supports()).toBe(false);
    expect(new CobaltProvider(config()).supports()).toBe(true);
  });

  it("maps a cobalt redirect into provider media", async () => {
    const response = JSON.stringify({
      status: "redirect",
      url: "https://cdn.example.com/video.mp4",
      filename: "A clean title.mp4",
    });
    jest.spyOn(safeFetchModule, "safeFetch").mockResolvedValue({
      status: 200,
      url: "https://cobalt.example.com/",
      headers: { "content-type": "application/json" },
      body: Readable.from([response]),
      text: async () => response,
    });

    const result = await new CobaltProvider(config()).extract("https://example.com/post/1");
    expect(result.title).toBe("A clean title");
    expect(result.videoUrls).toEqual(["https://cdn.example.com/video.mp4"]);
    expect(safeFetchModule.safeFetch).toHaveBeenCalledWith(
      "https://cobalt.example.com/",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({ Authorization: "Api-Key secret" }),
      }),
    );
  });

  it("maps cobalt picker responses", async () => {
    const response = JSON.stringify({
      status: "picker",
      picker: [
        { type: "photo", url: "https://cdn.example.com/one.jpg", thumb: "https://cdn.example.com/thumb.jpg" },
        { type: "video", url: "https://cdn.example.com/two.mp4" },
      ],
    });
    jest.spyOn(safeFetchModule, "safeFetch").mockResolvedValue({
      status: 200,
      url: "https://cobalt.example.com/",
      headers: {},
      body: Readable.from([response]),
      text: async () => response,
    });

    const result = await new CobaltProvider(config()).extract("https://example.com/post/2");
    expect(result.videoUrls).toHaveLength(2);
    expect(result.thumbnail).toBe("https://cdn.example.com/thumb.jpg");
  });
});
