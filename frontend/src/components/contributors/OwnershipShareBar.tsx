import React from 'react';

interface OwnershipShareBarProps {
  share: number | null | undefined;
  showPercentage?: boolean;
}

export const OwnershipShareBar: React.FC<OwnershipShareBarProps> = ({
  share,
  showPercentage = true,
}) => {
  if (share === null || share === undefined) {
    return <span className="text-slate-500 italic text-xs">—</span>;
  }

  const percentage = Math.min(100, Math.max(0, share * 100));
  const formattedPercent = `${percentage.toFixed(1)}%`;

  return (
    <div className="flex items-center space-x-2.5">
      <div className="w-24 bg-slate-800 rounded-full h-2 overflow-hidden ring-1 ring-slate-700/50">
        <div
          className="h-full bg-blue-500 rounded-full transition-all duration-300"
          style={{ width: `${percentage}%` }}
        />
      </div>
      {showPercentage && (
        <span className="font-mono text-xs font-semibold text-slate-300 min-w-[42px]">
          {formattedPercent}
        </span>
      )}
    </div>
  );
};
