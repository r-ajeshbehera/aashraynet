import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useApi } from '../../api/useApi'
import type { Role, Shelter } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { ErrorBox, Facilities, Loading, OccupancyBar, PageHeader, StatCard, StatusBadge, fmtDate } from '../../components/ui'
import CheckInPanel from './CheckInPanel'
import EvacueeList from './EvacueeList'
import FacilitiesForm from './FacilitiesForm'
import NearestPanel from './NearestPanel'
import StockPanel from './StockPanel'
import SupplyPanel from './SupplyPanel'

type Tab = 'checkin' | 'evacuees' | 'stock' | 'supplies' | 'nearest' | 'facilities'
const tabs: { id: Tab; label: string; roles: Role[] }[] = [
  { id: 'checkin', label: 'Check-in', roles: ['VOLUNTEER', 'SHELTER_INCHARGE'] },
  { id: 'evacuees', label: 'Evacuees', roles: ['VOLUNTEER', 'SHELTER_INCHARGE', 'DISTRICT_ADMIN', 'STATE_ADMIN'] },
  { id: 'stock', label: 'Stock', roles: ['SHELTER_INCHARGE', 'DISTRICT_ADMIN', 'STATE_ADMIN'] },
  { id: 'supplies', label: 'Supply requests', roles: ['SHELTER_INCHARGE', 'DISTRICT_ADMIN', 'STATE_ADMIN'] },
  { id: 'nearest', label: 'Nearest available', roles: ['VOLUNTEER', 'SHELTER_INCHARGE', 'DISTRICT_ADMIN', 'STATE_ADMIN'] },
  { id: 'facilities', label: 'Facilities', roles: ['SHELTER_INCHARGE', 'DISTRICT_ADMIN', 'STATE_ADMIN'] },
]

export default function ShelterDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const { data: shelter, loading, error, reload } = useApi<Shelter>(`/shelters/${id}`)
  const visible = tabs.filter((t) => user && t.roles.includes(user.role))
  const [tab, setTab] = useState<Tab>(visible[0]?.id ?? 'evacuees')

  if (loading && !shelter) return <Loading />
  if (error) return <ErrorBox message={error} onRetry={reload} />
  if (!shelter) return null
  const onChanged = (s?: Shelter) => { if (!s) void reload(); else void reload() }

  return (
    <div className="space-y-6">
      <PageHeader title={shelter.name} subtitle={`${shelter.village} · ${shelter.districtName} district · in-charge: ${shelter.inchargeName ?? 'not assigned'}`}
        actions={<StatusBadge status={shelter.status} />} />
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard label="Capacity" value={shelter.capacity} />
        <StatCard label="Occupancy" value={shelter.occupancy} />
        <StatCard label="Free places" value={shelter.freePlaces} tone={shelter.freePlaces === 0 ? 'red' : 'green'} />
        <StatCard label="Occupied" value={`${shelter.occupancyPercent}%`} tone={shelter.status === 'GREEN' ? 'green' : shelter.status === 'YELLOW' ? 'amber' : 'red'} />
      </div>
      <div className="card space-y-3">
        <OccupancyBar occupancy={shelter.occupancy} capacity={shelter.capacity} status={shelter.status} />
        <div className="flex flex-wrap justify-between gap-2 text-xs text-slate-500">
          <Facilities medical={shelter.hasMedical} ramp={shelter.hasRamp} generator={shelter.hasGenerator} />
          <span>Last updated {fmtDate(shelter.updatedAt)}</span>
        </div>
      </div>
      <div className="border-b border-slate-200 flex flex-wrap gap-1" role="tablist">
        {visible.map((t) => (
          <button key={t.id} role="tab" aria-selected={tab === t.id} onClick={() => setTab(t.id)}
            className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px ${tab === t.id ? 'border-navy-700 text-navy-900' : 'border-transparent text-slate-500 hover:text-slate-800'}`}>
            {t.label}
          </button>
        ))}
      </div>
      {tab === 'checkin' && <CheckInPanel shelter={shelter} onChanged={onChanged} />}
      {tab === 'evacuees' && <EvacueeList shelter={shelter} onChanged={onChanged} />}
      {tab === 'stock' && <StockPanel shelter={shelter} />}
      {tab === 'supplies' && <SupplyPanel shelter={shelter} />}
      {tab === 'nearest' && <NearestPanel shelter={shelter} />}
      {tab === 'facilities' && <FacilitiesForm shelter={shelter} onChanged={onChanged} />}
    </div>
  )
}
