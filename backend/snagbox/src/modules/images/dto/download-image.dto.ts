import { ApiProperty, ApiPropertyOptional } from "@nestjs/swagger";
import { IsOptional, IsString, IsUrl, MaxLength } from "class-validator";

export class DownloadImageDto {
  @ApiProperty({ example: "https://images.unsplash.com/photo-123456789.jpg" })
  @IsUrl({ require_protocol: true, protocols: ["http", "https"] })
  @MaxLength(2048)
  url!: string;

  @ApiPropertyOptional({ example: "Sunset over mountains" })
  @IsOptional()
  @IsString()
  @MaxLength(255)
  title?: string;

  @ApiPropertyOptional({ example: "jpg" })
  @IsOptional()
  @IsString()
  @MaxLength(10)
  format?: string;
}
