import { ImagePlus, X, AlertCircle, GripVertical } from "lucide-react";
import type { WizardFormState } from "@/types/domain";
import type { FieldSetter, FormPatchSetter } from "@/hooks/useWizardForm";
import { useState, useCallback, useEffect } from "react";

interface ImageError {
  fileName: string;
  message: string;
}

interface StepProps {
  form: WizardFormState;
  set: FieldSetter;
  patch?: FormPatchSetter;
}

function ImagePreview({ 
  source, 
  fileName, 
  index, 
  onRemove, 
  onMoveUp, 
  onMoveDown, 
  isCover 
}: { 
  source: string; 
  fileName: string; 
  index: number; 
  onRemove: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
  isCover: boolean;
}) {
  return (
    <div className="relative aspect-square rounded-2xl overflow-hidden bg-muted group">
      {/* Local object URLs are temporary previews selected by the user. */}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={source} alt={`Vista previa ${index + 1}`} className="size-full object-cover" />
      <button 
        type="button" 
        onClick={onRemove} 
        className="absolute top-2 right-2 size-8 rounded-full bg-black/65 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity" 
        aria-label={`Quitar ${fileName}`}
      >
        <X className="size-4" />
      </button>
      <div className="absolute top-2 left-2 flex flex-col gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
        <button 
          type="button" 
          onClick={onMoveUp} 
          disabled={index === 0}
          className="size-8 rounded-full bg-black/65 text-white flex items-center justify-center disabled:opacity-30" 
          aria-label="Mover hacia arriba"
          title="Mover hacia arriba"
        >
          <GripVertical className="size-4" />
        </button>
        <button 
          type="button" 
          onClick={onMoveDown} 
          disabled={isCover}
          className="size-8 rounded-full bg-black/65 text-white flex items-center justify-center disabled:opacity-30" 
          aria-label="Mover hacia abajo"
          title="Mover hacia abajo"
        >
          <GripVertical className="size-4 rotate-180" />
        </button>
      </div>
      {isCover && <span className="absolute left-2 bottom-2 rounded-full bg-white/90 px-2.5 py-1 text-[10px] font-extrabold">Portada</span>}
    </div>
  );
}

export function StepImagenes({ form, set }: StepProps) {
  const [errors, setErrors] = useState<ImageError[]>([]);

  // Cleanup object URLs on unmount
  useEffect(() => {
    return () => {
      form.images.forEach((image: { file: File; previewUrl: string }) => {
        URL.revokeObjectURL(image.previewUrl);
      });
    };
  }, [form.images]);

  const handleFileChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files) return;

    const newErrors: ImageError[] = [];
    const validFiles: Array<{ file: File; previewUrl: string }> = [];
    const MAX_SIZE = 5 * 1024 * 1024; // 5 MiB
    const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp"];
    const MAX_IMAGES = 5;

    Array.from(files).forEach((file) => {
      // Check file type
      if (!ALLOWED_TYPES.includes(file.type)) {
        newErrors.push({ fileName: file.name, message: "Tipo no válido (solo JPEG, PNG o WebP)" });
        return;
      }

      // Check file size
      if (file.size > MAX_SIZE) {
        newErrors.push({ fileName: file.name, message: "El archivo supera los 5 MiB" });
        return;
      }

      // Check for duplicates
      const isDuplicate = form.images.some(
        (existing: { file: File; previewUrl: string }) => existing.file.name === file.name && existing.file.size === file.size
      );
      if (isDuplicate) {
        newErrors.push({ fileName: file.name, message: "Ya agregaste esta imagen" });
        return;
      }

      validFiles.push({ file, previewUrl: URL.createObjectURL(file) });
    });

    setErrors(newErrors);

    if (validFiles.length > 0) {
      const currentCount = form.images.length;
      const availableSlots = MAX_IMAGES - currentCount;
      const toAdd = validFiles.slice(0, availableSlots);
      const toDiscard = validFiles.slice(availableSlots);

      // Revoke URLs for discarded files
      toDiscard.forEach((image) => URL.revokeObjectURL(image.previewUrl));

      if (toDiscard.length > 0) {
        newErrors.push({ 
          fileName: toDiscard.map((f) => f.file.name).join(", "), 
          message: `Solo se permiten ${MAX_IMAGES} imágenes en total` 
        });
        setErrors(newErrors);
      }

      const nextImages = [...form.images, ...toAdd];
      set("images")(nextImages);
    }

    event.target.value = "";
  }, [form.images, set]);

  const handleRemove = useCallback((index: number) => {
    const imageToRemove = form.images[index];
    if (imageToRemove) {
      URL.revokeObjectURL(imageToRemove.previewUrl);
    }
    set("images")(form.images.filter((_: { file: File; previewUrl: string }, imageIndex: number) => imageIndex !== index));
  }, [form.images, set]);

  const handleMoveUp = useCallback((index: number) => {
    if (index === 0) return;
    const newImages = [...form.images];
    if (newImages[index - 1] && newImages[index]) {
      [newImages[index - 1]!, newImages[index]!] = [newImages[index]!, newImages[index - 1]!];
      set("images")(newImages);
    }
  }, [form.images, set]);

  const handleMoveDown = useCallback((index: number) => {
    if (index === form.images.length - 1) return;
    const newImages = [...form.images];
    if (newImages[index] && newImages[index + 1]) {
      [newImages[index]!, newImages[index + 1]!] = [newImages[index + 1]!, newImages[index]!];
      set("images")(newImages);
    }
  }, [form.images, set]);

  return (
    <div className="pt-2">
      <div className="mb-6">
        <h3 className="font-display font-semibold text-xl">Sumale imágenes a tu plan</h3>
        <p className="mt-1 text-[13px] font-bold" style={{ color: "var(--muted-foreground)" }}>
          Son opcionales. La primera será la portada de la actividad y después podrán verse todas en la galería.
        </p>
      </div>

      {errors.length > 0 && (
        <div className="mb-4 space-y-2" role="alert" aria-live="polite">
          {errors.map((error, idx) => (
            <div key={idx} className="flex items-start gap-2 rounded-xl bg-destructive/10 p-3 border border-destructive/20">
              <AlertCircle className="size-4 mt-0.5 shrink-0" style={{ color: "var(--destructive)" }} aria-hidden="true" />
              <div className="flex-1">
                <p className="text-[11px] font-extrabold" style={{ color: "var(--destructive)" }}>{error.fileName}</p>
                <p className="text-[11px] font-bold" style={{ color: "var(--destructive)" }}>{error.message}</p>
              </div>
            </div>
          ))}
        </div>
      )}

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
          onChange={handleFileChange}
        />
      </label>

      {form.images.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mt-5">
          {form.images.map((image: { file: File; previewUrl: string }, index: number) => (
            <ImagePreview 
              key={`${image.file.name}-${image.file.lastModified}`} 
              source={image.previewUrl} 
              fileName={image.file.name} 
              index={index} 
              isCover={index === 0}
              onRemove={() => handleRemove(index)}
              onMoveUp={() => handleMoveUp(index)}
              onMoveDown={() => handleMoveDown(index)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
