import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { errorMessage } from '../api/client'
import { homePathFor, useAuth } from '../auth/AuthContext'
import { DemoBanner, ErrorBox } from '../components/ui'

const demoAccounts = [
  ['State admin', 'state@aashraynet.demo'],
  ['Puri district admin', 'puri.admin@aashraynet.demo'],
  ['Satapada in-charge', 'satapada.incharge@aashraynet.demo'],
  ['Puri volunteer', 'puri.volunteer@aashraynet.demo'],
]

export default function Login() {
  const { login, user } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={homePathFor(user.role)} replace />

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true); setError(null)
    try {
      const u = await login(email, password)
      navigate(homePathFor(u.role))
    } catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-navy-950 to-navy-700">
      <DemoBanner />
      <div className="flex-1 flex items-center justify-center p-4">
        <div className="w-full max-w-md space-y-4">
          <form onSubmit={submit} className="card space-y-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-navy-900">Aashray<span className="text-sky-600">Net</span></div>
              <p className="text-sm text-slate-500">Staff login</p>
            </div>
            {error && <ErrorBox message={error} />}
            <div><label className="label" htmlFor="email">Email</label>
              <input id="email" type="email" autoComplete="username" className="input" value={email} onChange={(e) => setEmail(e.target.value)} required /></div>
            <div><label className="label" htmlFor="pw">Password</label>
              <input id="pw" type="password" autoComplete="current-password" className="input" value={password} onChange={(e) => setPassword(e.target.value)} required /></div>
            <button className="btn-primary w-full" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
            <Link to="/" className="block text-center text-xs text-slate-500 hover:underline">Back to public pages</Link>
          </form>
          {import.meta.env.DEV && (
            <div className="card text-xs space-y-1">
              <div className="font-semibold text-slate-700">Development demo accounts (password <code>Demo@1234</code>)</div>
              {demoAccounts.map(([label, mail]) => (
                <button key={mail} type="button" className="block text-left text-navy-700 hover:underline" onClick={() => { setEmail(mail); setPassword('Demo@1234') }}>
                  {label}: {mail}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
