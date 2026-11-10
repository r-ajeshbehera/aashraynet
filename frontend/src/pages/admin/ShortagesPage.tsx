import { useApi } from '../../api/useApi'
import type { Shortage } from '../../api/types'
import { ErrorBox, Loading, PageHeader } from '../../components/ui'
import ShortageTable from './ShortageTable'

export default function ShortagesPage() {
  const { data, loading, error, reload } = useApi<Shortage[]>('/shortages')
  return (
    <div>
      <PageHeader title="Stock shortages" subtitle="days remaining = stock ÷ (people × daily need per person). Below 1 day = shortage."
        actions={<button className="btn-secondary" onClick={reload}>Refresh</button>} />
      <div className="card">
        {loading && <Loading />}{error && <ErrorBox message={error} onRetry={reload} />}
        {data && <ShortageTable rows={data} />}
      </div>
    </div>
  )
}
