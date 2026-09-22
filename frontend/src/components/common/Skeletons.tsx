import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

/** Content-shaped loading placeholders — one per real layout (explore card,
 * mis-actividades card, activity detail) so a page's skeleton state already
 * has the right rhythm before data arrives, instead of a generic spinner
 * jumping into a totally different layout. */

export function ExploreCardSkeleton() {
  return (
    <Card className="mb-5 overflow-hidden gap-0 py-0 lg:mb-0 lg:h-full" aria-hidden="true">
      <Skeleton className="h-[180px] w-full rounded-none" />
      <div className="p-4">
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-3 w-1/2 mt-2.5" />
        <Skeleton className="h-3 w-2/5 mt-1.5" />
        <div className="flex items-center justify-between mt-3.5">
          <Skeleton className="h-6 w-16 rounded-full" />
          <Skeleton className="h-3 w-20" />
        </div>
      </div>
    </Card>
  );
}

export function ExploreGridSkeleton({ count = 6 }: { count?: number }) {
  return (
    <div className="lg:grid lg:grid-cols-2 xl:grid-cols-3 lg:gap-5" aria-busy="true" aria-label="Cargando actividades">
      <span className="sr-only" role="status">Cargando actividades</span>
      {Array.from({ length: count }, (_, i) => (
        <ExploreCardSkeleton key={i} />
      ))}
    </div>
  );
}

export function MisCardSkeleton() {
  return (
    <Card className="mb-4 overflow-hidden gap-0 py-0 lg:mb-0 lg:h-full" aria-hidden="true">
      <div className="flex gap-3.5 p-3.5">
        <Skeleton className="w-[84px] h-[84px] shrink-0 rounded-2xl" />
        <div className="min-w-0 flex-1 py-0.5">
          <Skeleton className="h-4 w-14 rounded-full mb-2" />
          <Skeleton className="h-3.5 w-4/5" />
          <Skeleton className="h-3 w-2/5 mt-1.5" />
          <div className="flex items-center justify-between mt-2.5">
            <Skeleton className="h-5 w-12 rounded-full" />
            <Skeleton className="h-3 w-8" />
          </div>
        </div>
      </div>
    </Card>
  );
}

export function MisGridSkeleton({ count = 4 }: { count?: number }) {
  return (
    <div className="lg:grid lg:grid-cols-2 xl:grid-cols-3 lg:gap-4" aria-busy="true" aria-label="Cargando tus actividades">
      <span className="sr-only" role="status">Cargando tus actividades</span>
      {Array.from({ length: count }, (_, i) => (
        <MisCardSkeleton key={i} />
      ))}
    </div>
  );
}

export function StatisticsSkeleton() {
  return (
    <div aria-busy="true" aria-label="Cargando estadísticas">
      <span className="sr-only" role="status">Cargando estadísticas</span>
      <Skeleton className="h-3.5 w-48 mb-3" />
      <div className="grid grid-cols-2 gap-3 mb-4">
        {Array.from({ length: 4 }, (_, i) => (
          <Card key={i} className="p-4">
            <Skeleton className="h-8 w-12 mx-4" />
            <Skeleton className="h-3 w-20 mx-4 mt-2" />
          </Card>
        ))}
      </div>
      <Card className="p-5">
        <Skeleton className="h-5 w-32 mx-4 mb-3" />
        <div className="grid grid-cols-2 gap-3 px-4">
          {Array.from({ length: 4 }, (_, i) => (
            <Skeleton key={i} className="h-8 w-full" />
          ))}
        </div>
      </Card>
    </div>
  );
}

export function ActivityDetailSkeleton() {
  return (
    <div className="fade-in lg:max-w-5xl lg:mx-auto lg:py-8 lg:px-8" aria-busy="true" aria-label="Cargando actividad">
      <span className="sr-only" role="status">Cargando actividad</span>
      <Skeleton className="h-[260px] w-full rounded-none lg:rounded-3xl" />
      <div className="px-5 pt-5 lg:px-0">
        <div className="mb-4">
          <Skeleton className="h-4 w-32 mb-3" />
          <Skeleton className="h-3 w-full mb-1.5" />
          <Skeleton className="h-3 w-5/6 mb-4" />
          <div className="grid grid-cols-2 gap-3">
            <Skeleton className="h-16 rounded-2xl" />
            <Skeleton className="h-16 rounded-2xl" />
          </div>
        </div>
      </div>
    </div>
  );
}
