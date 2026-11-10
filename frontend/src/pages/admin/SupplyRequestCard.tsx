import { useState } from 'react'
import { api, errorMessage } from '../../api/client'
import type { RequestStatus, SupplyRequest } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { ConfirmButton, ErrorBox, fmtDate } from '../../components/ui'

const statusStyle: Record<RequestStatus, string> = {
  REQUESTED: 'bg-blue-100 text-blue-800', APPROVED: 'bg-indigo-100 text-indigo-800', REJECTED: 'bg-slate-200 text-slate-700',
  DISPATCHED: 'bg-amber-100 text-amber-800', RECEIVED: 'bg-emerald-100 text-emerald-800',
}
const steps: RequestStatus[] = ['REQUESTED', 'APPROVED', 'DISPATCHED', 'RECEIVED']

export default function SupplyRequestCard({ request, onChanged }: { request: SupplyRequest; onChanged: () => void }) {
  const { hasRole } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const [reason, setReason] = useState('')
  const act = async (action: string, body?: object) => {
    setError(null)
    try { await api.post(`/supply-requests/${request.id}/${action}`, body); onChanged() } catch (err) { setError(errorMessage(err)) }
  }
  const isAdmin = hasRole('DISTRICT_ADMIN')
  const isIncharge = hasRole('SHELTER_INCHARGE')
  const stepIndex = steps.indexOf(request.status)

  return (
    <div className="card space-y-3">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <div className="font-semibold text-navy-900">Request #{request.id} · {request.shelterName}</div>
          <div className="text-xs text-slate-500">Created {fmtDate(request.createdAt)}{request.requestedBy ? ` by ${request.requestedBy}` : ''}</div>
        </div>
        <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${statusStyle[request.status]}`}>{request.status}</span>
      </div>
      {request.status !== 'REJECTED' && (
        <ol className="flex text-xs gap-1">
          {steps.map((s, i) => <li key={s} className={`flex-1 rounded px-2 py-1 text-center ${i <= stepIndex ? 'bg-navy-800 text-white' : 'bg-slate-100 text-slate-500'}`}>{s}</li>)}
        </ol>
      )}
      {request.lines.length > 0 && (
        <ul className="text-sm text-slate-700 list-disc pl-5">{request.lines.map((l) => <li key={l.itemId}>{l.itemName}: {l.quantity} {l.unit}</li>)}</ul>
      )}
      {request.note && <p className="text-sm text-slate-600">Note: {request.note}</p>}
      {request.rejectionReason && <p className="text-sm text-red-700">Rejected: {request.rejectionReason}</p>}
      {error && <ErrorBox message={error} />}
      <div className="flex flex-wrap gap-2 items-center">
        {isAdmin && request.status === 'REQUESTED' && <>
          <ConfirmButton label="Approve" className="btn-success" title="Approve request?" message="The request will be approved for dispatch." onConfirm={() => act('approve')} />
          <input aria-label="Rejection reason" className="input w-56" placeholder="Reason to reject" value={reason} onChange={(e) => setReason(e.target.value)} />
          <ConfirmButton label="Reject" className="btn-danger" disabled={!reason.trim()} title="Reject request?" message="The shelter will see the reason." onConfirm={() => act('reject', { reason })} />
        </>}
        {isAdmin && request.status === 'APPROVED' &&
          <ConfirmButton label="Mark dispatched" title="Mark as dispatched?" message="Confirm the supplies have left the store." onConfirm={() => act('dispatch')} />}
        {isIncharge && request.status === 'DISPATCHED' &&
          <ConfirmButton label="Confirm receipt" className="btn-success" title="Confirm supplies received?" message="Stock will be increased by the requested quantities. This can only be done once." onConfirm={() => act('receive')} />}
      </div>
    </div>
  )
}
