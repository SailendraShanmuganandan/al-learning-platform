import { Link } from 'react-router-dom';
import type { RecommendationResponse } from '../types';
import { recommendationToRoute } from '../recommendationHelper';
import { Compass, ArrowRight, Lightbulb } from 'lucide-react';

interface RecommendationCardProps {
  recommendation: RecommendationResponse;
  className?: string;
}

export function RecommendationCard({
  recommendation,
  className = '',
}: RecommendationCardProps) {
  const primaryRoute = recommendationToRoute(recommendation);
  const alternatives = (recommendation.alternatives || []).slice(0, 3);

  return (
    <div
      className={`rounded-3xl border border-primary/20 bg-gradient-to-br from-card via-card to-primary/5 p-6 shadow-sm ${className}`}
    >
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 pb-4 border-b border-border/60">
        <div className="space-y-1">
          <div className="flex items-center gap-1.5 text-xs font-bold text-primary tracking-wide uppercase">
            <Compass size={14} />
            <span>Recommended Next Step</span>
          </div>
          <h3 className="text-lg font-bold text-foreground">
            {recommendation.title}
          </h3>
          <p className="text-xs text-muted-foreground leading-relaxed">
            {recommendation.reason}
          </p>
        </div>

        <Link
          to={primaryRoute.path}
          className="px-5 py-2.5 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center gap-2 shrink-0 self-start md:self-auto"
        >
          <span>{primaryRoute.label}</span>
          <ArrowRight size={14} />
        </Link>
      </div>

      {alternatives.length > 0 && (
        <div className="pt-3">
          <div className="flex items-center gap-1.5 text-[11px] font-bold text-muted-foreground mb-2">
            <Lightbulb size={12} className="text-amber-500" />
            <span>Or explore other options:</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
            {alternatives.map((alt, i) => {
              const altRoute = recommendationToRoute(alt);
              return (
                <Link
                  key={i}
                  to={altRoute.path}
                  className="p-3 rounded-2xl border border-border bg-card/60 hover:bg-muted/40 hover:border-primary/30 transition text-left group"
                >
                  <p className="text-xs font-bold text-foreground group-hover:text-primary transition truncate">
                    {alt.title}
                  </p>
                  <p className="text-[11px] text-muted-foreground truncate mt-0.5">
                    {alt.reason}
                  </p>
                </Link>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

