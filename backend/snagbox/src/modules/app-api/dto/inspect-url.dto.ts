import { ApiProperty } from "@nestjs/swagger";
import { IsUrl, MaxLength } from "class-validator";

export class InspectUrlDto {
  @ApiProperty({ example: "https://www.youtube.com/watch?v=dQw4w9WgXcQ" })
  @IsUrl({ require_protocol: true, protocols: ["http", "https"] })
  @MaxLength(2048)
  url!: string;
}
