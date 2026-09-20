import React from 'react';
import { CommitClassification } from '../../types/commit';

interface CommitClassificationBadgeProps {
  classification: CommitClassification | null | undefined;
  size?: 'sm' | 'md';
}

const CLASSIFICATION_CONFIG: Record<
  CommitClassification,
  { label: string; bg: string; text: string; border: string }
> = {
  FEATURE: {
    label: 'Feature',
    bg: 'bg-emerald-950/60',
    text: 'text-emerald-400',
    border: 'border-emerald-800/60',
  },
  BUG_FIX: {
    label: 'Bug Fix',
    bg: 'bg-rose-950/60',
    text: 'text-rose-400',
    border: 'border-rose-800/60',
  },
  REFACTOR: {
    label: 'Refactor',
    bg: 'bg-amber-950/60',
    text: 'text-amber-400',
    border: 'border-amber-800/60',
  },
  DOCUMENTATION: {
    label: 'Docs',
    bg: 'bg-sky-950/60',
    text: 'text-sky-400',
    border: 'border-sky-800/60',
  },
  TEST: {
    label: 'Test',
    bg: 'bg-purple-950/60',
    text: 'text-purple-400',
    border: 'border-purple-800/60',
  },
  BUILD: {
    label: 'Build',
    bg: 'bg-indigo-950/60',
    text: 'text-indigo-400',
    border: 'border-indigo-800/60',
  },
  CONFIGURATION: {
    label: 'Config',
    bg: 'bg-teal-950/60',
    text: 'text-teal-400',
    border: 'border-teal-800/60',
  },
  DEPENDENCY: {
    label: 'Dependency',
    bg: 'bg-cyan-950/60',
    text: 'text-cyan-400',
    border: 'border-cyan-800/60',
  },
  OTHER: {
    label: 'Other',
    bg: 'bg-slate-800/80',
    text: 'text-slate-300',
    border: 'border-slate-700/60',
  },
};

export const CommitClassificationBadge: React.FC<CommitClassificationBadgeProps> = ({
  classification,
  size = 'sm',
}) => {
  const sizeClasses =
    size === 'sm' ? 'px-2 py-0.5 text-[10px]' : 'px-2.5 py-1 text-xs';

  if (!classification) {
    return (
      <span
        className={`inline-flex items-center font-medium rounded border border-slate-700/60 bg-slate-900/80 text-slate-400 ${sizeClasses}`}
        title="Unclassified commit"
      >
        Unclassified
      </span>
    );
  }

  const config = CLASSIFICATION_CONFIG[classification] || {
    label: classification,
    bg: 'bg-slate-800/80',
    text: 'text-slate-300',
    border: 'border-slate-700/60',
  };

  return (
    <span
      className={`inline-flex items-center font-medium rounded border ${config.bg} ${config.text} ${config.border} ${sizeClasses}`}
    >
      {config.label}
    </span>
  );
};
