import type { ReactNode } from "react";

export function PageHeader({
  title,
  description,
  actions,
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <header className="mb-5 flex flex-wrap items-center gap-4">
      <div>
        <h1 className="text-[19px] font-semibold tracking-[-0.02em]">{title}</h1>
        {description ? (
          <p className="mt-px text-[12.5px] text-slate-2">{description}</p>
        ) : null}
      </div>
      {actions ? <div className="ml-auto flex items-center gap-2">{actions}</div> : null}
    </header>
  );
}
