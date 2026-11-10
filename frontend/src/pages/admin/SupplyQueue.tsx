import { useState } from 'react'
import { useApi } from '../../api/useApi'
import type { Page, RequestStatus, SupplyRequest } from '../../api/types'
import { Empty, ErrorBox, Loading, PageHeader } from '../../components/ui'
import SupplyRequestCard from './SupplyRequestCard'

export default function SupplyQueue() {
  const [status, setStatus] = useState<RequestStatus | ''>('REQUESTED')
  const { data, loading, error, reload } = useApi<Page<SupplyRequest>>('/supply-requests', { status: status || undefined, size: 50 })
  return (
    <div className="space-y-4">
      <PageHeader title="Supply requests" subtitle="REQUESTED → APPROVED → DISPATCHED → RECEIVED"
        actions={<select aria-label="Status" className="input w-48" value={status} onChange={(e) => setStatus(e.target.value as RequestStatus | '')}>
          <option value="">All</option>{(['REQUESTED', 'APPROVED', 'DISPATCHED', 'RECEIVED', 'REJECTED'] as const).map((s) => <option key={s}>{s}</option>)}</select>} />
      {loading && !data && <Loading />}{error && <ErrorBox message={error} onRetry={reload} />}
      {data && data.content.length === 0 && <div className="card"><Empty text="No requests with this status." /></div>}
      <div className="grid lg:grid-cols-2 gap-4">{data?.content.map((r) => <RequestLoader key={r.id} id={r.id} onChanged={reload} />)}</div>
    </div>
  )
}

/** The list endpoint is lightweight; load the full request (lines + history) for each card. */
function RequestLoader({ id, onChanged }: { id: number; onChanged: () => void }) {
  const { data, reload } = useApi<SupplyRequest>(`/supply-requests/${id}`)
  if (!data) return <div className="card"><Loading /></div>
  return <SupplyRequestCard request={data} onChanged={() => { void reload(); onChanged() }} />
}
