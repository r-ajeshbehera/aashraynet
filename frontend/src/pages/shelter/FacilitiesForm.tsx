import { useState } from 'react'
import { api, errorMessage } from '../../api/client'
import type { Shelter } from '../../api/types'
import { ErrorBox, SuccessBox } from '../../components/ui'

export default function FacilitiesForm({ shelter, onChanged }: { shelter: Shelter; onChanged: (s?: Shelter) => void }) {
  const [f, setF] = useState({ hasMedical: shelter.hasMedical, hasRamp: shelter.hasRamp, hasGenerator: shelter.hasGenerator })
  const [msg, setMsg] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const toggle = (k: keyof typeof f) => setF((x) => ({ ...x, [k]: !x[k] }))

  async function save() {
    setBusy(true); setMsg(null); setError(null)
    try {
      const res = await api.patch<Shelter>(`/shelters/${shelter.id}/facilities`, f)
      setMsg('Facilities saved.'); onChanged(res.data)
    } catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }

  return (
    <div className="card max-w-md space-y-4">
      {msg && <SuccessBox message={msg} />}{error && <ErrorBox message={error} />}
      {([['hasMedical', 'Medical facility'], ['hasRamp', 'Ramp / wheelchair access'], ['hasGenerator', 'Generator']] as const).map(([k, label]) => (
        <label key={k} className="flex items-center gap-3 text-sm"><input type="checkbox" checked={f[k]} onChange={() => toggle(k)} /> {label}</label>
      ))}
      <button className="btn-primary" onClick={save} disabled={busy}>{busy ? 'Saving…' : 'Save facilities'}</button>
    </div>
  )
}
