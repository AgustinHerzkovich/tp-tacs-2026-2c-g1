"use client"

import * as React from "react"
import { cn } from "cn"
import { Switch as SwitchPrimitive } from "radix-ui"

function Switch({ className, ...props }: React.ComponentProps<typeof SwitchPrimitive.Root>) {
  return (
    <SwitchPrimitive.Root
      data-slot="switch"
      className={cn(
        "relative inline-flex h-6 w-[42px] shrink-0 items-center rounded-full border-2 outline-none transition-colors disabled:cursor-not-allowed disabled:opacity-50 focus-visible:ring-3 focus-visible:ring-ring/50 data-[state=checked]:border-primary data-[state=checked]:bg-primary data-[state=unchecked]:border-border data-[state=unchecked]:bg-white",
        className
      )}
      {...props}
    >
      <SwitchPrimitive.Thumb
        data-slot="switch-thumb"
        className="pointer-events-none absolute top-1/2 size-[18px] -translate-y-1/2 rounded-full bg-white transition-[left] data-[state=checked]:left-[19px] data-[state=checked]:shadow-[0_2px_4px_rgba(58,51,82,.35)] data-[state=unchecked]:left-[2px] data-[state=unchecked]:bg-border"
      />
    </SwitchPrimitive.Root>
  )
}

export { Switch }
