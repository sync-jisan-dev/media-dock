import { Readable } from "stream";
import { safeFetch } from "../../common/http/safe-fetch";
import { ApifyDownloaderService } from "./apify-downloader.service";
import { CancelledError } from "../ytdlp.service";

jest.mock("../../common/http/safe-fetch", () => ({ safeFetch: jest.fn() }));
const fetchMock = safeFetch as jest.MockedFunction<typeof safeFetch>;
const url = "https://www.youtube.com/watch?v=uwVQ__LfXIk";
const input = {
  url, outputPath: "/tmp/test.%(ext)s", onProgress: jest.fn(),
  request: { url, kind: "video" as const, format: "mp4" as const, quality: "360p" as const },
};
const result = {
  status: "success", ok: true, meta: { title: "Sample", duration_sec: 873 },
  artifacts: [{ name: "source", bytes: 1024,
    url: "https://api.apify.com/v2/key-value-stores/store/records/source.mp4?token=old-token" }],
};
function response(data: unknown, status = 200) {
  const body = JSON.stringify(data);
  return { status, url: "https://api.apify.com/", headers: {},
    body: Readable.from([body]), text: async () => body };
}
function fixture() {
  const config = { apifyEnabled: true, apifyApiToken: "server-secret" };
  const direct = { download: jest.fn().mockResolvedValue(undefined) };
  const service = new ApifyDownloaderService(config as never, direct as never);
  fetchMock.mockResolvedValueOnce(response({ data: { id: "run", status: "SUCCEEDED", defaultDatasetId: "dataset" } }));
  fetchMock.mockResolvedValueOnce(response([result]));
  return { service, config, direct };
}

describe("ApifyDownloaderService", () => {
  beforeEach(() => fetchMock.mockReset());

  it("requires an enabled server credential and a YouTube URL", () => {
    const f = fixture();
    expect(f.service.supports(url)).toBe(true);
    expect(f.service.supports("https://example.com/video")).toBe(false);
    f.config.apifyEnabled = false;
    expect(f.service.supports(url)).toBe(false);
  });

  it("caps runs and maps the verified resolution without exposing credentials", async () => {
    const f = fixture();
    const info = await f.service.getMediaInfo(url);
    expect(info.title).toBe("Sample");
    expect(info.durationSeconds).toBe(873);
    expect(info.qualities.video).toEqual(["360p"]);
    expect(JSON.stringify(info)).not.toContain("server-secret");
    expect(fetchMock.mock.calls[0][0]).toContain("maxTotalChargeUsd=0.50");
    expect(fetchMock.mock.calls[0][0]).toContain("timeout=180");
  });

  it("reuses successful metadata without another Actor run", async () => {
    const f = fixture();
    await Promise.all([f.service.getMediaInfo(url), f.service.getMediaInfo(url)]);
    await f.service.getMediaInfo(url);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("downloads hosted media and strips query tokens", async () => {
    const f = fixture();
    await f.service.runDownload(input);
    expect(f.direct.download).toHaveBeenCalledWith(expect.objectContaining({
      url: "https://api.apify.com/v2/key-value-stores/store/records/source.mp4",
      headers: { Authorization: "Bearer server-secret" },
    }));
  });

  it("rejects artifact URLs on other hosts before sending credentials", async () => {
    const f = fixture();
    fetchMock.mockReset();
    fetchMock.mockResolvedValueOnce(response({ data: { id: "run", status: "SUCCEEDED", defaultDatasetId: "dataset" } }));
    fetchMock.mockResolvedValueOnce(response([{ ...result, artifacts: [{ bytes: 1024, url: "https://evil.example/file" }] }]));
    await expect(f.service.runDownload(input)).rejects.toThrow("unexpected artifact");
    expect(f.direct.download).not.toHaveBeenCalled();
  });

  it("leaves advanced options and unsupported qualities to the next extractor", () => {
    const f = fixture();
    expect(f.service.canDownload(input)).toBe(true);
    expect(f.service.canDownload({ ...input, request: { ...input.request, quality: "720p" } })).toBe(false);
    expect(f.service.canDownload({ ...input, request: { ...input.request, clipStart: 1 } })).toBe(false);
  });

  it("does not start another paid run after an HTTP failure", async () => {
    const f = fixture();
    fetchMock.mockReset().mockResolvedValue(response({}, 402));
    await expect(f.service.getMediaInfo(url)).rejects.toThrow("HTTP 402");
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("does not start a run for a cancelled download", async () => {
    const f = fixture();
    await expect(f.service.runDownload({ ...input, isCancelled: () => true })).rejects.toBeInstanceOf(CancelledError);
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
