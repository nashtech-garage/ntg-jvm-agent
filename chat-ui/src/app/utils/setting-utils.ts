import { MaintenanceModeSetting } from '../models/maintenance-setting';

export async function getSetting(): Promise<MaintenanceModeSetting | null> {
  try {
    const baseUrl = `${process.env.NEXT_PUBLIC_ORCHESTRATOR_SERVER}/api/settings/isMaintenance`;

    const response = await fetch(`${baseUrl}`);
    if (!response.ok) {
      throw new Error('Failed to fetch system settings');
    }
    if (!response.ok) {
      throw new Error('Failed to fetch system settings');
    }
    const data: MaintenanceModeSetting = await response.json();
    return data;
  } catch (error) {
    console.error('Token refresh failed:', error);
    return null;
  }
}
