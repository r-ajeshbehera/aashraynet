import { useState } from 'react'
import { useApi } from '../../api/useApi'
import type { NearestShelter, Shelter } from '../../api/types'
import { ErrorBox, Loading } from '../../components/ui'
import NearestTable from './NearestTable'

export default function NearestPanel({ shelter }: { shelter: Shelter }) {
  const [people, setPeople] = useState(1)
  const [count, setCount] = useState(3)
  const { data, loading, error, reload } = useApi<NearestShelter[]>(`/shelters/${shelter.id}/nearest-available`, { people, count })
  return (
    <div className="card space-y-4">
      <div className="flex flex-wrap gap-3 items-end">
        <div><label className="label" htmlFor="p">People needing places</label>
          <input id="p" type="number" min={1} max={500} className="input w-40" value={people} onChange={(e) => setPeople(Math.max(1, Number(e.target.value)))} /></div>
        <div><label className="label" htmlFor="c">Results</label>
          <select id="c" className="input w-28" value={count} onChange={(e) => setCount(Number(e.target.value))}>{[3, 5, 10].map((n) => <option key={n}>{n}</option>)}</select></div>
        <p className="text-xs text-slate-500 flex-1">Distance is straight-line (Haversine). Shelters without enough free places are left out. This is a recommendation only.</p>
      </div>
      {loading && <Loading />}
      {error && <ErrorBox message={error} onRetry={reload} />}
      {data && <NearestTable rows={data} />}
    </div>
  )
}
