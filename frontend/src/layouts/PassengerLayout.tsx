import { AppShell } from '../components/layout/AppShell'
import { PASSENGER_NAV } from '../components/layout/nav'

/** Layout of /passenger/*: the shared app shell with the passenger menu. */
export function PassengerLayout() {
  return <AppShell sections={PASSENGER_NAV} portalLabel="Passenger Portal" />
}
