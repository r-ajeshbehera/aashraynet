import { useState } from 'react'
import { api, errorMessage } from '../../api/client'
import { useApi } from '../../api/useApi'
import type { CheckInResponse, Evacuee, Page, Shelter } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { ConfirmButton, Empty, ErrorBox, Loading, fmtDate } from '../../components/ui'

export default function EvacueeList({ shelter, onChanged }: { shelter: Shelter; onChanged: (s?: Shelter) => void }) {
  const { hasRole } = useAuth()
  const [q, setQ] = useState('')
  const [active, setActive] = useState(true)
  const [page, setPage] = useState(0)
  const [actionError, setActionError] = useState<string | null>(null)
  const { data, loading, error, reload } = useApi<Page<Evacuee>>(`/shelters/${shelter.id}/evacuees`,
    { active, q: q || undefined, page, size: 20, sort: 'checkedInAt,desc' })
  const canCheckout = hasRole('SHELTER_INCHARGE', 'DISTRICT_ADMIN', 'STATE_ADMIN')
  const showNeeds = !hasRole('VOLUNTEER')

  async function checkout(id: number) {
    setActionError(null)
    try {
      const res = await api.post<CheckInResponse>(`/evacuees/${id}/checkout`)
      onChanged(res.data.shelter)
      await reload()
    } catch (err) { setActionError(errorMessage(err)) }
  }

  return (
    <div className="card space-y-4">
      <div className="flex flex-wrap gap-3 items-end">
        <div className="flex-1 min-w-48"><label className="label" htmlFor="q">Search name</label>
          <input id="q" className="input" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} placeholder="Type a name…" /></div>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={active} onChange={(e) => { setActive(e.target.checked); setPage(0) }} /> Currently in shelter only</label>
      </div>
      {actionError && <ErrorBox message={actionError} />}
      {loading && !data && <Loading />}
      {error && <ErrorBox message={error} onRetry={reload} />}
      {data && data.content.length === 0 && <Empty text="No evacuees found." />}
      {data && data.content.length > 0 && (
        <div className="overflow-x-auto">
          <table className="table">
            <thead><tr><th>Name</th><th>Age</th><th>Gender</th><th>Village</th>{showNeeds && <th>Special need</th>}<th>Checked in</th><th>Checked out</th>{canCheckout && <th />}</tr></thead>
            <tbody>
              {data.content.map((e) => (
                <tr key={e.id}>
                  <td className="font-medium">{e.fullName}</td><td>{e.age}</td><td>{e.gender}</td><td>{e.village}</td>
                  {showNeeds && <td>{e.specialNeed !== 'NONE' ? <span className="rounded bg-purple-50 text-purple-800 px-2 py-0.5 text-xs">{e.specialNeed}</span> : '—'}</td>}
                  <td className="text-xs">{fmtDate(e.checkedInAt)}</td><td className="text-xs">{fmtDate(e.checkedOutAt)}</td>
                  {canCheckout && <td>{!e.checkedOutAt && (
                    <ConfirmButton label="Check out" className="btn-secondary" title="Check out evacuee?"
                      message={`${e.fullName} will be marked as having left the shelter, freeing one place.`} onConfirm={() => checkout(e.id)} />
                  )}</td>}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm">
          <span>{data.totalElements} evacuees · page {data.number + 1} of {data.totalPages}</span>
          <div className="flex gap-2">
            <button className="btn-secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
            <button className="btn-secondary" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</button>
          </div>
        </div>
      )}
    </div>
  )
}
