"use client"

import * as React from "react"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { DayPicker, getDefaultClassNames, type ChevronProps } from "react-day-picker"
import { cn } from "cn"

function Calendar({
  className,
  classNames,
  showOutsideDays = true,
  ...props
}: React.ComponentProps<typeof DayPicker>) {
  const defaultClassNames = getDefaultClassNames()

  return (
    <DayPicker
      showOutsideDays={showOutsideDays}
      className={cn("p-0 font-body", className)}
      classNames={{
        // `Nav` renders as a sibling of `.month` (a direct child of
        // `.months`), not inside it — it has to be `relative` here, not on
        // `.month`, or the absolutely-positioned nav escapes to whatever
        // ancestor happens to be positioned (previously the popover itself,
        // which put the arrows on top of unrelated content above the grid).
        months: cn(defaultClassNames.months, "relative flex flex-col sm:flex-row gap-6 mx-auto w-fit"),
        month: cn(defaultClassNames.month, "flex flex-col gap-3"),
        month_caption: cn(defaultClassNames.month_caption, "flex items-center justify-center h-10 px-10 font-display font-semibold text-[15px]"),
        nav: cn(defaultClassNames.nav, "flex items-center justify-between absolute inset-x-0 top-0 h-10 px-1"),
        button_previous: cn(
          defaultClassNames.button_previous,
          "tap size-8 rounded-full flex items-center justify-center hover:bg-muted disabled:opacity-30"
        ),
        button_next: cn(
          defaultClassNames.button_next,
          "tap size-8 rounded-full flex items-center justify-center hover:bg-muted disabled:opacity-30"
        ),
        weekdays: cn(defaultClassNames.weekdays, "flex"),
        weekday: cn(defaultClassNames.weekday, "size-11 flex items-center justify-center text-[11.5px] font-extrabold uppercase text-muted-foreground"),
        week: cn(defaultClassNames.week, "flex w-full"),
        day: cn(defaultClassNames.day, "relative size-11 p-0 text-center text-[14px] font-semibold"),
        day_button: cn(
          defaultClassNames.day_button,
          "tap size-11 rounded-full flex items-center justify-center hover:bg-muted"
        ),
        // `data-selected` (and every other day-state flag) lands on the day
        // *cell*, not on the button inside it — DayButton only ever gets the
        // static `day_button` class, so every state below has to reach into
        // its own child button via `[&>button]`, and the connecting fill for
        // a range has to be painted on the cell itself (the button is the
        // same size as the cell, so a cell-level background shows all the
        // way to its edges and butts up against the next cell with no gap).
        range_start: cn(
          defaultClassNames.range_start,
          "rounded-l-full bg-secondary [&>button]:border-2 [&>button]:border-white [&>button]:bg-primary [&>button]:text-primary-foreground [&>button]:shadow-[0_2px_0_var(--secondary-foreground)] [&>button]:hover:bg-primary"
        ),
        range_end: cn(
          defaultClassNames.range_end,
          "rounded-r-full bg-secondary [&>button]:border-2 [&>button]:border-white [&>button]:bg-primary [&>button]:text-primary-foreground [&>button]:shadow-[0_2px_0_var(--secondary-foreground)] [&>button]:hover:bg-primary"
        ),
        range_middle: cn(
          defaultClassNames.range_middle,
          "bg-secondary [&>button]:rounded-none [&>button]:hover:bg-transparent"
        ),
        today: cn(defaultClassNames.today, "[&:not([data-selected])>button]:ring-2 [&:not([data-selected])>button]:ring-primary/40"),
        outside: cn(defaultClassNames.outside, "text-muted-foreground/40"),
        disabled: cn(defaultClassNames.disabled, "text-muted-foreground/30 opacity-50"),
        ...classNames,
      }}
      components={{
        Chevron: (chevronProps: ChevronProps) =>
          chevronProps.orientation === "left" ? (
            <ChevronLeft className="size-4" />
          ) : (
            <ChevronRight className="size-4" />
          ),
      }}
      {...props}
    />
  )
}

export { Calendar }
