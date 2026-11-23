import type { ProviderMedia } from "./providers/types";
import { ExtractionService } from "./extraction.service";
import { CancelledError } from "./ytdlp.service";

const sourceUrl = "https://www.youtube.com/watch?v=uwVQ__LfXIk";
const input = {
  url: sourceUrl,
  outputPath: "/tmp/job.%(ext)s",
  request: { url: sourceUrl, kind: "video" as const },
  kind: "video" as const,
  format: "mp4",
  onProgress: jest.fn(),
};

function fixture() {
  const primaryError = new Error("Primary extractor failed");
  const ytdlp = {
    extractInfo: jest.fn().mockRejectedValue(primaryError),
    runDownload: jest.fn().mockRejectedValue(primaryError),
  };
  const media = (url: string): ProviderMedia => ({
    id: "sample", title: "Sample", description: "", thumbnail: null,
    durationSeconds: 10, uploader: null, uploaderUrl: null,
    viewCount: null, likeCount: null, commentCount: null, uploadedAt: null,
    isLive: false, videoUrls: [url], headers: {},
  });
  const first = {
    name: "first", supports: jest.fn().mockReturnValue(true),
    extract: jest.fn().mockResolvedValue(media("https://cdn.example.com/first.mp4")),
  };
  const second = {
    name: "second", supports: jest.fn().mockReturnValue(true),
    extract: jest.fn().mockResolvedValue(media("https://cdn.example.com/second.mp4")),
  };
  const apify = {
    supports: jest.fn().mockReturnValue(false), canDownload: jest.fn().mockReturnValue(false),
    getMediaInfo: jest.fn(), runDownload: jest.fn(),
  };
  const tikLiveApi = {
    name: "tikliveapi", supports: jest.fn().mockReturnValue(false),
    extract: jest.fn().mockResolvedValue(media("https://v16.tokcdn.com/sample.mp4")),
  };
  const service = new ExtractionService(
    ytdlp as never, {} as never, { mediaCacheTtlMs: 0 } as never,
    {} as never, first as never, second as never, tikLiveApi as never,
    apify as never,
  );
  const transfer = jest.spyOn(service as any, "downloadDirect").mockResolvedValue(undefined);
  return { service, ytdlp, first, second, transfer, primaryError, apify, tikLiveApi };
}

describe("ExtractionService provider fallback", () => {
  it("uses TikLiveAPI when the earlier providers cannot transfer a TikTok video", async () => {
    const f = fixture();
    const url = "https://www.tiktok.com/@tiktok/video/7460937381265411370";
    f.tikLiveApi.supports.mockReturnValue(true);
    f.transfer.mockRejectedValueOnce(new Error("CDN unavailable")).mockRejectedValueOnce(new Error("CDN unavailable"));
    await f.service.runDownload({ ...input, url, request: { ...input.request, url } });
    expect(f.tikLiveApi.extract).toHaveBeenCalledWith(url);
    expect(f.transfer).toHaveBeenLastCalledWith(expect.anything(), "https://v16.tokcdn.com/sample.mp4", {});
  });

  it("returns cloud metadata immediately when it succeeds", async () => {
    const f = fixture();
    f.apify.supports.mockReturnValue(true);
    f.apify.getMediaInfo.mockResolvedValue({ title: "Cloud result" });
    expect(await f.service.getMediaInfo(sourceUrl)).toEqual({ title: "Cloud result" });
    expect(f.ytdlp.extractInfo).not.toHaveBeenCalled();
    expect(f.first.extract).not.toHaveBeenCalled();
  });

  it("stops after a successful cloud download", async () => {
    const f = fixture();
    f.apify.canDownload.mockReturnValue(true);
    f.apify.runDownload.mockResolvedValue(undefined);
    await f.service.runDownload(input);
    expect(f.ytdlp.runDownload).not.toHaveBeenCalled();
    expect(f.first.extract).not.toHaveBeenCalled();
  });

  it("continues after a cloud service failure", async () => {
    const f = fixture();
    f.apify.canDownload.mockReturnValue(true);
    f.apify.runDownload.mockRejectedValue(new Error("Free allowance exhausted"));
    f.ytdlp.runDownload.mockResolvedValue(undefined);
    await f.service.runDownload(input);
    expect(f.ytdlp.runDownload).toHaveBeenCalledTimes(1);
    expect(f.first.extract).not.toHaveBeenCalled();
  });

  it("propagates cloud cancellation without starting the next extractor", async () => {
    const f = fixture();
    f.apify.canDownload.mockReturnValue(true);
    f.apify.runDownload.mockRejectedValue(new CancelledError());
    await expect(f.service.runDownload(input)).rejects.toBeInstanceOf(CancelledError);
    expect(f.ytdlp.runDownload).not.toHaveBeenCalled();
  });
  it("stops after the primary extractor succeeds", async () => {
    const f = fixture();
    f.ytdlp.runDownload.mockResolvedValue(undefined);
    await f.service.runDownload(input);
    expect(f.first.extract).not.toHaveBeenCalled();
    expect(f.second.extract).not.toHaveBeenCalled();
  });

  it("stops after the first provider transfers the file", async () => {
    const f = fixture();
    await f.service.runDownload(input);
    expect(f.transfer).toHaveBeenCalledTimes(1);
    expect(f.second.extract).not.toHaveBeenCalled();
  });

  it("tries the next provider when extraction fails", async () => {
    const f = fixture();
    f.first.extract.mockRejectedValue(new Error("Provider unavailable"));
    await f.service.runDownload(input);
    expect(f.second.extract).toHaveBeenCalledTimes(1);
  });

  it("tries the next provider when its CDN link fails", async () => {
    const f = fixture();
    f.transfer.mockRejectedValueOnce(new Error("CDN returned 403"));
    await f.service.runDownload(input);
    expect(f.second.extract).toHaveBeenCalledTimes(1);
    expect(f.transfer).toHaveBeenCalledTimes(2);
  });

  it("tries another rendition before moving to the next provider", async () => {
    const f = fixture();
    const media = await f.first.extract();
    media.videoUrls.push("https://cdn.example.com/alternate.mp4");
    f.transfer.mockRejectedValueOnce(new Error("Expired URL"));
    await f.service.runDownload(input);
    expect(f.transfer).toHaveBeenCalledTimes(2);
    expect(f.second.extract).not.toHaveBeenCalled();
  });

  it("propagates cancellation without starting another service", async () => {
    const f = fixture();
    f.transfer.mockRejectedValueOnce(new CancelledError());
    await expect(f.service.runDownload(input)).rejects.toBeInstanceOf(CancelledError);
    expect(f.second.extract).not.toHaveBeenCalled();
  });

  it("rejects metadata without a downloadable rendition and continues", async () => {
    const f = fixture();
    const media = await f.first.extract();
    media.videoUrls = [];
    const info = await f.service.getMediaInfo(sourceUrl);
    expect(info.title).toBe("Sample");
    expect(f.second.extract).toHaveBeenCalledTimes(1);
  });

  it("reports failure when every transfer fails", async () => {
    const f = fixture();
    f.transfer.mockRejectedValue(new Error("CDN failed"));
    await expect(f.service.runDownload(input)).rejects.toBe(f.primaryError);
    expect(f.second.extract).toHaveBeenCalledTimes(1);
  });
});
