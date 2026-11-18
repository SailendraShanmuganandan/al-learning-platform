import { Link, useNavigate } from 'react-router-dom';
import { PageHeader } from './PageHeader';
import { Sparkles, ArrowLeft } from 'lucide-react';

export interface ComingSoonPageProps {
  title?: string;
  description?: string;
  category?: string;
  backUrl?: string;
  backLabel?: string;
}

export function ComingSoonPage({
  title = 'Coming Soon',
  description = 'This feature is currently under active development and will be available soon.',
  category,
  backUrl,
  backLabel = 'Go Back',
}: ComingSoonPageProps) {
  const navigate = useNavigate();

  return (
    <div>
      <PageHeader
        title={
          <div className="flex items-center gap-2">
            <span>{title}</span>
            {category && (
              <span className="rounded-full bg-primary/10 text-primary border border-primary/20 px-2.5 py-0.5 text-xs font-semibold">
                {category}
              </span>
            )}
          </div>
        }
        description={description}
      />

      <div className="rounded-3xl border border-dashed border-border bg-card/60 p-12 text-center flex flex-col items-center justify-center space-y-4 shadow-xs">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/10 text-primary">
          <Sparkles size={32} />
        </div>
        <div className="space-y-1 max-w-md">
          <h2 className="text-lg font-bold text-foreground">Feature in Development</h2>
          <p className="text-xs text-muted-foreground leading-relaxed">
            We are working hard to bring you this part of the A/L ICT Learning Platform. Check back in the next update.
          </p>
        </div>

        <div className="pt-2 flex items-center gap-3">
          {backUrl ? (
            <Link
              to={backUrl}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl border border-border text-xs font-semibold hover:bg-muted transition"
            >
              <ArrowLeft size={14} />
              <span>{backLabel}</span>
            </Link>
          ) : (
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl border border-border text-xs font-semibold hover:bg-muted transition"
            >
              <ArrowLeft size={14} />
              <span>{backLabel}</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
