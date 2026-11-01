import { AppShell } from '../components/layout/AppShell'
import { STATION_NAV } from '../components/layout/nav'

/** Layout of /station/*: the shared app shell with the station menu, filtered by designation. */
export function StationLayout() {
  return <AppShell sections={STATION_NAV} portalLabel="Station Portal" />
}
