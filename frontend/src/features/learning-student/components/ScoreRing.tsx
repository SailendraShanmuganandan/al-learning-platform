interface ScoreRingProps {
  percentage: number;
  size?: number;
  strokeWidth?: number;
  className?: string;
}

export function ScoreRing({
  percentage,
  size = 80,
  strokeWidth = 7,
  className = '',
}: ScoreRingProps) {
  const clamped = Math.max(0, Math.min(100, Math.round(percentage)));
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (clamped / 100) * circumference;

  let colorClass: string;
  if (clamped >= 75) colorClass = 'text-emerald-500';
  else if (clamped >= 50) colorClass = 'text-sky-500';
  else if (clamped >= 35) colorClass = 'text-amber-500';
  else colorClass = 'text-rose-500';

  return (
    <div className={`relative inline-flex items-center justify-center ${className}`}>
      <svg width={size} height={size} className="transform -rotate-90">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke="currentColor"
          strokeWidth={strokeWidth}
          className="text-muted/30"
          fill="none"
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke="currentColor"
          strokeWidth={strokeWidth}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          className={`${colorClass} transition-all duration-700 ease-out`}
          fill="none"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
        <span className="text-base font-extrabold tracking-tight text-foreground">
          {clamped}%
        </span>
      </div>
    </div>
  );
}

interface ProgressBarProps {
  value: number; // 0 to 100
  height?: number;
  className?: string;
  color?: string;
  showLabel?: boolean;
}

export function ProgressBar({
  value,
  height = 8,
  className = '',
  color,
  showLabel = false,
}: ProgressBarProps) {
  const clamped = Math.max(0, Math.min(100, Math.round(value)));

  let barColor = color;
  if (!barColor) {
    if (clamped >= 75) barColor = 'bg-emerald-500';
    else if (clamped >= 50) barColor = 'bg-sky-500';
    else if (clamped >= 35) barColor = 'bg-amber-500';
    else barColor = 'bg-rose-500';
  }

  return (
    <div className={`w-full ${className}`}>
      {showLabel && (
        <div className="flex justify-between text-xs text-muted-foreground mb-1 font-medium">
          <span>Progress</span>
          <span>{clamped}%</span>
        </div>
      )}
      <div
        className="w-full rounded-full bg-muted overflow-hidden"
        style={{ height: `${height}px` }}
      >
        <div
          className={`h-full rounded-full transition-all duration-500 ease-out ${barColor}`}
          style={{ width: `${clamped}%` }}
        />
      </div>
    </div>
  );
}
