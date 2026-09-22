import { ImagePlus, X, AlertCircle, GripVertical } from "lucide-react";
import { useState, useCallback } from "react";
import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  SortableContext,
  arrayMove,
  rectSortingStrategy,
  sortableKeyboardCoordinates,
  useSortable,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { Chip } from "@/components/common/Chip";
import type { WizardFormState } from "@/types/domain";
import type { FieldSetter, FormPatchSetter } from "@/hooks/useWizardForm";

interface ImageError {
  fileName: string;
  message: string;
}

interface StepProps {
  form: WizardFormState;
  set: FieldSetter;
  patch?: FormPatchSetter;
}

function imageId(image: { file: File }): string {
  return `${image.file.name}-${image.file.lastModified}`;
}

function ImagePreview({
  id,
  source,
  fileName,
  index,
  onRemove,
  isCover,
}: {
  id: string;
  source: string;
  fileName: string;
  index: number;
  onRemove: () => void;
  isCover: boolean;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id });

  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition, opacity: isDragging ? 0.5 : 1 }}
      className="relative aspect-square rounded-2xl overflow-hidden bg-muted group"
    >
      {/* Local object URLs are temporary previews selected by the user. */}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={source} alt={`Vista previa ${index + 1}`} className="size-full object-cover" />
      <button
        type="button"
        onClick={onRemove}
        className="absolute top-2 right-2 size-8 rounded-full bg-[var(--foreground)] ring-2 ring-white text-white flex items-center justify-center lg:opacity-0 lg:group-hover:opacity-100 lg:group-focus-within:opacity-100 transition-opacity"
        aria-label={`Quitar ${fileName}`}
      >
        <X className="size-4" />
      </button>
      <button
        type="button"
        {...attributes}
        {...listeners}
        className="absolute top-2 left-2 size-8 rounded-full bg-[var(--foreground)] ring-2 ring-white text-white flex items-center justify-center cursor-grab touch-none active:cursor-grabbing lg:opacity-0 lg:group-hover:opacity-100 lg:group-focus-within:opacity-100 transition-opacity"
        aria-label={`Arrastrar para reordenar ${fileName}`}
      >
        <GripVertical className="size-4" />
      </button>
      {isCover && (
        <Chip tone="sun" sticker className="absolute left-2 bottom-2">
          Portada
        </Chip>
      )}
    </div>
  );
}

export function StepImagenes({ form, set }: StepProps) {
  const [errors, setErrors] = useState<ImageError[]>([]);
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
  );

  const handleFileChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files) return;

    const newErrors: ImageError[] = [];
    const validFiles: Array<{ file: File; previewUrl: string }> = [];
    const MAX_SIZE = 5 * 1024 * 1024; // 5 MiB
    const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp"];
    const MAX_IMAGES = 5;

    Array.from(files).forEach((file) => {
      if (file.size === 0) {
        newErrors.push({ fileName: file.name, message: "El archivo está vacío" });
        return;
      }

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
      ) || validFiles.some((existing) => existing.file.name === file.name && existing.file.size === file.size);
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
          message: `Solo se permiten ${MAX_IMAGES} imágenes en total`,
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

  const handleDragEnd = useCallback((event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const ids = form.images.map(imageId);
    const oldIndex = ids.indexOf(String(active.id));
    const newIndex = ids.indexOf(String(over.id));
    if (oldIndex === -1 || newIndex === -1) return;
    set("images")(arrayMove(form.images, oldIndex, newIndex));
  }, [form.images, set]);

  return (
    <div className="pt-2">
      <div className="mb-6">
        <h3 className="font-brand text-xl">Sumale imágenes a tu plan</h3>
        <p className="mt-1 text-[13px] font-bold" style={{ color: "var(--muted-foreground)" }}>
          Son opcionales. La primera será la portada de la actividad y después podrán verse todas en la galería.
        </p>
      </div>

      {errors.length > 0 && (
        <div className="mb-4 space-y-1.5" role="alert" aria-live="polite">
          {errors.map((error, idx) => (
            <div key={idx} className="flex items-start gap-2">
              <AlertCircle className="size-3.5 mt-0.5 shrink-0" style={{ color: "var(--destructive)" }} aria-hidden="true" />
              <p className="text-[11px] font-bold" style={{ color: "var(--destructive)" }}>
                <span className="font-extrabold">{error.fileName}:</span> {error.message}
              </p>
            </div>
          ))}
        </div>
      )}

      <label className="tap min-h-40 flex flex-col items-center justify-center gap-3 rounded-3xl border-2 border-dashed px-6 py-8 cursor-pointer text-center" style={{ borderColor: "var(--border)", background: "var(--muted)" }}>
        <span className="size-14 rounded-2xl bg-white flex items-center justify-center" style={{ boxShadow: "0 3px 0 var(--lav)" }}>
          <ImagePlus className="size-6" style={{ color: "var(--primary)" }} />
        </span>
        <span className="font-brand-title">Elegir imágenes</span>
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
        <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
          <SortableContext items={form.images.map(imageId)} strategy={rectSortingStrategy}>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mt-5">
              {form.images.map((image: { file: File; previewUrl: string }, index: number) => (
                <ImagePreview
                  key={imageId(image)}
                  id={imageId(image)}
                  source={image.previewUrl}
                  fileName={image.file.name}
                  index={index}
                  isCover={index === 0}
                  onRemove={() => handleRemove(index)}
                />
              ))}
            </div>
          </SortableContext>
        </DndContext>
      )}
    </div>
  );
}
