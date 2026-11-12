import { ImageDownloadService } from "./image-download.service";
import { AppConfigService } from "../../config/app-config.service";
import { PrismaService } from "../../prisma/prisma.service";
import * as safeFetchModule from "../../common/http/safe-fetch";
import { Readable } from "stream";
import * as fs from "fs/promises";

jest.mock("fs/promises");

describe("ImageDownloadService", () => {
  let service: ImageDownloadService;
  let mockConfig: Partial<AppConfigService>;
  let mockPrisma: any;

  beforeEach(() => {
    mockConfig = {
      downloadsDir: "test-downloads",
      fileLinkSecret: "test-secret-1234567890123456789012",
      signedUrlTtlSeconds: 3600,
      fileTtlMinutes: 1440,
    };
    mockPrisma = {
      download: {
        create: jest.fn().mockImplementation(({ data }) => ({
          ...data,
          id: "mock-download-uuid-1234",
          createdAt: new Date(),
          updatedAt: new Date(),
        })),
      },
    };
    service = new ImageDownloadService(
      mockConfig as AppConfigService,
      mockPrisma as PrismaService,
    );
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should download JPEG image, verify magic bytes, save to disk, and return signed download job", async () => {
    // Magic bytes for JPEG: FF D8 FF
    const jpegBuffer = Buffer.from([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46, 0x00, 0x01]);
    const stream = Readable.from([jpegBuffer]);

    jest.spyOn(safeFetchModule, "assertPublicHttpUrl").mockReturnValue(new URL("https://example.com/photo.jpg"));
    jest.spyOn(safeFetchModule, "safeFetch").mockResolvedValue({
      status: 200,
      url: "https://example.com/photo.jpg",
      headers: { "content-type": "image/jpeg" },
      body: stream,
      text: async () => "",
    });

    (fs.mkdir as jest.Mock).mockResolvedValue(undefined);
    (fs.writeFile as jest.Mock).mockResolvedValue(undefined);

    const job = await service.download({
      url: "https://example.com/photo.jpg",
      title: "Mountain Sunset",
    });

    expect(job).toBeDefined();
    expect(job.status).toBe("COMPLETED");
    expect(job.kind).toBe("image");
    expect(job.fileUrl).toContain("/v2/files/");
    expect(mockPrisma.download.create).toHaveBeenCalled();
  });
});
