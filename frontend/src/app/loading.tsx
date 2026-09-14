import { ExploreGridSkeleton } from "@/components/common/Skeletons";

export default function Loading() {
  return (
    <main className="px-5 pt-6 pb-6 lg:px-10 lg:pt-8">
      <ExploreGridSkeleton count={4} />
    </main>
  );
}
