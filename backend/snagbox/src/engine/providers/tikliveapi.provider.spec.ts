import { safeFetch } from "../../common/http/safe-fetch";
import type { AppConfigService } from "../../config/app-config.service";
import { TikLiveApiProvider } from "./tikliveapi.provider";

jest.mock("../../common/http/safe-fetch");

const fetchVideo = jest.mocked(safeFetch);
const video = "https://www.tiktok.com/@tiktok/video/7460937381265411370";
const hd = "https://v16.tokcdn.com/clip.mp4";
const standard = "https://v16m.tiktokcdn-us.com/clip.mp4?signature=kept";

function provider(key = "test-placeholder") {
  return new TikLiveApiProvider({ tikLiveApiKey: key } as AppConfigService);
}

function respond(json: object) {
  fetchVideo.mockResolvedValueOnce({
    status: 200,
    text: async () => JSON.stringify(json),
  } as Awaited<ReturnType<typeof safeFetch>>);
}

beforeEach(() => fetchVideo.mockReset());

it("uses the download endpoint once for the same video and prefers the HD stream", async () => {
  respond({ video_hd: hd, video: standard });
  const service = provider();
  const [a, b] = await Promise.all([service.extract(video + "/?tracking=example"), service.extract(video)]);
  expect(a).toEqual(b);
  expect(a.videoUrls).toEqual([hd, standard]);
  expect(a.headers).not.toHaveProperty("X-Api-Key");
  expect(fetchVideo).toHaveBeenCalledTimes(1);
  const [url, options] = fetchVideo.mock.calls[0];
  expect(new URL(url).origin + new URL(url).pathname).toBe("https://api.tikliveapi.com/download-video/");
  expect(new URL(url).searchParams.get("url")).toBe(video);
  expect(options).toMatchObject({ headers: { "X-Api-Key": "test-placeholder" }, maxRedirects: 0 });
});

it("accepts TikTok share links and rejects other origins and disabled keys", () => {
  expect(provider().supports("https://vm.tiktok.com/Example/", "tiktok")).toBe(true);
  expect(provider().supports("https://www.tiktok.com.invalid/@tiktok/video/1", "tiktok")).toBe(false);
  expect(provider().supports("https://www.tiktok.com/@tiktok/photo/1", "tiktok")).toBe(false);
  expect(provider("").supports(video, "tiktok")).toBe(false);
});

it("handles a provider error in a 200 response and drops it from the cache", async () => {
  respond({ message: "Key or quota unavailable" });
  respond({ video: standard });
  const service = provider();
  await expect(service.extract(video)).rejects.toThrow("declined");
  await expect(service.extract(video)).resolves.toMatchObject({ videoUrls: [standard] });
});

it("rejects non-TikTok media hosts before the downloader can fetch them", async () => {
  respond({ video_hd: "https://v16.tokcdn.com.example.org/video.mp4", video: "http://127.0.0.1/media" });
  await expect(provider().extract(video)).rejects.toThrow("no supported TikTok video links");
});
