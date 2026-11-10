import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import RequireRole from './components/RequireRole'
import Login from './pages/Login'
import AvailableShelters from './pages/public/AvailableShelters'
import FindRelative from './pages/public/FindRelative'
import Home from './pages/public/Home'
import PublicLayout from './pages/public/PublicLayout'
import MyShelter from './pages/shelter/MyShelter'
import ShelterDetail from './pages/shelter/ShelterDetail'
import DistrictDashboard from './pages/admin/DistrictDashboard'
import EventsPage from './pages/admin/EventsPage'
import SheltersPage from './pages/admin/SheltersPage'
import ShortagesPage from './pages/admin/ShortagesPage'
import StateDashboard from './pages/admin/StateDashboard'
import SupplyQueue from './pages/admin/SupplyQueue'
import UsersPage from './pages/admin/UsersPage'

export default function App() {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route path="/" element={<Home />} />
        <Route path="/find" element={<FindRelative />} />
        <Route path="/available" element={<AvailableShelters />} />
      </Route>
      <Route path="/login" element={<Login />} />

      <Route element={<RequireRole />}>
        <Route element={<Layout />}>
          <Route path="/shelters/:id" element={<ShelterDetail />} />
          <Route element={<RequireRole roles={['VOLUNTEER', 'SHELTER_INCHARGE']} />}>
            <Route path="/my-shelter" element={<MyShelter />} />
          </Route>
          <Route element={<RequireRole roles={['VOLUNTEER', 'DISTRICT_ADMIN', 'STATE_ADMIN']} />}>
            <Route path="/shelters" element={<SheltersPage />} />
          </Route>
          <Route element={<RequireRole roles={['SHELTER_INCHARGE', 'DISTRICT_ADMIN', 'STATE_ADMIN']} />}>
            <Route path="/shortages" element={<ShortagesPage />} />
          </Route>
          <Route element={<RequireRole roles={['DISTRICT_ADMIN', 'STATE_ADMIN']} />}>
            <Route path="/district" element={<DistrictDashboard />} />
            <Route path="/supply-requests" element={<SupplyQueue />} />
            <Route path="/users" element={<UsersPage />} />
          </Route>
          <Route element={<RequireRole roles={['STATE_ADMIN']} />}>
            <Route path="/state" element={<StateDashboard />} />
            <Route path="/events" element={<EventsPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
