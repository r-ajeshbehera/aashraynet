import { Link, Navigate } from 'react-router-dom'
import { useApi } from '../../api/useApi'
import type { Shelter } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Empty, ErrorBox, Facilities, Loading, OccupancyBar, PageHeader, StatusBadge } from '../../components/ui'

/** In-charge: their shelter(s). Volunteer: every active shelter in their district (they can check in anywhere there). */
export default function MyShelter() {
  const { user } = useAuth()
  const { data, loading, error, reload } = useApi<Shelter[]>('/shelters')
  if (loading) return <Loading />
  if (error) return <ErrorBox message={error} onRetry={reload} />
  if (user?.role === 'SHELTER_INCHARGE' && data?.length === 1) return <Navigate to={`/shelters/${data[0].id}`} replace />
  return (
    <div>
      <PageHeader title={user?.role === 'VOLUNTEER' ? 'Shelters in my district' : 'My shelters'}
        subtitle="Choose a shelter to register evacuees" />
      {data?.length === 0 && <Empty text="No shelter is assigned to you yet. Ask your district administrator." />}
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {data?.map((s) => (
          <Link key={s.id} to={`/shelters/${s.id}`} className="card hover:border-navy-700 transition space-y-3">
            <div className="flex justify-between gap-2">
              <div><div className="font-semibold text-navy-900">{s.name}</div><div className="text-xs text-slate-500">{s.village}</div></div>
              <StatusBadge status={s.status} />
            </div>
            <OccupancyBar occupancy={s.occupancy} capacity={s.capacity} status={s.status} />
            <Facilities medical={s.hasMedical} ramp={s.hasRamp} generator={s.hasGenerator} />
          </Link>
        ))}
      </div>
    </div>
  )
}
