import { InHouseDashboardResponse } from './inhouse.models';
import { SpinningDashboardResponse } from './spinning.models';
import { StitchingDashboardResponse } from './stitching.models';
import { YarnDashboardResponse } from './yarn.models';

export type { InHouseDashboardResponse, SpinningDashboardResponse, StitchingDashboardResponse, YarnDashboardResponse };

export interface OverviewDashboardBundle {
  yarn: YarnDashboardResponse;
  spinning: SpinningDashboardResponse;
  inHouse: InHouseDashboardResponse;
  stitching: StitchingDashboardResponse;
}
