import { Link } from 'react-router-dom'

export default function Home() {
  return (
    <div className="space-y-8">
      <section className="rounded-2xl bg-gradient-to-br from-navy-900 to-navy-700 text-white p-8 md:p-12">
        <h1 className="text-3xl md:text-4xl font-bold">Cyclone shelter &amp; relief coordination</h1>
        <p className="mt-3 max-w-2xl text-slate-200">
          AashrayNet helps district teams in Odisha track shelter occupancy, register evacuees, find the nearest shelter with
          free places and keep relief supplies flowing before they run out.
        </p>
        <div className="mt-6 flex flex-wrap gap-3">
          <Link to="/find" className="btn bg-white text-navy-900 hover:bg-slate-100">Find a relative</Link>
          <Link to="/available" className="btn border border-white/40 hover:bg-white/10">See available shelters</Link>
        </div>
      </section>
      <section className="grid md:grid-cols-3 gap-4">
        {[
          ['Live occupancy', 'Every shelter shows GREEN, YELLOW, RED or FULL from real check-ins.'],
          ['Nearest free shelter', 'When a shelter is full, the nearest ones with enough places are suggested instantly.'],
          ['Shortage alerts', 'Stock is compared with daily needs every hour, so shortages are spotted early.'],
        ].map(([t, d]) => (
          <div key={t} className="card"><h2 className="font-semibold text-navy-900">{t}</h2><p className="text-sm text-slate-600 mt-1">{d}</p></div>
        ))}
      </section>
    </div>
  )
}
