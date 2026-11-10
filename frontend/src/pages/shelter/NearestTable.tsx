import type { NearestShelter } from '../../api/types'
import { Empty, Facilities, StatusBadge } from '../../components/ui'

export default function NearestTable({ rows }: { rows: NearestShelter[] }) {
  if (rows.length === 0) return <Empty text="No shelter within range has enough free places." />
  return (
    <div className="overflow-x-auto">
      <table className="table">
        <thead><tr><th>Shelter</th><th>District</th><th className="text-right">Distance</th><th className="text-right">Free</th><th>Status</th><th>Facilities</th></tr></thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.shelterId}>
              <td className="font-medium">{r.name}<div className="text-xs text-slate-500">{r.village}</div></td>
              <td>{r.district}</td>
              <td className="text-right">{r.distanceKm.toFixed(1)} km</td>
              <td className="text-right">{r.freePlaces} / {r.capacity}</td>
              <td><StatusBadge status={r.status} /></td>
              <td><Facilities medical={r.hasMedical} ramp={r.hasRamp} generator={r.hasGenerator} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
