import { useState } from 'react'
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { useApi } from '../../api/useApi'
import type { DisasterEvent, District, DistrictDashboard as Dash, OccupancyStatus } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Empty, ErrorBox, Loading, PageHeader, StatCard, statusColor } from '../../components/ui'
import ShortageTable from './ShortageTable'

export default function DistrictDashboard() {
  const { user } = useAuth()
  const districts = useApi<District[]>('/districts')
  const events = useApi<DisasterEvent[]>('/events')
  const [districtId, setDistrictId] = useState<number | null>(user?.districtId ?? null)
  const [filters, setFilters] = useState({ event: '', from: '', to: '' })
  const id = districtId ?? districts.data?.[0]?.id ?? null
  const params = Object.fromEntries(Object.entries(filters).filter(([, v]) => v))
  const { data, loading, error, reload } = useApi<Dash>(id ? `/dashboard/district/${id}` : null, params)

  const statusData = data ? (Object.entries(data.sheltersByStatus) as [OccupancyStatus, number][]).map(([name, value]) => ({ name, value })) : []
  return (
    <div className="space-y-6">
      <PageHeader title={`District dashboard${data ? ` — ${data.districtName}` : ''}`} subtitle="All figures are calculated from live database records." />
      <div className="card flex flex-wrap gap-3 items-end">
        {user?.role === 'STATE_ADMIN' && (
          <div><label className="label" htmlFor="dist">District</label>
            <select id="dist" className="input" value={id ?? ''} onChange={(e) => setDistrictId(Number(e.target.value))}>
              {districts.data?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}</select></div>
        )}
        <div><label className="label" htmlFor="ev">Event</label>
          <select id="ev" className="input" value={filters.event} onChange={(e) => setFilters({ ...filters, event: e.target.value })}>
            <option value="">All events</option>{events.data?.map((ev) => <option key={ev.id} value={ev.id}>{ev.name}</option>)}</select></div>
        <div><label className="label" htmlFor="from">Checked in from</label><input id="from" type="date" className="input" value={filters.from} onChange={(e) => setFilters({ ...filters, from: e.target.value })} /></div>
        <div><label className="label" htmlFor="to">to</label><input id="to" type="date" className="input" value={filters.to} onChange={(e) => setFilters({ ...filters, to: e.target.value })} /></div>
      </div>
      {loading && !data && <Loading />}
      {error && <ErrorBox message={error} onRetry={reload} />}
      {data && <>
        <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
          <StatCard label="Evacuees in shelters" value={data.totalEvacuees} />
          <StatCard label="Active shelters" value={data.activeShelters} />
          <StatCard label="Free places" value={data.availableCapacity} hint={`of ${data.totalCapacity}`} tone="green" />
          <StatCard label="Pending requests" value={data.pendingSupplyRequests} tone="amber" />
          <StatCard label="Shortages" value={data.shortages.length} tone={data.shortages.length ? 'red' : 'green'} />
        </div>
        <div className="grid lg:grid-cols-3 gap-6">
          <div className="card lg:col-span-2">
            <h2 className="font-semibold text-navy-900 mb-3">Occupancy by shelter (%)</h2>
            {data.occupancyByShelter.length === 0 ? <Empty text="No shelters." /> : (
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={data.occupancyByShelter} margin={{ left: -10 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" tick={{ fontSize: 11 }} interval={0} angle={-15} textAnchor="end" height={50} />
                  <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} />
                  <Tooltip formatter={(v) => `${v}%`} />
                  <Bar dataKey="percent" radius={[4, 4, 0, 0]}>
                    {data.occupancyByShelter.map((s) => <Cell key={s.shelterId} fill={statusColor[s.status]} />)}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
          <div className="card">
            <h2 className="font-semibold text-navy-900 mb-3">Shelters by status</h2>
            <ResponsiveContainer width="100%" height={200}>
              <PieChart><Pie data={statusData.filter((d) => d.value > 0)} dataKey="value" nameKey="name" innerRadius={45} outerRadius={80}>
                {statusData.filter((d) => d.value > 0).map((d) => <Cell key={d.name} fill={statusColor[d.name]} />)}</Pie><Tooltip /></PieChart>
            </ResponsiveContainer>
            <ul className="text-sm grid grid-cols-2 gap-1 mt-2">{statusData.map((d) => (
              <li key={d.name} className="flex items-center gap-2"><span className="h-3 w-3 rounded-sm" style={{ background: statusColor[d.name] }} />{d.name}: {d.value}</li>))}</ul>
          </div>
        </div>
        <div className="grid lg:grid-cols-3 gap-6">
          {data.specialNeeds && (
            <div className="card">
              <h2 className="font-semibold text-navy-900 mb-3">Special needs (in shelters now)</h2>
              <ul className="text-sm divide-y divide-slate-100">{Object.entries(data.specialNeeds).filter(([k]) => k !== 'NONE').map(([k, v]) => (
                <li key={k} className="flex justify-between py-1.5"><span>{k}</span><span className="font-semibold">{v}</span></li>))}</ul>
            </div>
          )}
          <div className="card">
            <h2 className="font-semibold text-navy-900 mb-3">Supply requests by status</h2>
            <ul className="text-sm divide-y divide-slate-100">{Object.entries(data.supplyRequestsByStatus).map(([k, v]) => (
              <li key={k} className="flex justify-between py-1.5"><span>{k}</span><span className="font-semibold">{v}</span></li>))}</ul>
          </div>
          <div className="card lg:col-span-1">
            <h2 className="font-semibold text-navy-900 mb-3">Fullest shelters</h2>
            <ul className="text-sm divide-y divide-slate-100">{data.occupancyByShelter.slice(0, 6).map((s) => (
              <li key={s.shelterId} className="flex justify-between py-1.5"><span>{s.name}</span><span>{s.occupancy}/{s.capacity}</span></li>))}</ul>
          </div>
        </div>
        <div className="card"><h2 className="font-semibold text-navy-900 mb-3">Stock shortages (less than 1 day)</h2><ShortageTable rows={data.shortages} /></div>
      </>}
    </div>
  )
}
