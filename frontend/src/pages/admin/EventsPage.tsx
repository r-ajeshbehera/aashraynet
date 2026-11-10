import { useState, type FormEvent } from 'react'
import { api, errorMessage } from '../../api/client'
import { useApi } from '../../api/useApi'
import type { DisasterEvent } from '../../api/types'
import { ConfirmButton, Empty, ErrorBox, Loading, PageHeader, SuccessBox } from '../../components/ui'

export default function EventsPage() {
  const { data, loading, error, reload } = useApi<DisasterEvent[]>('/events')
  const [f, setF] = useState({ name: '', startDate: '', endDate: '' })
  const [msg, setMsg] = useState<string | null>(null)
  const [err, setErr] = useState<string | null>(null)

  async function create(e: FormEvent) {
    e.preventDefault(); setMsg(null); setErr(null)
    try { await api.post('/events', { ...f, endDate: f.endDate || null }); setMsg('Event created.'); setF({ name: '', startDate: '', endDate: '' }); await reload() }
    catch (ex) { setErr(errorMessage(ex)) }
  }
  async function toggle(ev: DisasterEvent) {
    try { await api.put(`/events/${ev.id}`, { name: ev.name, startDate: ev.startDate, endDate: ev.endDate, active: !ev.active }); await reload() }
    catch (ex) { setErr(errorMessage(ex)) }
  }

  return (
    <div className="space-y-4">
      <PageHeader title="Disaster events" subtitle="All operational records are linked to an event" />
      {msg && <SuccessBox message={msg} />}{err && <ErrorBox message={err} />}
      <form onSubmit={create} className="card grid md:grid-cols-4 gap-3 items-end">
        <div className="md:col-span-2"><label className="label" htmlFor="en">Name</label><input id="en" className="input" placeholder="Cyclone Dana 2026 (DEMO)" value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} required /></div>
        <div><label className="label" htmlFor="es">Start</label><input id="es" type="date" className="input" value={f.startDate} onChange={(e) => setF({ ...f, startDate: e.target.value })} required /></div>
        <div><label className="label" htmlFor="ee">End (optional)</label><input id="ee" type="date" className="input" value={f.endDate} onChange={(e) => setF({ ...f, endDate: e.target.value })} /></div>
        <div className="md:col-span-4"><button className="btn-primary">Create event</button></div>
      </form>
      <div className="card">
        {loading && <Loading />}{error && <ErrorBox message={error} onRetry={reload} />}
        {data && data.length === 0 && <Empty text="No events yet." />}
        {data && data.length > 0 && (
          <table className="table"><thead><tr><th>Event</th><th>Start</th><th>End</th><th>Status</th><th /></tr></thead>
            <tbody>{data.map((ev) => (
              <tr key={ev.id}><td className="font-medium">{ev.name}</td><td>{ev.startDate}</td><td>{ev.endDate ?? 'ongoing'}</td>
                <td>{ev.active ? <span className="text-emerald-700 font-semibold">Active</span> : <span className="text-slate-500">Inactive</span>}</td>
                <td><ConfirmButton label={ev.active ? 'Deactivate' : 'Activate'} className="btn-secondary" title={`${ev.active ? 'Deactivate' : 'Activate'} event?`}
                  message={ev.active ? 'No new check-ins or supply requests can be linked to an inactive event.' : 'The event becomes available for check-ins.'} onConfirm={() => toggle(ev)} /></td></tr>))}
            </tbody></table>
        )}
      </div>
    </div>
  )
}
