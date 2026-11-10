import { useState, type ReactNode } from 'react'
import type { OccupancyStatus } from '../api/types'

const statusStyle: Record<OccupancyStatus, string> = {
  GREEN: 'bg-emerald-100 text-emerald-800 ring-emerald-600/20',
  YELLOW: 'bg-amber-100 text-amber-800 ring-amber-600/20',
  RED: 'bg-red-100 text-red-800 ring-red-600/20',
  FULL: 'bg-slate-800 text-white ring-slate-900/20',
}
const barStyle: Record<OccupancyStatus, string> = {
  GREEN: 'bg-emerald-500', YELLOW: 'bg-amber-500', RED: 'bg-red-500', FULL: 'bg-slate-800',
}
export const statusColor: Record<OccupancyStatus, string> = {
  GREEN: '#10b981', YELLOW: '#f59e0b', RED: '#ef4444', FULL: '#1e293b',
}

export function StatusBadge({ status }: { status: OccupancyStatus }) {
  return <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${statusStyle[status]}`}>{status}</span>
}

export function OccupancyBar({ occupancy, capacity, status }: { occupancy: number; capacity: number; status: OccupancyStatus }) {
  const pct = Math.min(100, Math.round((occupancy / capacity) * 1000) / 10)
  return (
    <div>
      <div className="flex justify-between text-xs text-slate-600 mb-1">
        <span>{occupancy} / {capacity} occupied</span><span>{pct}%</span>
      </div>
      <div className="h-3 w-full rounded-full bg-slate-200 overflow-hidden" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
        <div className={`h-full ${barStyle[status]} transition-all`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}

export function Loading({ text = 'Loading…' }: { text?: string }) {
  return <div className="flex items-center gap-3 text-slate-500 py-8 justify-center"><span className="h-4 w-4 animate-spin rounded-full border-2 border-navy-700 border-t-transparent" />{text}</div>
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800 flex items-center justify-between gap-4" role="alert">
      <span>{message}</span>
      {onRetry && <button className="btn-secondary" onClick={onRetry}>Retry</button>}
    </div>
  )
}

export function SuccessBox({ message }: { message: string }) {
  return <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800" role="status">{message}</div>
}

export function Empty({ text }: { text: string }) {
  return <div className="text-center text-slate-500 py-10 text-sm">{text}</div>
}

export function StatCard({ label, value, hint, tone = 'navy' }: { label: string; value: ReactNode; hint?: string; tone?: 'navy' | 'red' | 'green' | 'amber' }) {
  const tones = { navy: 'text-navy-800', red: 'text-red-600', green: 'text-emerald-600', amber: 'text-amber-600' }
  return (
    <div className="card">
      <div className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</div>
      <div className={`mt-1 text-3xl font-bold ${tones[tone]}`}>{value}</div>
      {hint && <div className="mt-1 text-xs text-slate-500">{hint}</div>}
    </div>
  )
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4 mb-6">
      <div>
        <h1 className="text-2xl font-bold text-navy-900">{title}</h1>
        {subtitle && <p className="text-sm text-slate-500 mt-1">{subtitle}</p>}
      </div>
      {actions && <div className="flex gap-2">{actions}</div>}
    </div>
  )
}

export function DemoBanner() {
  return (
    <div className="bg-amber-50 border-b border-amber-200 text-amber-900 text-xs text-center py-1.5 px-4">
      Portfolio project — all data is <strong>demonstration data</strong>. Not an official emergency-response service.
    </div>
  )
}

/** Accessible confirmation dialog used before irreversible actions. */
export function ConfirmButton({ label, title, message, onConfirm, className = 'btn-primary', disabled }: {
  label: string; title: string; message: string; onConfirm: () => Promise<void> | void; className?: string; disabled?: boolean
}) {
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  return (
    <>
      <button className={className} disabled={disabled} onClick={() => setOpen(true)}>{label}</button>
      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
          <div className="card max-w-md w-full">
            <h2 id="confirm-title" className="text-lg font-semibold text-navy-900">{title}</h2>
            <p className="text-sm text-slate-600 mt-2">{message}</p>
            <div className="mt-5 flex justify-end gap-2">
              <button className="btn-secondary" onClick={() => setOpen(false)} disabled={busy}>Cancel</button>
              <button className={className} disabled={busy} onClick={async () => {
                setBusy(true)
                try { await onConfirm() } finally { setBusy(false); setOpen(false) }
              }}>{busy ? 'Working…' : 'Confirm'}</button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

export function Facilities({ medical, ramp, generator }: { medical: boolean; ramp: boolean; generator?: boolean }) {
  const chip = (on: boolean, text: string) => (
    <span className={`rounded px-2 py-0.5 text-xs ${on ? 'bg-blue-50 text-blue-800' : 'bg-slate-100 text-slate-400 line-through'}`}>{text}</span>
  )
  return <div className="flex flex-wrap gap-1">{chip(medical, 'Medical')}{chip(ramp, 'Ramp')}{generator !== undefined && chip(generator, 'Generator')}</div>
}

export const fmtDate = (s: string | null | undefined) => (s ? new Date(s).toLocaleString() : '—')
