import { ApiProperty } from "@nestjs/swagger";
import { IsUrl, MaxLength } from "class-validator";

export class ExtractImagesDto {
  @ApiProperty({ example: "https://unsplash.com/photos/nature" })
  @IsUrl({ require_protocol: true, protocols: ["http", "https"] })
  @MaxLength(2048)
  url!: string;
}
