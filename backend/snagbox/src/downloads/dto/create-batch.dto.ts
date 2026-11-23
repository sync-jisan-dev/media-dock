import { ApiProperty, ApiPropertyOptional } from "@nestjs/swagger";
import {
  ALL_MEDIA_FORMATS,
  AUDIO_FORMATS,
  AUDIO_QUALITIES,
  VIDEO_FORMATS,
  VIDEO_QUALITIES,
} from "@snagbox/shared";
import {
  ArrayMaxSize,
  ArrayNotEmpty,
  IsArray,
  IsBoolean,
  IsIn,
  IsOptional,
  IsString,
  IsUrl,
  MaxLength,
} from "class-validator";

const ALL_QUALITIES = [...VIDEO_QUALITIES, ...AUDIO_QUALITIES];
const ALL_FORMATS = [...ALL_MEDIA_FORMATS];

/** Ceiling per request. The queue's own backlog cap still applies on top. */
export const MAX_BATCH_URLS = 50;

export class CreateBatchDto {
  @ApiProperty({
    type: [String],
    maxItems: MAX_BATCH_URLS,
    example: ["https://www.youtube.com/watch?v=dQw4w9WgXcQ"],
  })
  @IsArray()
  @ArrayNotEmpty()
  @ArrayMaxSize(MAX_BATCH_URLS)
  @IsUrl({ require_protocol: true, protocols: ["http", "https"] }, { each: true })
  @MaxLength(2048, { each: true })
  urls!: string[];

  @ApiProperty({ enum: ["video", "audio", "image", "document"] })
  @IsIn(["video", "audio", "image", "document"])
  kind!: "video" | "audio" | "image" | "document";

  @ApiPropertyOptional({ enum: ALL_QUALITIES, default: "highest / best" })
  @IsOptional()
  @IsString()
  quality?: string;

  @ApiPropertyOptional({ enum: ALL_FORMATS, default: "mp4 / mp3 / jpg / pdf" })
  @IsOptional()
  @IsString()
  format?: string;

  @ApiPropertyOptional({ description: "Embed thumbnail into audio files" })
  @IsOptional()
  @IsBoolean()
  embedThumbnail?: boolean;
}
