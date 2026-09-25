"use client";

import { useEffect } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Chip } from "@/components/common/Chip";
import { useAuth } from "@/hooks/useAuth";
import { useHasMounted } from "@/hooks/useHasMounted";
import type { Tone } from "@/lib/activityVisuals";

/** 16px outer radius minus the 6px (p-1.5) sticker border gives the inner
 * photo a 10px radius, so the two corners stay concentric instead of one
 * cutting sharper than the other. */
const PHOTO_CARD_CLASS = "rounded-[16px] bg-white p-1.5 shadow-[0_14px_26px_-10px_rgba(58,51,82,.4)]";
/** `next/image`'s `fill` positions absolutely against the card's padding
 * box, so it would cover the white sticker border instead of respecting it
 * — this inner wrapper gives it its own (unpadded) box to fill. */
const PHOTO_IMAGE_WRAP_CLASS = "relative h-full w-full overflow-hidden rounded-[10px]";

interface Feature {
  icon: string;
  tone: Tone;
  title: string;
  description: string;
}

const FEATURES: Feature[] = [
  { icon: "📅", tone: "mint", title: "Armá el plan", description: "Elegí lugar, fecha y sumá gente en dos minutos." },
  {
    icon: "🌦️",
    tone: "sky",
    title: "Vigilamos el cielo",
    description: "Si el pronóstico empeora, te avisamos antes de que se arruine la salida.",
  },
  {
    icon: "🗳️",
    tone: "violet",
    title: "El grupo decide",
    description: "Abrimos una votación para reprogramar o cancelar — nadie se entera tarde.",
  },
];

const TRUST_ITEMS = ["☀️ Pronóstico en tiempo real", "🗳️ Decisiones en grupo", "🔒 Tu cuenta, tus datos"];

const LINK_CLASS =
  "h-auto p-0 text-[13.5px] font-extrabold text-foreground underline decoration-wavy decoration-[#ffb59c] underline-offset-[3px] hover:text-primary";

const HEADER_CTA_CLASS =
  "hidden h-auto rounded-lg px-5 py-[11px] text-[13px] font-black shadow-[0_4px_0_var(--secondary-foreground)] hover:shadow-[0_2px_0_var(--secondary-foreground)] hover:translate-y-[2px] lg:inline-flex";

/** Public marketing entry point at "/" — shown to signed-out visitors as an
 * alternative to the bare `LoginPage`. Redirects away once Keycloak confirms
 * a session, same as `LoginPage`, so an already-logged-in visitor never sees
 * the pitch again. */
export function LandingPage() {
  const router = useRouter();
  const { initialized, isAuthenticated, login, register } = useAuth();
  const ready = useHasMounted() && initialized;

  useEffect(() => {
    if (isAuthenticated) router.replace("/explorar");
  }, [isAuthenticated, router]);

  const handleLogin = () => void login();
  const handleRegister = () => void register();

  return (
    <main className="fade-in min-h-screen">
      <div className="flex items-center justify-between px-5 pt-6 pb-3 lg:mx-auto lg:max-w-[1280px] lg:px-10">
        <span
          className="font-brand sticker-outline text-[28px] lg:text-[34px]"
          style={{ color: "var(--primary)", transform: "rotate(-3deg)", filter: "drop-shadow(2px 3px 0 rgba(46,42,69,.18))" }}
        >
          Planazo
        </span>
        <div className="flex items-center gap-2 lg:gap-5">
          <Button variant="link" className={LINK_CLASS} disabled={!ready} onClick={handleLogin}>
            Ingresar
          </Button>
          <Button size="lg" className={HEADER_CTA_CLASS} disabled={!ready} onClick={handleRegister}>
            Unite a la comunidad
          </Button>
        </div>
      </div>

      <div className="px-5 pt-4 pb-2 lg:mx-auto lg:grid lg:max-w-[1280px] lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:gap-12 lg:px-10 lg:pt-8 lg:pb-8">
        <div className="text-center lg:text-left">
          <Chip tone="sun" sticker rotate={-1.5} className="mb-4 text-[11.5px] lg:text-xs">
            🌤️ Clima + planes, todo junto
          </Chip>
          <h1 className="font-brand text-[28px] leading-[1.18] lg:text-[44px] lg:leading-[1.16]">
            Armá el plan.
            <br />
            Nosotros vigilamos{" "}
            <span className="sticker-outline" style={{ color: "var(--primary)" }}>
              el cielo
            </span>
            .
          </h1>
          <p
            className="mt-3 mb-[22px] text-sm font-bold leading-relaxed lg:mt-4 lg:mb-7 lg:max-w-[440px] lg:text-[15.5px]"
            style={{ color: "var(--muted-foreground)" }}
          >
            Organizá salidas con amigos y dejá que el grupo decida si hay que cambiar de fecha cuando el pronóstico se
            pone feo.
          </p>

          <div className="scrollbar-none mb-6 flex gap-2.5 overflow-x-auto pt-3.5 pb-1 lg:hidden" aria-hidden="true">
            <div className={`h-[134px] w-[108px] shrink-0 -rotate-[4deg] ${PHOTO_CARD_CLASS}`}>
              <div className={PHOTO_IMAGE_WRAP_CLASS}>
                <Image src="/landing/rafting.jpg" alt="" fill sizes="108px" className="object-cover" />
              </div>
            </div>
            <div className={`h-[134px] w-[108px] shrink-0 rotate-[3deg] ${PHOTO_CARD_CLASS}`}>
              <div className={PHOTO_IMAGE_WRAP_CLASS}>
                <Image src="/landing/camping.jpg" alt="" fill sizes="108px" className="object-cover" />
              </div>
            </div>
            <div className={`h-[134px] w-[108px] shrink-0 -rotate-3 ${PHOTO_CARD_CLASS}`}>
              <div className={PHOTO_IMAGE_WRAP_CLASS}>
                <Image src="/landing/mate.jpg" alt="" fill sizes="108px" className="object-cover" />
              </div>
            </div>
            <div className={`h-[134px] w-[108px] shrink-0 rotate-[4deg] ${PHOTO_CARD_CLASS}`}>
              <div className={PHOTO_IMAGE_WRAP_CLASS}>
                <Image src="/landing/fogon.jpg" alt="" fill sizes="108px" className="object-cover" />
              </div>
            </div>
            <div className={`h-[134px] w-[108px] shrink-0 -rotate-2 ${PHOTO_CARD_CLASS}`}>
              <div className={PHOTO_IMAGE_WRAP_CLASS}>
                <Image src="/landing/billiards.jpg" alt="" fill sizes="108px" className="object-cover" />
              </div>
            </div>
            <div className={`h-[134px] w-[108px] shrink-0 rotate-[5deg] ${PHOTO_CARD_CLASS}`}>
              <div className={PHOTO_IMAGE_WRAP_CLASS}>
                <Image src="/landing/boardgame.jpg" alt="" fill sizes="108px" className="object-cover" />
              </div>
            </div>
          </div>

          <div className="flex flex-col items-center gap-3 lg:flex-row lg:gap-6">
            <Button size="xl" className="w-full rounded-lg lg:w-auto" disabled={!ready} onClick={handleRegister}>
              {ready ? "Unite a la comunidad" : "Comprobando sesión..."} <span aria-hidden="true">→</span>
            </Button>
            <Button variant="link" className={LINK_CLASS} disabled={!ready} onClick={handleLogin}>
              Ya tengo cuenta · Ingresar
            </Button>
          </div>

          <div className="mt-7 hidden flex-wrap gap-5 lg:flex">
            {TRUST_ITEMS.map((item) => (
              <span key={item} className="text-[12.5px] font-bold" style={{ color: "var(--muted-foreground)" }}>
                {item}
              </span>
            ))}
          </div>
        </div>

        <div className="relative hidden h-[380px] lg:block" aria-hidden="true">
          <div className={`absolute top-0 left-0 h-[183px] w-[166px] -rotate-3 ${PHOTO_CARD_CLASS}`}>
            <div className={PHOTO_IMAGE_WRAP_CLASS}>
              <Image src="/landing/rafting.jpg" alt="" fill sizes="166px" className="object-cover" />
            </div>
          </div>
          <div className={`absolute top-1.5 left-[182px] h-[183px] w-[166px] rotate-2 ${PHOTO_CARD_CLASS}`}>
            <div className={PHOTO_IMAGE_WRAP_CLASS}>
              <Image src="/landing/billiards.jpg" alt="" fill sizes="166px" className="object-cover" />
            </div>
          </div>
          <div className={`absolute top-0 left-[364px] h-[183px] w-[166px] -rotate-2 ${PHOTO_CARD_CLASS}`}>
            <div className={PHOTO_IMAGE_WRAP_CLASS}>
              <Image src="/landing/boardgame.jpg" alt="" fill sizes="166px" className="object-cover" />
            </div>
          </div>
          <div className={`absolute top-[197px] left-0 h-[183px] w-[166px] rotate-2 ${PHOTO_CARD_CLASS}`}>
            <div className={PHOTO_IMAGE_WRAP_CLASS}>
              <Image src="/landing/camping.jpg" alt="" fill sizes="166px" className="object-cover" />
            </div>
          </div>
          <div className={`absolute top-[191px] left-[182px] h-[183px] w-[166px] -rotate-3 ${PHOTO_CARD_CLASS}`}>
            <div className={PHOTO_IMAGE_WRAP_CLASS}>
              <Image src="/landing/mate.jpg" alt="" fill sizes="166px" className="object-cover" />
            </div>
          </div>
          <div className={`absolute top-[197px] left-[364px] h-[183px] w-[166px] rotate-3 ${PHOTO_CARD_CLASS}`}>
            <div className={PHOTO_IMAGE_WRAP_CLASS}>
              <Image src="/landing/fogon.jpg" alt="" fill sizes="166px" className="object-cover" />
            </div>
          </div>
        </div>
      </div>

      <div className="bg-muted py-8 text-center lg:py-14">
        <div className="px-5 lg:mx-auto lg:max-w-[1280px] lg:px-10">
          <p className="text-[11px] font-black uppercase tracking-wide" style={{ color: "var(--primary)" }}>
            Por qué Planazo
          </p>
          <h2 className="font-brand mt-2 mb-6 text-2xl lg:mb-7 lg:text-[28px]">Todo lo que necesitás para armar un plan</h2>
          <div className="grid gap-3.5 lg:grid-cols-3 lg:gap-5">
            {FEATURES.map((feature) => (
              <div
                key={feature.title}
                className="flex items-start gap-3.5 rounded-[20px] bg-white p-4 text-left shadow-[0_6px_16px_-10px_rgba(58,51,82,.22)] lg:flex-col lg:items-center lg:gap-0 lg:p-6 lg:text-center"
              >
                <div
                  className="flex size-[46px] shrink-0 items-center justify-center rounded-full border-2 border-white text-xl shadow-[0_3px_6px_rgba(58,51,82,.15)] lg:mb-3.5 lg:size-[52px] lg:text-2xl"
                  style={{ background: `var(--${feature.tone})` }}
                  aria-hidden="true"
                >
                  {feature.icon}
                </div>
                <div>
                  <h3 className="font-brand-title text-[15.5px] lg:text-base">{feature.title}</h3>
                  <p
                    className="mt-1 text-[12.5px] font-bold leading-relaxed lg:text-[13px]"
                    style={{ color: "var(--muted-foreground)" }}
                  >
                    {feature.description}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="flex flex-col gap-4 px-5 pt-6 pb-8 lg:mx-auto lg:grid lg:max-w-[1280px] lg:grid-cols-2 lg:gap-6 lg:px-10 lg:pt-10 lg:pb-10">
        <div className="bg-rose relative rounded-tl-[4px] rounded-tr-[20px] rounded-br-[20px] rounded-bl-[20px] px-[18px] pt-5 pb-4 shadow-[0_6px_14px_-8px_rgba(58,51,82,.28)] -rotate-1 lg:flex lg:flex-col lg:justify-center lg:px-[28px] lg:py-[26px]">
          <span className="emoji-3d absolute -top-3.5 left-[22px] text-[22px]" style={{ transform: "rotate(-12deg)" }} aria-hidden="true">
            📌
          </span>
          <p className="text-rose-ink text-[13.5px] font-extrabold leading-[1.45] lg:text-base">
            &ldquo;Nunca más se nos arruinó un asado sin avisar.&rdquo;
          </p>
          <div className="mt-2.5 flex items-center gap-2 lg:mt-3">
            <span className="text-rose-ink flex size-[30px] items-center justify-center rounded-full border-2 border-white bg-white text-[10.5px] font-black lg:size-8 lg:text-[11px]">
              CO
            </span>
            <span className="text-rose-ink text-[11.5px] font-extrabold lg:text-[12.5px]">Coti, organizadora</span>
          </div>
        </div>

        <div
          className="flex flex-col items-center gap-4 rounded-[22px] px-6 py-6 text-center shadow-[0_8px_20px_-10px_rgba(255,90,60,.55)] lg:flex-row lg:items-center lg:justify-between lg:px-8 lg:text-left"
          style={{ background: "var(--primary)" }}
        >
          <div>
            <h3 className="font-brand text-xl text-white">¿List@ para tu próximo plan?</h3>
            <p className="mt-1 text-[12.5px] font-bold" style={{ color: "#ffe1d1" }}>
              Es gratis y te lleva un minuto.
            </p>
          </div>
          <Button
            size="xl"
            className="w-full shrink-0 rounded-lg bg-white shadow-[0_4px_0_#ffcbb8] hover:bg-white hover:shadow-[0_2px_0_#ffcbb8] lg:w-auto"
            style={{ color: "var(--primary)" }}
            disabled={!ready}
            onClick={handleRegister}
          >
            Crear cuenta gratis
          </Button>
        </div>
      </div>

      <div className="border-t-2 py-6 text-center" style={{ borderColor: "var(--border)" }}>
        <p className="px-5 text-[11px] font-bold lg:mx-auto lg:max-w-[1280px] lg:px-10" style={{ color: "var(--muted-foreground)" }}>
          Planazo · organizá sin sorpresas del clima
        </p>
      </div>
    </main>
  );
}
