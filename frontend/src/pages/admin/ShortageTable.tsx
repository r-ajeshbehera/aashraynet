import { Link } from 'react-router-dom'
import type { Shortage } from '../../api/types'
import { Empty } from '../../components/ui'

export default function ShortageTable({ rows }: { rows: Shortage[] }) {
  if (rows.length === 0) return <Empty text="No shortages right now." />
  return (
    <div className="overflow-x-auto"><table className="table">
      <thead><tr><th>Shelter</th><th>District</th><th>Item</th><th className="text-right">Stock</th><th className="text-right">People</th><th className="text-right">Daily need</th><th className="text-right">Days left</th></tr></thead>
      <tbody>{rows.map((s) => (
        <tr key={`${s.shelterId}-${s.itemId}`}>
          <td><Link className="text-navy-700 hover:underline font-medium" to={`/shelters/${s.shelterId}`}>{s.shelterName}</Link></td>
          <td>{s.districtName}</td><td>{s.itemName}</td>
          <td className="text-right">{s.quantity} {s.unit}</td><td className="text-right">{s.occupancy}</td>
          <td className="text-right">{(Number(s.perPersonPerDay) * s.occupancy).toFixed(1)} {s.unit}</td>
          <td className="text-right font-bold text-red-700">{s.daysRemaining}</td>
        </tr>))}
      </tbody></table></div>
  )
}
