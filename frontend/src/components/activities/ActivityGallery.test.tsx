import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeAll, describe, expect, it, vi } from "vitest";
import { ActivityGallery } from "@/components/activities/ActivityGallery";

beforeAll(() => {
  Element.prototype.scrollIntoView = vi.fn();
});

const IMAGES = [
  "https://cdn.example.com/1.jpg",
  "https://cdn.example.com/2.jpg",
  "https://cdn.example.com/3.jpg",
];

describe("ActivityGallery", () => {
  it("renders the scene placeholder (gradient + pattern overlay, no image) when there are no images", () => {
    render(<ActivityGallery images={[]} scene="skyMint" pattern="dots" title="Sin fotos" />);
    const placeholder = document.querySelector('[data-scene="skyMint"]');
    expect(placeholder).toBeInTheDocument();
    expect(placeholder).toHaveAttribute("data-pattern", "dots");
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });

  it("renders every image with an accessible label", () => {
    render(<ActivityGallery images={IMAGES} scene="sunRose" pattern="plain" title="Partido de vóley" />);
    const pictures = screen.getAllByRole("img");
    expect(pictures).toHaveLength(3);
    expect(pictures[0]).toHaveAttribute("src", IMAGES[0]);
    expect(pictures[0]).toHaveAttribute("alt", "Partido de vóley, imagen 1");
  });

  it("reports the active slide and allows navigating with the dots", async () => {
    const user = userEvent.setup();
    render(<ActivityGallery images={IMAGES} scene="sunRose" pattern="plain" title="Partido" />);

    expect(screen.getByLabelText(/Imagen 1 de 3/)).toBeInTheDocument();
    const prevButton = screen.getByRole("button", { name: "Imagen anterior" });
    expect(prevButton).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Ver imagen 3" }));
    expect(screen.getByLabelText(/Imagen 3 de 3/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Imagen siguiente" })).toBeDisabled();
  });

  it("hides navigation controls when only one image exists", () => {
    render(<ActivityGallery images={[IMAGES[0]!]} scene="lavSky" pattern="plain" title="Cine" />);
    expect(screen.queryByRole("button", { name: "Imagen anterior" })).not.toBeInTheDocument();
  });
});