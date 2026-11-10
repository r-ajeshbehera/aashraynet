import { useState, type FormEvent } from 'react'
import { api, apiError, errorMessage } from '../../api/client'
import { useApi } from '../../api/useApi'
import type { CheckInResponse, DisasterEvent, Gender, NearestShelter, Shelter, SpecialNeed } from '../../api/types'
import { ErrorBox, SuccessBox } from '../../components/ui'
import NearestTable from './NearestTable'

const needs: SpecialNeed[] = ['NONE', 'PREGNANT', 'ELDERLY', 'DISABLED', 'INFANT', 'MEDICAL']

export default function CheckInPanel({ shelter, onChanged }: { shelter: Shelter; onChanged: (s?: Shelter) => void }) {
  const events = useApi<DisasterEvent[]>('/events', { active: true })
  const [form, setForm] = useState({ eventId: '', fullName: '', age: '', gender: 'FEMALE' as Gender, village: shelter.village, specialNeed: 'NONE' as SpecialNeed })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [alternatives, setAlternatives] = useState<NearestShelter[] | null>(null)
  const eventId = form.eventId || String(events.data?.[0]?.id ?? '')
  const set = (k: keyof typeof form, v: string) => setForm((f) => ({ ...f, [k]: v }))

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true); setError(null); setSuccess(null); setAlternatives(null)
    try {
      const res = await api.post<CheckInResponse>(`/shelters/${shelter.id}/evacuees`, {
        eventId: Number(eventId), fullName: form.fullName, age: Number(form.age), gender: form.gender,
        village: form.village, specialNeed: form.specialNeed,
      })
      setSuccess(`${res.data.evacuee.fullName} checked in. ${res.data.shelter.freePlaces} place(s) left.`)
      setForm((f) => ({ ...f, fullName: '', age: '', specialNeed: 'NONE' }))
      onChanged(res.data.shelter)
    } catch (err) {
      const body = apiError(err)
      if (body?.code === 'SHELTER_FULL') setAlternatives(body.details?.alternatives ?? [])
      setError(errorMessage(err))
    } finally { setBusy(false) }
  }

  if (events.data && events.data.length === 0) return <ErrorBox message="There is no active disaster event. Check-in is disabled." />
  return (
    <div className="grid lg:grid-cols-2 gap-6">
      <form onSubmit={submit} className="card space-y-4">
        <h2 className="font-semibold text-navy-900">Register evacuee</h2>
        {success && <SuccessBox message={success} />}
        {error && <ErrorBox message={error} />}
        <div><label className="label" htmlFor="ev">Disaster event</label>
          <select id="ev" className="input" value={eventId} onChange={(e) => set('eventId', e.target.value)} required>
            {events.data?.map((ev) => <option key={ev.id} value={ev.id}>{ev.name}</option>)}
          </select></div>
        <div><label className="label" htmlFor="fn">Full name</label>
          <input id="fn" className="input" value={form.fullName} onChange={(e) => set('fullName', e.target.value)} required maxLength={120} /></div>
        <div className="grid grid-cols-2 gap-3">
          <div><label className="label" htmlFor="age">Age</label>
            <input id="age" type="number" min={0} max={120} className="input" value={form.age} onChange={(e) => set('age', e.target.value)} required /></div>
          <div><label className="label" htmlFor="g">Gender</label>
            <select id="g" className="input" value={form.gender} onChange={(e) => set('gender', e.target.value)}>
              <option value="FEMALE">Female</option><option value="MALE">Male</option><option value="OTHER">Other</option>
            </select></div>
        </div>
        <div><label className="label" htmlFor="v">Village</label>
          <input id="v" className="input" value={form.village} onChange={(e) => set('village', e.target.value)} required maxLength={120} /></div>
        <div><label className="label" htmlFor="sn">Special need</label>
          <select id="sn" className="input" value={form.specialNeed} onChange={(e) => set('specialNeed', e.target.value)}>
            {needs.map((n) => <option key={n} value={n}>{n}</option>)}
          </select></div>
        <button className="btn-primary w-full" disabled={busy || shelter.freePlaces === 0}>
          {busy ? 'Saving…' : shelter.freePlaces === 0 ? 'Shelter is full' : 'Check in'}
        </button>
      </form>
      <div className="card space-y-3">
        <h2 className="font-semibold text-navy-900">{alternatives ? 'Shelter full — nearest alternatives' : 'Tips'}</h2>
        {alternatives ? <NearestTable rows={alternatives} /> : (
          <ul className="text-sm text-slate-600 list-disc pl-5 space-y-1">
            <li>Write the name the way the family will search for it.</li>
            <li>Mark pregnant, elderly, disabled, infant or medical cases so the in-charge can plan care.</li>
            <li>If two volunteers take the last place at the same moment, only one check-in succeeds — the other sees alternatives.</li>
          </ul>
        )}
      </div>
    </div>
  )
}
