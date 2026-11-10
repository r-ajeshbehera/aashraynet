import { Link, Outlet } from 'react-router-dom'
import { DemoBanner } from '../../components/ui'

export default function PublicLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      <DemoBanner />
      <header className="bg-navy-900 text-white">
        <div className="mx-auto max-w-5xl px-4 py-3 flex items-center gap-6">
          <Link to="/" className="font-bold text-lg">Aashray<span className="text-sky-300">Net</span></Link>
          <nav className="flex gap-4 text-sm text-slate-200 flex-1">
            <Link to="/find" className="hover:text-white">Find a relative</Link>
            <Link to="/available" className="hover:text-white">Available shelters</Link>
          </nav>
          <Link to="/login" className="rounded-md border border-white/30 px-3 py-1.5 text-sm hover:bg-navy-800">Staff login</Link>
        </div>
      </header>
      <main className="mx-auto max-w-5xl w-full px-4 py-8 flex-1"><Outlet /></main>
      <footer className="text-center text-xs text-slate-500 py-4">AashrayNet · portfolio project by Rajesh Behera · demonstration data only</footer>
    </div>
  )
}
