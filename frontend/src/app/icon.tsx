import { ImageResponse } from "next/og";
import { readFile } from "node:fs/promises";
import { join } from "node:path";

// Image metadata
export const size = { width: 32, height: 32 };
export const contentType = "image/png";

// Same brand face + treatment as the header wordmark's mobile "Pzo" collapse
// (see Header.tsx: font-brand, --primary orange fill, .sticker-outline white
// die-cut stroke) — read once at module scope since it doesn't depend on
// request data.
const beautySmile = await readFile(
  join(process.cwd(), "src/assets/fonts/beauty-smile.otf")
);

export default async function Icon() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <div
          style={{
            display: "flex",
            fontFamily: "Beauty Smile",
            fontSize: 24,
            color: "#ff5a3c",
            WebkitTextStroke: "4px #fff",
          }}
        >
          P
        </div>
      </div>
    ),
    {
      ...size,
      fonts: [
        {
          name: "Beauty Smile",
          data: beautySmile,
          style: "normal",
        },
      ],
    }
  );
}
