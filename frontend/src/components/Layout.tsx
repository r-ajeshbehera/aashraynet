import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import type { Role } from '../api/types'
import { DemoBanner } from './ui'

const links: { to: string; label: string; roles: Role[] }[] = [
  { to: '/state', label: 'State dashboard', roles: ['STATE_ADMIN'] },
  { to: '/district', label: 'District dashboard', roles: ['DISTRICT_ADMIN', 'STATE_ADMIN'] },
  { to: '/my-shelter', label: 'My shelter', roles: ['VOLUNTEER', 'SHELTER_INCHARGE'] },
  { to: '/shelters', label: 'Shelters', roles: ['DISTRICT_ADMIN', 'STATE_ADMIN', 'VOLUNTEER'] },
  { to: '/supply-requests', label: 'Supply requests', roles: ['DISTRICT_ADMIN', 'STATE_ADMIN'] },
  { to: '/shortages', label: 'Shortages', roles: ['DISTRICT_ADMIN', 'STATE_ADMIN', 'SHELTER_INCHARGE'] },
  { to: '/events', label: 'Events', roles: ['STATE_ADMIN'] },
  { to: '/users', label: 'Users', roles: ['DISTRICT_ADMIN', 'STATE_ADMIN'] },
]

const roleLabel: Record<Role, string> = {
  VOLUNTEER: 'Volunteer', SHELTER_INCHARGE: 'Shelter in-charge', DISTRICT_ADMIN: 'District admin', STATE_ADMIN: 'State admin',
}

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  if (!user) return null
  return (
    <div className="min-h-screen flex flex-col">
      <DemoBanner />
      <header className="bg-navy-900 text-white">
        <div className="mx-auto max-w-7xl px-4 py-3 flex flex-wrap items-center gap-x-6 gap-y-2">
          <div className="font-bold text-lg tracking-tight">Aashray<span className="text-sky-300">Net</span></div>
          <nav className="flex flex-wrap gap-1 text-sm flex-1">
            {links.filter((l) => l.roles.includes(user.role)).map((l) => (
              <NavLink key={l.to} to={l.to} className={({ isActive }) =>
                `rounded-md px-3 py-1.5 ${isActive ? 'bg-navy-700 text-white' : 'text-slate-200 hover:bg-navy-800'}`}>{l.label}</NavLink>
            ))}
          </nav>
          <div className="flex items-center gap-3 text-sm">
            <div className="text-right leading-tight">
              <div className="font-medium">{user.fullName}</div>
              <div className="text-xs text-slate-300">{roleLabel[user.role]}{user.districtName ? ` · ${user.districtName}` : ''}</div>
            </div>
            <button className="rounded-md border border-white/30 px-3 py-1.5 hover:bg-navy-800"
              onClick={async () => { await logout(); navigate('/login') }}>Log out</button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-7xl w-full px-4 py-6 flex-1"><Outlet /></main>
    </div>
  )
}
