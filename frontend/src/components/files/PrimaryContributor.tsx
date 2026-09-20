import React from 'react';
import { PrimaryContributorSummaryResponse } from '../../types/file';

interface PrimaryContributorProps {
  contributor: PrimaryContributorSummaryResponse | null | undefined;
  className?: string;
}

export const PrimaryContributor: React.FC<PrimaryContributorProps> = ({
  contributor,
  className = '',
}) => {
  if (!contributor) {
    return <span className="text-slate-500 italic text-xs">Unassigned</span>;
  }

  const displayName =
    contributor.name || contributor.username || contributor.email.split('@')[0];

  return (
    <div className={`inline-flex items-center space-x-1.5 text-xs ${className}`}>
      {contributor.avatarUrl ? (
        <img
          src={contributor.avatarUrl}
          alt={displayName}
          className="h-4 w-4 rounded-full border border-slate-700 object-cover"
        />
      ) : (
        <div className="flex h-4 w-4 items-center justify-center rounded-full bg-slate-800 text-[9px] font-bold text-slate-300">
          {displayName.charAt(0).toUpperCase()}
        </div>
      )}
      <span className="font-medium text-slate-300 truncate max-w-[140px]" title={contributor.email}>
        {displayName}
      </span>
    </div>
  );
};
