import { useState, type FormEvent } from 'react'
import { api, errorMessage } from '../../api/client'
import { useApi } from '../../api/useApi'
import type { Movement, MovementType, Page, Shelter, Stock } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Empty, ErrorBox, Loading, SuccessBox, fmtDate } from '../../components/ui'

const types: MovementType[] = ['INITIAL', 'CONSUMPTION', 'ADJUSTMENT_IN', 'ADJUSTMENT_OUT']

export default function StockPanel({ shelter }: { shelter: Shelter }) {
  const { hasRole } = useAuth()
  const stock = useApi<Stock[]>(`/shelters/${shelter.id}/stock`)
  const moves = useApi<Page<Movement>>(`/shelters/${shelter.id}/stock/movements`, { size: 10 })
  const [form, setForm] = useState({ itemId: '', type: 'CONSUMPTION' as MovementType, quantity: '', reason: '' })
  const [msg, setMsg] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const canAdjust = hasRole('SHELTER_INCHARGE', 'DISTRICT_ADMIN')
  const itemName = (id: number) => stock.data?.find((s) => s.itemId === id)?.itemName ?? `#${id}`

  async function submit(e: FormEvent) {
    e.preventDefault(); setMsg(null); setError(null)
    try {
      await api.post(`/shelters/${shelter.id}/stock/adjustments`, { itemId: Number(form.itemId), type: form.type, quantity: Number(form.quantity), reason: form.reason || undefined })
      setMsg('Stock updated.'); setForm((f) => ({ ...f, quantity: '', reason: '' }))
      await Promise.all([stock.reload(), moves.reload()])
    } catch (err) { setError(errorMessage(err)) }
  }

  return (
    <div className="grid lg:grid-cols-3 gap-6">
      <div className="card lg:col-span-2 space-y-3">
        <h2 className="font-semibold text-navy-900">Current stock</h2>
        {stock.loading && <Loading />}{stock.error && <ErrorBox message={stock.error} onRetry={stock.reload} />}
        {stock.data && (
          <div className="overflow-x-auto"><table className="table">
            <thead><tr><th>Item</th><th className="text-right">Quantity</th><th className="text-right">Need / person / day</th><th className="text-right">Days left</th></tr></thead>
            <tbody>{stock.data.map((s) => (
              <tr key={s.itemId} className={s.shortage ? 'bg-red-50' : ''}>
                <td className="font-medium">{s.itemName}</td>
                <td className="text-right">{s.quantity} {s.unit}</td>
                <td className="text-right">{Number(s.perPersonPerDay) > 0 ? `${s.perPersonPerDay} ${s.unit}` : '—'}</td>
                <td className={`text-right font-semibold ${s.shortage ? 'text-red-700' : ''}`}>{s.daysRemaining ?? '—'}{s.shortage && ' ⚠ shortage'}</td>
              </tr>))}
            </tbody></table></div>
        )}
        <h3 className="font-semibold text-navy-900 pt-4">Recent movements</h3>
        {moves.data && moves.data.content.length === 0 && <Empty text="No stock movements yet." />}
        {moves.data && moves.data.content.length > 0 && (
          <div className="overflow-x-auto"><table className="table">
            <thead><tr><th>When</th><th>Item</th><th>Type</th><th className="text-right">Change</th><th className="text-right">Balance</th><th>Reason</th></tr></thead>
            <tbody>{moves.data.content.map((m) => (
              <tr key={m.id}><td className="text-xs">{fmtDate(m.createdAt)}</td><td>{itemName(m.itemId)}</td><td className="text-xs">{m.type}</td>
                <td className={`text-right ${Number(m.quantityDelta) < 0 ? 'text-red-700' : 'text-emerald-700'}`}>{Number(m.quantityDelta) > 0 ? '+' : ''}{m.quantityDelta}</td>
                <td className="text-right">{m.balanceAfter}</td><td className="text-xs text-slate-500">{m.reason ?? ''}</td></tr>))}
            </tbody></table></div>
        )}
      </div>
      {canAdjust && (
        <form onSubmit={submit} className="card space-y-3 h-fit">
          <h2 className="font-semibold text-navy-900">Record stock change</h2>
          {msg && <SuccessBox message={msg} />}{error && <ErrorBox message={error} />}
          <div><label className="label" htmlFor="it">Item</label>
            <select id="it" className="input" value={form.itemId} onChange={(e) => setForm({ ...form, itemId: e.target.value })} required>
              <option value="">Choose…</option>{stock.data?.map((s) => <option key={s.itemId} value={s.itemId}>{s.itemName}</option>)}
            </select></div>
          <div><label className="label" htmlFor="ty">Type</label>
            <select id="ty" className="input" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value as MovementType })}>
              {types.map((t) => <option key={t}>{t}</option>)}</select></div>
          <div><label className="label" htmlFor="qt">Quantity</label>
            <input id="qt" type="number" step="0.01" min="0.01" className="input" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} required /></div>
          <div><label className="label" htmlFor="rs">Reason</label>
            <input id="rs" className="input" value={form.reason} maxLength={255} onChange={(e) => setForm({ ...form, reason: e.target.value })} /></div>
          <button className="btn-primary w-full">Save</button>
          <p className="text-xs text-slate-500">Supply deliveries are recorded automatically when a dispatched request is received.</p>
        </form>
      )}
    </div>
  )
}
