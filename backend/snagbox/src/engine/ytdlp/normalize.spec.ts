import { normalizeInfo } from "./normalize";

const base = {
  id: "instagram-reel",
  title: "Example reel",
  uploader: "creator",
};

describe("normalizeInfo quality availability", () => {
  it("does not invent fixed resolutions when yt-dlp reports no heights", () => {
    const info = normalizeInfo({ ...base, formats: [] }, "https://www.instagram.com/reel/example/", "instagram");

    expect(info.qualities.video).toEqual(["highest"]);
  });

  it("keeps only the resolutions actually reported by the extractor", () => {
    const info = normalizeInfo(
      {
        ...base,
        formats: [
          { resolution: "480p", vcodec: "h264", acodec: "none" },
          { format_note: "audio only", vcodec: "none", acodec: "mp4a" },
        ],
      },
      "https://www.instagram.com/reel/example/",
      "instagram",
    );

    expect(info.qualities.video).toEqual(["highest", "480p", "lowest"]);
    expect(info.qualities.video).not.toContain("1080p");
  });
});
