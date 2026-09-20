import React from 'react';
import {
  ScatterChart,
  Scatter,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
} from 'recharts';
import { RepositoryFileResponse } from '../../types/file';

interface BaselineCompositeChartProps {
  files: RepositoryFileResponse[];
  onSelectFile: (filePath: string) => void;
  selectedFilePath?: string | null;
}

interface ScatterPoint {
  x: number;
  y: number;
  file: RepositoryFileResponse;
}

export const BaselineCompositeChart: React.FC<BaselineCompositeChartProps> = ({
  files,
  onSelectFile,
}) => {
  if (!files || files.length === 0) {
    return null;
  }

  const data: ScatterPoint[] = files.map((file) => ({
    x: Number((file.baselineScore ?? 0).toFixed(2)),
    y: Number((file.compositeScore ?? 0).toFixed(2)),
    file,
  }));

  const CustomTooltip = ({ active, payload }: { active?: boolean; payload?: { payload: ScatterPoint }[] }) => {
    if (active && payload && payload.length) {
      const point = payload[0].payload;
      const f = point.file;
      const baseline = f.baselineScore ?? 0;
      const composite = f.compositeScore ?? 0;
      const delta = composite - baseline;

      return (
        <div className="rounded-lg border border-slate-700 bg-slate-900/95 p-3 shadow-xl backdrop-blur text-xs max-w-xs space-y-1.5 font-sans">
          <div className="font-mono text-slate-100 font-semibold truncate border-b border-slate-800 pb-1">
            {f.filePath}
          </div>
          <div className="grid grid-cols-2 gap-x-2 text-[11px]">
            <span className="text-slate-400">Baseline (Freq):</span>
            <span className="font-mono text-slate-200 font-semibold text-right">{baseline.toFixed(2)}</span>
            <span className="text-slate-400">Composite Score:</span>
            <span className="font-mono text-blue-400 font-semibold text-right">{composite.toFixed(2)}</span>
            <span className="text-slate-400">Difference (Δ):</span>
            <span className="font-mono text-slate-300 font-semibold text-right">
              {delta >= 0 ? `+${delta.toFixed(2)}` : delta.toFixed(2)}
            </span>
          </div>
          <div className="pt-1 border-t border-slate-800/80 text-[10px] text-slate-400 grid grid-cols-2 gap-x-1">
            <span>Churn: {(f.churnScore ?? 0).toFixed(2)}</span>
            <span>Recency: {(f.recencyScore ?? 0).toFixed(2)}</span>
            <span>Ownership: {(f.ownershipConcentrationScore ?? 0).toFixed(2)}</span>
            <span>Revisions: {(f.revisionFrequencyScore ?? 0).toFixed(2)}</span>
          </div>
          <div className="text-[9px] text-blue-400/80 italic pt-1">Click to inspect file details</div>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-6 shadow-sm backdrop-blur space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b border-slate-800 pb-3">
        <div>
          <h3 className="text-sm font-bold uppercase tracking-wider text-slate-200">
            Baseline vs Multidimensional Signal Distribution
          </h3>
          <p className="text-xs text-slate-400">
            Scatter plot comparing single-dimensional baseline (X) with multidimensional composite signal (Y).
          </p>
        </div>
        <span className="mt-2 sm:mt-0 text-[11px] font-mono text-slate-400">
          Sample: {files.length} files
        </span>
      </div>

      <div className="h-72 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <ScatterChart margin={{ top: 20, right: 20, bottom: 20, left: 10 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#334155" opacity={0.5} />
            <XAxis
              type="number"
              dataKey="x"
              name="Baseline Score"
              domain={[0, 1]}
              tick={{ fill: '#94a3b8', fontSize: 11 }}
              tickCount={6}
              unit=""
              label={{
                value: 'Baseline Score (Revision Frequency Only) →',
                position: 'insideBottom',
                offset: -12,
                fill: '#94a3b8',
                fontSize: 11,
              }}
            />
            <YAxis
              type="number"
              dataKey="y"
              name="Composite Score"
              domain={[0, 1]}
              tick={{ fill: '#94a3b8', fontSize: 11 }}
              tickCount={6}
              unit=""
              label={{
                value: '↑ Multidimensional Composite Score',
                angle: -90,
                position: 'insideLeft',
                offset: 5,
                fill: '#94a3b8',
                fontSize: 11,
              }}
            />
            <Tooltip content={<CustomTooltip />} />
            {/* y = x diagonal reference line */}
            <ReferenceLine
              segment={[
                { x: 0, y: 0 },
                { x: 1, y: 1 },
              ]}
              stroke="#64748b"
              strokeDasharray="4 4"
              ifOverflow="extendDomain"
            />
            <Scatter
              name="Files"
              data={data}
              fill="#3b82f6"
              onClick={(entry: any) => {
                if (entry?.payload?.file?.filePath) {
                  onSelectFile(entry.payload.file.filePath);
                } else if (entry?.file?.filePath) {
                  onSelectFile(entry.file.filePath);
                }
              }}
              className="cursor-pointer"
            />
          </ScatterChart>
        </ResponsiveContainer>
      </div>

      <div className="flex flex-wrap items-center justify-between text-[11px] text-slate-400 pt-2 border-t border-slate-800/80">
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-1.5">
            <span className="inline-block h-2 w-2 rounded-full bg-blue-500" />
            <span>Repository File</span>
          </div>
          <div className="flex items-center space-x-1.5">
            <span className="inline-block h-0.5 w-4 bg-slate-500 border-t border-dashed border-slate-400" />
            <span>Diagonal Line (Composite = Baseline)</span>
          </div>
        </div>
        <div>
          <span>Points above diagonal: higher multidimensional score than frequency alone.</span>
        </div>
      </div>
    </div>
  );
};
