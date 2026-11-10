import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api, errorMessage } from '../../api/client'
import { useApi } from '../../api/useApi'
import type { District, OccupancyStatus, Shelter, User } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Empty, ErrorBox, Facilities, Loading, PageHeader, StatusBadge, SuccessBox } from '../../components/ui'

function exportCsv(rows: Shelter[]) {
  const header = ['Shelter', 'District', 'Village', 'Capacity', 'Occupancy', 'Free', 'Percent', 'Status', 'Medical', 'Ramp', 'Generator']
  const lines = rows.map((s) => [s.name, s.districtName, s.village, s.capacity, s.occupancy, s.freePlaces, s.occupancyPercent, s.status, s.hasMedical, s.hasRamp, s.hasGenerator]
    .map((v) => `"${String(v).replace(/"/g, '""')}"`).join(','))
  const blob = new Blob([[header.join(','), ...lines].join('\n')], { type: 'text/csv' })
  const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'shelters-report.csv'; a.click(); URL.revokeObjectURL(a.href)
}

export default function SheltersPage() {
  const { user, hasRole } = useAuth()
  const [status, setStatus] = useState<OccupancyStatus | ''>('')
  const [district, setDistrict] = useState('')
  const districts = useApi<District[]>('/districts')
  const { data, loading, error, reload } = useApi<Shelter[]>('/shelters', { status: status || undefined, district: district || undefined })
  const isAdmin = hasRole('DISTRICT_ADMIN', 'STATE_ADMIN')
  const incharges = useApi<{ content: User[] }>(isAdmin ? '/users' : null, { role: 'SHELTER_INCHARGE', size: 100 })
  const [showForm, setShowForm] = useState(false)
  const empty = { districtId: String(user?.districtId ?? ''), name: '', village: '', latitude: '', longitude: '', capacity: '', hasMedical: false, hasRamp: false, hasGenerator: false }
  const [f, setF] = useState(empty)
  const [msg, setMsg] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  async function create(e: FormEvent) {
    e.preventDefault(); setMsg(null); setFormError(null)
    try {
      await api.post('/shelters', { ...f, districtId: Number(f.districtId || districts.data?.[0]?.id), latitude: Number(f.latitude), longitude: Number(f.longitude), capacity: Number(f.capacity) })
      setMsg('Shelter created.'); setF(empty); setShowForm(false); await reload()
    } catch (err) { setFormError(errorMessage(err)) }
  }
  async function assign(shelterId: number, userId: string) {
    try { await api.patch(`/shelters/${shelterId}/incharge`, { userId: userId ? Number(userId) : null }); await reload() } catch (err) { setFormError(errorMessage(err)) }
  }

  return (
    <div className="space-y-4">
      <PageHeader title="Shelters" subtitle="Live occupancy from check-ins and check-outs"
        actions={<>
          {data && data.length > 0 && <button className="btn-secondary" onClick={() => exportCsv(data)}>Export CSV</button>}
          {isAdmin && <button className="btn-primary" onClick={() => setShowForm(!showForm)}>{showForm ? 'Close' : '+ New shelter'}</button>}
        </>} />
      {msg && <SuccessBox message={msg} />}
      {formError && <ErrorBox message={formError} />}
      {showForm && (
        <form onSubmit={create} className="card grid md:grid-cols-3 gap-3">
          {user?.role === 'STATE_ADMIN' && (
            <div><label className="label" htmlFor="sd">District</label>
              <select id="sd" className="input" value={f.districtId} onChange={(e) => setF({ ...f, districtId: e.target.value })} required>
                <option value="">Choose…</option>{districts.data?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}</select></div>
          )}
          <div><label className="label" htmlFor="sn">Name</label><input id="sn" className="input" value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} required /></div>
          <div><label className="label" htmlFor="sv">Village</label><input id="sv" className="input" value={f.village} onChange={(e) => setF({ ...f, village: e.target.value })} required /></div>
          <div><label className="label" htmlFor="sla">Latitude</label><input id="sla" type="number" step="0.000001" min={-90} max={90} className="input" value={f.latitude} onChange={(e) => setF({ ...f, latitude: e.target.value })} required /></div>
          <div><label className="label" htmlFor="slo">Longitude</label><input id="slo" type="number" step="0.000001" min={-180} max={180} className="input" value={f.longitude} onChange={(e) => setF({ ...f, longitude: e.target.value })} required /></div>
          <div><label className="label" htmlFor="sc">Capacity</label><input id="sc" type="number" min={1} className="input" value={f.capacity} onChange={(e) => setF({ ...f, capacity: e.target.value })} required /></div>
          <div className="flex items-end gap-4 text-sm">
            {(['hasMedical', 'hasRamp', 'hasGenerator'] as const).map((k) => (
              <label key={k} className="flex items-center gap-1"><input type="checkbox" checked={f[k]} onChange={(e) => setF({ ...f, [k]: e.target.checked })} />{k.replace('has', '')}</label>))}
          </div>
          <div className="md:col-span-3"><button className="btn-primary">Create shelter</button></div>
        </form>
      )}
      <div className="card space-y-4">
        <div className="flex flex-wrap gap-3">
          {user?.role === 'STATE_ADMIN' && (
            <select aria-label="District filter" className="input w-48" value={district} onChange={(e) => setDistrict(e.target.value)}>
              <option value="">All districts</option>{districts.data?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}</select>
          )}
          <select aria-label="Status filter" className="input w-48" value={status} onChange={(e) => setStatus(e.target.value as OccupancyStatus | '')}>
            <option value="">All statuses</option>{(['GREEN', 'YELLOW', 'RED', 'FULL'] as const).map((s) => <option key={s}>{s}</option>)}</select>
        </div>
        {loading && !data && <Loading />}{error && <ErrorBox message={error} onRetry={reload} />}
        {data && data.length === 0 && <Empty text="No shelters match these filters." />}
        {data && data.length > 0 && (
          <div className="overflow-x-auto"><table className="table">
            <thead><tr><th>Shelter</th><th>District</th><th className="text-right">Occupancy</th><th className="text-right">Free</th><th>Status</th><th>Facilities</th>{isAdmin && <th>In-charge</th>}</tr></thead>
            <tbody>{data.map((s) => (
              <tr key={s.id}>
                <td><Link to={`/shelters/${s.id}`} className="font-medium text-navy-700 hover:underline">{s.name}</Link><div className="text-xs text-slate-500">{s.village}</div></td>
                <td>{s.districtName}</td>
                <td className="text-right">{s.occupancy}/{s.capacity} <span className="text-xs text-slate-500">({s.occupancyPercent}%)</span></td>
                <td className="text-right">{s.freePlaces}</td><td><StatusBadge status={s.status} /></td>
                <td><Facilities medical={s.hasMedical} ramp={s.hasRamp} generator={s.hasGenerator} /></td>
                {isAdmin && <td>
                  <select aria-label={`In-charge for ${s.name}`} className="input text-xs py-1" value={s.inchargeId ?? ''} onChange={(e) => assign(s.id, e.target.value)}>
                    <option value="">— none —</option>
                    {incharges.data?.content.filter((u) => u.districtId === s.districtId && u.active).map((u) => <option key={u.id} value={u.id}>{u.fullName}</option>)}
                  </select></td>}
              </tr>))}
            </tbody></table></div>
        )}
      </div>
    </div>
  )
}
