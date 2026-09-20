import React from 'react';
import { ContributorResponse } from '../../types/contributor';
import { ContributorSummary } from '../../types/ownership';

interface ContributorIdentityProps {
  contributor: ContributorResponse | ContributorSummary | null | undefined;
  showEmail?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

export const ContributorIdentity: React.FC<ContributorIdentityProps> = ({
  contributor,
  showEmail = true,
  size = 'md',
}) => {
  if (!contributor) {
    return <span className="text-slate-500 italic text-xs">Unknown Contributor</span>;
  }

  const displayName = contributor.name || contributor.username || contributor.email || 'Anonymous';
  const subtext = contributor.email || contributor.username;
  const initial = displayName.charAt(0).toUpperCase();

  const avatarDimensions =
    size === 'sm' ? 'w-6 h-6 text-xs' : size === 'lg' ? 'w-10 h-10 text-base' : 'w-8 h-8 text-sm';

  return (
    <div className="flex items-center space-x-3">
      {contributor.avatarUrl ? (
        <img
          src={contributor.avatarUrl}
          alt={displayName}
          className={`${avatarDimensions} rounded-full bg-slate-800 object-cover ring-1 ring-slate-700/60`}
          referrerPolicy="no-referrer"
          onError={(e) => {
            // Fallback to avatar placeholder on image error
            (e.target as HTMLElement).style.display = 'none';
          }}
        />
      ) : (
        <div
          className={`${avatarDimensions} rounded-full bg-blue-950 border border-blue-700/60 text-blue-300 font-semibold flex items-center justify-center flex-shrink-0`}
        >
          {initial}
        </div>
      )}

      <div className="min-w-0">
        <div className="font-medium text-slate-200 truncate text-xs sm:text-sm">
          {displayName}
        </div>
        {showEmail && subtext && subtext !== displayName && (
          <div className="text-[11px] font-mono text-slate-400 truncate">
            {subtext}
          </div>
        )}
      </div>
    </div>
  );
};
