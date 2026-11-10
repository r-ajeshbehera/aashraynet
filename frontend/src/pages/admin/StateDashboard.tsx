import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { useApi } from '../../api/useApi'
import type { StateDashboard as Dash } from '../../api/types'
import { ErrorBox, Loading, PageHeader, StatCard } from '../../components/ui'

export default function StateDashboard() {
  const { data, loading, error, reload } = useApi<Dash>('/dashboard/state')
  if (loading && !data) return <Loading />
  if (error) return <ErrorBox message={error} onRetry={reload} />
  if (!data) return null
  return (
    <div className="space-y-6">
      <PageHeader title="State dashboard" subtitle={data.activeEvents.length ? `Active: ${data.activeEvents.map((e) => e.name).join(', ')}` : 'No active disaster event'}
        actions={<button className="btn-secondary" onClick={reload}>Refresh</button>} />
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
        <StatCard label="Evacuees" value={data.totalEvacuees} />
        <StatCard label="Capacity" value={data.totalCapacity} />
        <StatCard label="Free places" value={data.availableCapacity} tone="green" />
        <StatCard label="Full shelters" value={data.sheltersByStatus.FULL} tone="red" />
        <StatCard label="Open shortages" value={data.openShortages} tone={data.openShortages ? 'red' : 'green'} />
      </div>
      <div className="card">
        <h2 className="font-semibold text-navy-900 mb-3">Occupancy vs capacity by district</h2>
        <ResponsiveContainer width="100%" height={260}>
          <BarChart data={data.districts}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="districtName" /><YAxis /><Tooltip /><Legend />
            <Bar dataKey="capacity" name="Capacity" fill="#cbd5e1" radius={[4, 4, 0, 0]} />
            <Bar dataKey="occupancy" name="Occupancy" fill="#1f4f86" radius={[4, 4, 0, 0]} /></BarChart>
        </ResponsiveContainer>
      </div>
      <div className="card overflow-x-auto">
        <h2 className="font-semibold text-navy-900 mb-3">District summary</h2>
        <table className="table">
          <thead><tr><th>District</th><th className="text-right">Shelters</th><th className="text-right">Evacuees</th><th className="text-right">Occupancy</th><th className="text-right">Full</th><th className="text-right">Shortages</th><th className="text-right">Pending requests</th></tr></thead>
          <tbody>{data.districts.map((d) => (
            <tr key={d.districtId}><td className="font-medium">{d.districtName}</td><td className="text-right">{d.shelters}</td><td className="text-right">{d.evacuees}</td>
              <td className="text-right">{d.occupancy}/{d.capacity}</td><td className="text-right">{d.fullShelters}</td>
              <td className={`text-right ${d.shortages ? 'text-red-700 font-semibold' : ''}`}>{d.shortages}</td><td className="text-right">{d.pendingRequests}</td></tr>))}
          </tbody>
        </table>
      </div>
      <div className="card text-sm grid sm:grid-cols-2 gap-4">
        <div><h3 className="font-semibold mb-2">Shelters by status</h3>{Object.entries(data.sheltersByStatus).map(([k, v]) => <div key={k} className="flex justify-between"><span>{k}</span><span>{v}</span></div>)}</div>
        <div><h3 className="font-semibold mb-2">Supply requests</h3>{Object.entries(data.supplyRequestsByStatus).map(([k, v]) => <div key={k} className="flex justify-between"><span>{k}</span><span>{v}</span></div>)}</div>
      </div>
    </div>
  )
}
