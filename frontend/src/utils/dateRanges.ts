export type DateRangePreset = '3m' | '6m' | '12m' | 'custom';

export interface EvolutionDateRange {
  preset: DateRangePreset;
  currentFrom: string;
  currentTo: string;
  previousFrom: string;
  previousTo: string;
  customStartDate?: string;
  customEndDate?: string;
}

export function calculateDateRange(
  preset: DateRangePreset,
  customStart?: string,
  customEnd?: string
): EvolutionDateRange {
  const now = new Date();

  if (preset === 'custom' && customStart && customEnd) {
    const fromDate = new Date(`${customStart}T00:00:00.000Z`);
    // End date is inclusive in user intent, so we make exclusive boundary at the start of the next second/day
    const toDate = new Date(`${customEnd}T23:59:59.999Z`);
    const durationMs = toDate.getTime() - fromDate.getTime();
    const prevToDate = new Date(fromDate.getTime());
    const prevFromDate = new Date(fromDate.getTime() - durationMs);

    return {
      preset: 'custom',
      currentFrom: fromDate.toISOString(),
      currentTo: toDate.toISOString(),
      previousFrom: prevFromDate.toISOString(),
      previousTo: prevToDate.toISOString(),
      customStartDate: customStart,
      customEndDate: customEnd,
    };
  }

  const months = preset === '3m' ? 3 : preset === '12m' ? 12 : 6;

  // Align to current time
  const currentToDate = new Date(now.getTime());
  const currentFromDate = new Date(now.getTime());
  currentFromDate.setMonth(currentFromDate.getMonth() - months);

  const durationMs = currentToDate.getTime() - currentFromDate.getTime();
  const previousToDate = new Date(currentFromDate.getTime());
  const previousFromDate = new Date(currentFromDate.getTime() - durationMs);

  return {
    preset,
    currentFrom: currentFromDate.toISOString(),
    currentTo: currentToDate.toISOString(),
    previousFrom: previousFromDate.toISOString(),
    previousTo: previousToDate.toISOString(),
  };
}

export function formatMonthLabel(isoString: string): string {
  try {
    const date = new Date(isoString);
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      year: '2-digit',
      timeZone: 'UTC',
    }).format(date);
  } catch {
    return isoString;
  }
}
