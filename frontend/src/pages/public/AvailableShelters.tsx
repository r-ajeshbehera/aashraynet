import { useState } from 'react'
import { useApi } from '../../api/useApi'
import type { District, PublicShelters } from '../../api/types'
import { Empty, ErrorBox, Facilities, Loading, StatusBadge, fmtDate } from '../../components/ui'

export default function AvailableShelters() {
  const [district, setDistrict] = useState<string>('')
  const districts = useApi<District[]>('/districts')
  const { data, loading, error, reload } = useApi<PublicShelters>('/public/shelters/available', district ? { district } : {})

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-navy-900">Shelters with free places</h1>
          {data && <p className="text-xs text-slate-500 mt-1">Data generated {fmtDate(data.generatedAt)}</p>}
        </div>
        <div><label className="label" htmlFor="d">District</label>
          <select id="d" className="input" value={district} onChange={(e) => setDistrict(e.target.value)}>
            <option value="">All districts</option>
            {districts.data?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
          </select></div>
      </div>
      {data && <div className="rounded-lg bg-amber-50 border border-amber-200 p-3 text-xs text-amber-900">{data.dataNotice}</div>}
      {loading && <Loading />}
      {error && <ErrorBox message={error} onRetry={reload} />}
      {data && data.shelters.length === 0 && <Empty text="No shelters with free places in this district." />}
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {data?.shelters.map((s) => (
          <div key={s.shelterName} className="card space-y-2">
            <div className="flex items-start justify-between gap-2">
              <div><div className="font-semibold text-navy-900">{s.shelterName}</div><div className="text-xs text-slate-500">{s.village}, {s.district}</div></div>
              <StatusBadge status={s.status} />
            </div>
            <div className="text-2xl font-bold text-navy-800">{s.freePlaces} <span className="text-sm font-normal text-slate-500">free places</span></div>
            <Facilities medical={s.hasMedical} ramp={s.hasRamp} />
            <div className="text-xs text-slate-400">Last updated {fmtDate(s.lastUpdated)}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
