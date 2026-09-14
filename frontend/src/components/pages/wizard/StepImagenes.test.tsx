import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useWizardForm } from "@/hooks/useWizardForm";
import { StepImagenes } from "@/components/pages/wizard/StepImagenes";

let objectUrlIndex = 0;
let createObjectURL: ReturnType<typeof vi.spyOn>;
let revokeObjectURL: ReturnType<typeof vi.spyOn>;

beforeEach(() => {
  objectUrlIndex = 0;
  createObjectURL = vi.spyOn(URL, "createObjectURL").mockImplementation(() => `blob:preview-${++objectUrlIndex}`);
  revokeObjectURL = vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => undefined);
});

afterEach(() => {
  createObjectURL.mockClear();
  revokeObjectURL.mockClear();
});

function imageFile(name: string, size = 100, type = "image/jpeg"): File {
  return new File([new Uint8Array(size).fill(1)], name, { type });
}

function Harness() {
  const wizard = useWizardForm();
  return <StepImagenes form={wizard.form} set={wizard.set} />;
}

async function uploadTo(user: ReturnType<typeof userEvent.setup>, files: File[]) {
  const input = document.querySelector("input[type='file']");
  expect(input).not.toBeNull();
  await user.upload(input as HTMLInputElement, files);
}

describe("StepImagenes", () => {
  it("adds selected images as previews and marks the first one as the cover", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await uploadTo(user, [imageFile("portada.jpg"), imageFile("foto2.png")]);

    const firstPreview = screen.getByAltText("Vista previa 1");
    expect(firstPreview).toHaveAttribute("src", "blob:preview-1");
    expect(screen.getByAltText("Vista previa 2")).toHaveAttribute("src", "blob:preview-2");
    expect(screen.getByText("Portada")).toBeInTheDocument();
    expect(revokeObjectURL).not.toHaveBeenCalled();
  });

  it("does not add files whose type or size is not accepted", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await uploadTo(user, [
      imageFile("big.pdf", 4096, "application/pdf"),
      imageFile("oversize.jpg", 5 * 1024 * 1024 + 1),
    ]);

    expect(screen.queryByAltText("Vista previa 1")).not.toBeInTheDocument();
    expect(createObjectURL).not.toHaveBeenCalled();
  });

  it("caps the total collection at five images and revokes the excess object URLs", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await uploadTo(user, [imageFile("a.jpg"), imageFile("b.jpg"), imageFile("c.jpg")]);
    await uploadTo(user, [imageFile("d.jpg"), imageFile("e.jpg"), imageFile("f.jpg")]);

    expect(screen.getAllByAltText(/Vista previa/)).toHaveLength(5);
    expect(revokeObjectURL).toHaveBeenCalledWith("blob:preview-6");
  });

  it("removes an image and revokes its object URL", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await uploadTo(user, [imageFile("foto.jpg")]);
    await user.click(screen.getByRole("button", { name: "Quitar foto.jpg" }));

    expect(screen.queryByAltText("Vista previa 1")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Quitar foto.jpg" })).not.toBeInTheDocument();
    expect(revokeObjectURL).toHaveBeenCalledWith("blob:preview-1");
  });
});