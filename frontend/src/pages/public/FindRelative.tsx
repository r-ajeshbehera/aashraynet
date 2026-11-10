import { useState, type FormEvent } from 'react'
import { api, errorMessage } from '../../api/client'
import type { PublicFind } from '../../api/types'
import { ErrorBox } from '../../components/ui'

export default function FindRelative() {
  const [name, setName] = useState('')
  const [village, setVillage] = useState('')
  const [result, setResult] = useState<PublicFind | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true); setError(null); setResult(null)
    try {
      const res = await api.get<PublicFind>('/public/find', { params: { name, village } })
      setResult(res.data)
    } catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-navy-900">Find a relative</h1>
        <p className="text-sm text-slate-600 mt-1">Enter the person's full name and village exactly as registered at the shelter.</p>
      </div>
      <form onSubmit={submit} className="card space-y-4">
        <div><label className="label" htmlFor="name">Full name</label>
          <input id="name" className="input" value={name} onChange={(e) => setName(e.target.value)} required maxLength={120} /></div>
        <div><label className="label" htmlFor="village">Village</label>
          <input id="village" className="input" value={village} onChange={(e) => setVillage(e.target.value)} required maxLength={120} /></div>
        <button className="btn-primary w-full" disabled={busy}>{busy ? 'Searching…' : 'Search'}</button>
      </form>
      {error && <ErrorBox message={error} />}
      {result && (
        <div className="card space-y-3">
          {result.matches.length === 0
            ? <p className="text-slate-700">No current check-in matches this name and village.</p>
            : <>
                <p className="font-medium text-slate-700">Possible matches:</p>
                <ul className="divide-y divide-slate-100">
                  {result.matches.map((m) => (
                    <li key={m.shelterName + m.district} className="py-2"><span className="font-semibold text-navy-900">{m.shelterName}</span> · {m.district} district</li>
                  ))}
                </ul>
              </>}
          <p className="text-xs text-slate-500 border-t pt-3">{result.notice}</p>
        </div>
      )}
      <div className="rounded-lg bg-blue-50 border border-blue-100 p-4 text-xs text-blue-900">
        <strong>Privacy notice:</strong> to protect evacuees, this page only shows the shelter name and district. Age, health
        and other personal details are never shown publicly. Searches are rate-limited.
      </div>
    </div>
  )
}
