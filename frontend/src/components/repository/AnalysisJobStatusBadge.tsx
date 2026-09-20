import React from 'react';
import { AnalysisJobStatus } from '../../types/analysisJob';
import { Badge, BadgeVariant } from '../common/Badge';

interface AnalysisJobStatusBadgeProps {
  status: AnalysisJobStatus;
  isPolling?: boolean;
}

const statusConfig: Record<AnalysisJobStatus, { label: string; variant: BadgeVariant }> = {
  PENDING: { label: 'PENDING', variant: 'warning' },
  RUNNING: { label: 'RUNNING', variant: 'primary' },
  COMPLETED: { label: 'COMPLETED', variant: 'success' },
  FAILED: { label: 'FAILED', variant: 'danger' },
};

export const AnalysisJobStatusBadge: React.FC<AnalysisJobStatusBadgeProps> = ({
  status,
  isPolling = false,
}) => {
  const config = statusConfig[status] || { label: status, variant: 'default' };

  return (
    <span className="inline-flex items-center gap-1.5">
      <Badge variant={config.variant}>
        {status === 'RUNNING' || isPolling ? (
          <span className="mr-1 inline-block h-1.5 w-1.5 animate-ping rounded-full bg-blue-500" />
        ) : null}
        {config.label}
      </Badge>
    </span>
  );
};
