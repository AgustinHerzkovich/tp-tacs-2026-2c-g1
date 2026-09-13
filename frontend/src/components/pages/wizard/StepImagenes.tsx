import { ImagePlus, X } from "lucide-react";
import type { WizardFormState } from "@/types/domain";
import type { FieldSetter, FormPatchSetter } from "@/hooks/useWizardForm";

interface StepProps {
  form: WizardFormState;
  set: FieldSetter;
  patch?: FormPatchSetter;
}

function ImagePreview({ source, fileName, index, onRemove }: { source: string; fileName: string; index: number; onRemove: () => void }) {
  return (
    <div className="relative aspect-square rounded-2xl overflow-hidden bg-muted">
      {/* Local object URLs are temporary previews selected by the user. */}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={source} alt={`Vista previa ${index + 1}`} className="size-full object-cover" />
      <button type="button" onClick={onRemove} className="absolute top-2 right-2 size-8 rounded-full bg-black/65 text-white flex items-center justify-center" aria-label={`Quitar ${fileName}`}>
        <X className="size-4" />
      </button>
      {index === 0 && <span className="absolute left-2 bottom-2 rounded-full bg-white/90 px-2.5 py-1 text-[10px] font-extrabold">Portada</span>}
    </div>
  );
}

export function StepImagenes({ form, set }: StepProps) {
  return (
    <div className="pt-2">
      <div className="mb-6">
        <h3 className="font-display font-semibold text-xl">Sumale imágenes a tu plan</h3>
        <p className="mt-1 text-[13px] font-bold" style={{ color: "var(--muted-foreground)" }}>
          Son opcionales. La primera será la portada de la actividad y después podrán verse todas en la galería.
        </p>
      </div>

      <label className="tap min-h-40 flex flex-col items-center justify-center gap-3 rounded-3xl border-2 border-dashed px-6 py-8 cursor-pointer text-center" style={{ borderColor: "var(--border)", background: "var(--muted)" }}>
        <span className="size-14 rounded-2xl bg-white flex items-center justify-center shadow-sm">
          <ImagePlus className="size-6" style={{ color: "var(--primary)" }} />
        </span>
        <span className="font-display font-semibold">Elegir imágenes</span>
        <span className="text-[11px] font-bold" style={{ color: "var(--muted-foreground)" }}>JPEG, PNG o WebP · hasta 5 · máximo 5 MiB cada una</span>
        <input
          type="file"
          accept="image/jpeg,image/png,image/webp"
          multiple
          className="sr-only"
          onChange={(event) => {
            const selected = Array.from(event.target.files ?? [])
              .filter((file) => file.size <= 5 * 1024 * 1024)
              .map((file) => ({ file, previewUrl: URL.createObjectURL(file) }));
            const nextImages = [...form.images, ...selected].slice(0, 5);
            selected.slice(Math.max(0, 5 - form.images.length)).forEach((image) => URL.revokeObjectURL(image.previewUrl));
            set("images")(nextImages);
            event.target.value = "";
          }}
        />
      </label>

      {form.images.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mt-5">
          {form.images.map((image, index) => (
            <ImagePreview key={`${image.file.name}-${image.file.lastModified}`} source={image.previewUrl} fileName={image.file.name} index={index} onRemove={() => {
              URL.revokeObjectURL(image.previewUrl);
              set("images")(form.images.filter((_, imageIndex) => imageIndex !== index));
            }} />
          ))}
        </div>
      )}
    </div>
  );
}
