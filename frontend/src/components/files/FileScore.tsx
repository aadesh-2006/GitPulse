import React from 'react';

interface FileScoreProps {
  score: number | null | undefined;
  showBar?: boolean;
  className?: string;
  size?: 'sm' | 'md' | 'lg';
}

export const FileScore: React.FC<FileScoreProps> = ({
  score,
  showBar = true,
  className = '',
  size = 'md',
}) => {
  if (score === null || score === undefined || Number.isNaN(score)) {
    return <span className="text-slate-500 font-mono text-xs">—</span>;
  }

  const clampedScore = Math.max(0, Math.min(1, score));
  const formattedScore = clampedScore.toFixed(2);
  const percentage = Math.round(clampedScore * 100);

  const textSize =
    size === 'sm' ? 'text-xs' : size === 'lg' ? 'text-base' : 'text-sm';

  return (
    <div className={`inline-flex items-center gap-2 ${className}`}>
      <span className={`font-mono font-semibold text-slate-200 ${textSize}`}>
        {formattedScore}
      </span>
      {showBar && (
        <div className="h-1.5 w-12 rounded-full bg-slate-800 overflow-hidden border border-slate-700/60">
          <div
            className="h-full rounded-full bg-blue-500 transition-all duration-300"
            style={{ width: `${percentage}%` }}
          />
        </div>
      )}
    </div>
  );
};
