export type Role = 'VOLUNTEER' | 'SHELTER_INCHARGE' | 'DISTRICT_ADMIN' | 'STATE_ADMIN'
export type OccupancyStatus = 'GREEN' | 'YELLOW' | 'RED' | 'FULL'
export type Gender = 'MALE' | 'FEMALE' | 'OTHER'
export type SpecialNeed = 'NONE' | 'PREGNANT' | 'ELDERLY' | 'DISABLED' | 'INFANT' | 'MEDICAL'
export type RequestStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'DISPATCHED' | 'RECEIVED'
export type MovementType = 'INITIAL' | 'RECEIPT' | 'CONSUMPTION' | 'ADJUSTMENT_IN' | 'ADJUSTMENT_OUT'

export interface User { id: number; fullName: string; email: string; role: Role; districtId: number | null; districtName: string | null; active: boolean }
export interface LoginResponse { accessToken: string; tokenType: string; expiresAt: string; user: User }
export interface District { id: number; name: string }
export interface DisasterEvent { id: number; name: string; startDate: string; endDate: string | null; active: boolean }

export interface Shelter {
  id: number; districtId: number; districtName: string; name: string; village: string
  latitude: number; longitude: number; capacity: number; occupancy: number; freePlaces: number
  occupancyPercent: number; status: OccupancyStatus; hasMedical: boolean; hasRamp: boolean; hasGenerator: boolean
  inchargeId: number | null; inchargeName: string | null; active: boolean; updatedAt: string | null
}
export interface NearestShelter {
  shelterId: number; name: string; district: string; village: string; distanceKm: number
  capacity: number; occupancy: number; freePlaces: number; status: OccupancyStatus
  hasMedical: boolean; hasRamp: boolean; hasGenerator: boolean
}
export interface Evacuee {
  id: number; eventId: number; shelterId: number; shelterName: string; fullName: string; age: number
  gender: Gender; village: string; specialNeed?: SpecialNeed; checkedInAt: string; checkedOutAt: string | null
}
export interface CheckInResponse { evacuee: Evacuee; shelter: Shelter }
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }

export interface SupplyItem { id: number; name: string; unit: string; perPersonPerDay: number }
export interface Stock { itemId: number; itemName: string; unit: string; quantity: number; perPersonPerDay: number; daysRemaining: number | null; shortage: boolean }
export interface Movement { id: number; itemId: number; type: MovementType; quantityDelta: number; balanceAfter: number; requestId: number | null; reason: string | null; performedBy: number; createdAt: string }
export interface SupplyRequest {
  id: number; eventId: number; shelterId: number; shelterName: string; districtId: number; status: RequestStatus
  note: string | null; rejectionReason: string | null; requestedBy: string | null; approvedBy: string | null
  dispatchedBy: string | null; receivedBy: string | null; createdAt: string; decidedAt: string | null
  dispatchedAt: string | null; receivedAt: string | null
  lines: { itemId: number; itemName: string; unit: string; quantity: number }[]
  history: { fromStatus: RequestStatus | null; toStatus: RequestStatus; changedBy: number; note: string | null; changedAt: string }[]
}
export interface Shortage { shelterId: number; shelterName: string; districtId: number; districtName: string; itemId: number; itemName: string; unit: string; quantity: number; occupancy: number; perPersonPerDay: number; daysRemaining: number }

export interface DistrictDashboard {
  districtId: number; districtName: string; totalEvacuees: number; activeShelters: number; totalCapacity: number
  availableCapacity: number; sheltersByStatus: Record<OccupancyStatus, number>
  occupancyByShelter: { shelterId: number; name: string; capacity: number; occupancy: number; percent: number; status: OccupancyStatus }[]
  specialNeeds: Record<SpecialNeed, number> | null; pendingSupplyRequests: number
  supplyRequestsByStatus: Record<RequestStatus, number>; shortages: Shortage[]
}
export interface StateDashboard {
  activeEvents: DisasterEvent[]; totalEvacuees: number; totalCapacity: number; availableCapacity: number
  sheltersByStatus: Record<OccupancyStatus, number>; supplyRequestsByStatus: Record<RequestStatus, number>; openShortages: number
  districts: { districtId: number; districtName: string; shelters: number; capacity: number; occupancy: number; evacuees: number; fullShelters: number; shortages: number; pendingRequests: number }[]
}
export interface PublicFind { matches: { shelterName: string; district: string }[]; notice: string }
export interface PublicShelters { dataNotice: string; generatedAt: string; shelters: { shelterName: string; district: string; village: string; status: OccupancyStatus; freePlaces: number; hasMedical: boolean; hasRamp: boolean; lastUpdated: string | null }[] }

export interface ApiError { status: number; code: string; message: string; fieldErrors?: Record<string, string>; details?: { alternatives?: NearestShelter[] } }
