import { useState, type FormEvent } from 'react'
import { api, errorMessage } from '../../api/client'
import { useApi } from '../../api/useApi'
import type { DisasterEvent, Page, Shelter, SupplyItem, SupplyRequest } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Empty, ErrorBox, Loading, SuccessBox } from '../../components/ui'
import SupplyRequestCard from '../admin/SupplyRequestCard'

export default function SupplyPanel({ shelter }: { shelter: Shelter }) {
  const { hasRole } = useAuth()
  const isIncharge = hasRole('SHELTER_INCHARGE')
  const items = useApi<SupplyItem[]>('/supply-items')
  const events = useApi<DisasterEvent[]>('/events', { active: true })
  const list = useApi<Page<SupplyRequest>>('/supply-requests', { shelter: shelter.id, size: 20 })
  const [lines, setLines] = useState<{ itemId: string; quantity: string }[]>([{ itemId: '', quantity: '' }])
  const [note, setNote] = useState('')
  const [msg, setMsg] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault(); setMsg(null); setError(null)
    try {
      await api.post(`/shelters/${shelter.id}/supply-requests`, {
        eventId: events.data?.[0]?.id, note: note || undefined,
        lines: lines.filter((l) => l.itemId).map((l) => ({ itemId: Number(l.itemId), quantity: Number(l.quantity) })),
      })
      setMsg('Request sent to the district administrator.'); setLines([{ itemId: '', quantity: '' }]); setNote('')
      await list.reload()
    } catch (err) { setError(errorMessage(err)) }
  }

  return (
    <div className="grid lg:grid-cols-3 gap-6">
      <div className="lg:col-span-2 space-y-3">
        {list.loading && <Loading />}{list.error && <ErrorBox message={list.error} onRetry={list.reload} />}
        {list.data && list.data.content.length === 0 && <div className="card"><Empty text="No supply requests yet." /></div>}
        {list.data?.content.map((r) => <SupplyRequestCard key={r.id} request={r} onChanged={list.reload} />)}
      </div>
      {isIncharge && (
        <form onSubmit={submit} className="card space-y-3 h-fit">
          <h2 className="font-semibold text-navy-900">New supply request</h2>
          {msg && <SuccessBox message={msg} />}{error && <ErrorBox message={error} />}
          {lines.map((l, i) => (
            <div key={i} className="flex gap-2">
              <select aria-label="Item" className="input" value={l.itemId} onChange={(e) => setLines(lines.map((x, j) => j === i ? { ...x, itemId: e.target.value } : x))} required={i === 0}>
                <option value="">Item…</option>{items.data?.map((it) => <option key={it.id} value={it.id}>{it.name} ({it.unit})</option>)}
              </select>
              <input aria-label="Quantity" type="number" min="0.01" step="0.01" className="input w-28" value={l.quantity} placeholder="Qty"
                onChange={(e) => setLines(lines.map((x, j) => j === i ? { ...x, quantity: e.target.value } : x))} required={!!l.itemId} />
            </div>
          ))}
          <button type="button" className="btn-secondary w-full" onClick={() => setLines([...lines, { itemId: '', quantity: '' }])}>+ Add item</button>
          <div><label className="label" htmlFor="note">Note</label><input id="note" className="input" maxLength={255} value={note} onChange={(e) => setNote(e.target.value)} /></div>
          <button className="btn-primary w-full" disabled={!events.data?.length}>Send request</button>
        </form>
      )}
    </div>
  )
}
