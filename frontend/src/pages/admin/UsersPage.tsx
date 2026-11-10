import { useState, type FormEvent } from 'react'
import { api, errorMessage } from '../../api/client'
import { useApi } from '../../api/useApi'
import type { District, Page, Role, User } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { ConfirmButton, ErrorBox, Loading, PageHeader, SuccessBox } from '../../components/ui'

export default function UsersPage() {
  const { user } = useAuth()
  const isState = user?.role === 'STATE_ADMIN'
  const roles: Role[] = isState ? ['VOLUNTEER', 'SHELTER_INCHARGE', 'DISTRICT_ADMIN', 'STATE_ADMIN'] : ['VOLUNTEER', 'SHELTER_INCHARGE']
  const districts = useApi<District[]>('/districts')
  const { data, loading, error, reload } = useApi<Page<User>>('/users', { size: 100 })
  const [f, setF] = useState({ fullName: '', email: '', password: '', role: 'VOLUNTEER' as Role, districtId: String(user?.districtId ?? '') })
  const [msg, setMsg] = useState<string | null>(null)
  const [err, setErr] = useState<string | null>(null)

  async function create(e: FormEvent) {
    e.preventDefault(); setMsg(null); setErr(null)
    try {
      await api.post('/users', { ...f, districtId: f.role === 'STATE_ADMIN' ? null : Number(f.districtId) })
      setMsg(`Account created for ${f.email}. Share the password securely.`)
      setF({ ...f, fullName: '', email: '', password: '' }); await reload()
    } catch (ex) { setErr(errorMessage(ex)) }
  }
  async function setActive(u: User, active: boolean) {
    try { await api.patch(`/users/${u.id}`, { active }); await reload() } catch (ex) { setErr(errorMessage(ex)) }
  }

  return (
    <div className="space-y-4">
      <PageHeader title="User accounts" subtitle="There is no public sign-up: administrators create every staff account." />
      {msg && <SuccessBox message={msg} />}{err && <ErrorBox message={err} />}
      <form onSubmit={create} className="card grid md:grid-cols-3 gap-3 items-end">
        <div><label className="label" htmlFor="un">Full name</label><input id="un" className="input" value={f.fullName} onChange={(e) => setF({ ...f, fullName: e.target.value })} required /></div>
        <div><label className="label" htmlFor="ue">Email</label><input id="ue" type="email" className="input" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} required /></div>
        <div><label className="label" htmlFor="up">Temporary password (min 8)</label><input id="up" type="password" minLength={8} className="input" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} required /></div>
        <div><label className="label" htmlFor="ur">Role</label><select id="ur" className="input" value={f.role} onChange={(e) => setF({ ...f, role: e.target.value as Role })}>{roles.map((r) => <option key={r}>{r}</option>)}</select></div>
        {isState && f.role !== 'STATE_ADMIN' && (
          <div><label className="label" htmlFor="ud">District</label><select id="ud" className="input" value={f.districtId} onChange={(e) => setF({ ...f, districtId: e.target.value })} required>
            <option value="">Choose…</option>{districts.data?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}</select></div>
        )}
        <div><button className="btn-primary">Create account</button></div>
      </form>
      <div className="card overflow-x-auto">
        {loading && <Loading />}{error && <ErrorBox message={error} onRetry={reload} />}
        {data && (
          <table className="table"><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>District</th><th>Status</th><th /></tr></thead>
            <tbody>{data.content.map((u) => (
              <tr key={u.id}><td className="font-medium">{u.fullName}</td><td>{u.email}</td><td className="text-xs">{u.role}</td><td>{u.districtName ?? '—'}</td>
                <td>{u.active ? 'Active' : <span className="text-slate-500">Disabled</span>}</td>
                <td>{u.id !== user?.id && <ConfirmButton label={u.active ? 'Disable' : 'Enable'} className="btn-secondary" title={`${u.active ? 'Disable' : 'Enable'} account?`}
                  message={u.active ? 'The user is logged out immediately and cannot log in.' : 'The user can log in again.'} onConfirm={() => setActive(u, !u.active)} />}</td></tr>))}
            </tbody></table>
        )}
      </div>
    </div>
  )
}
