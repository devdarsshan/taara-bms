import { StitchingSectionRef, StyleRef } from './common.models';

export type StitchingOrderStatus = 'PENDING' | 'PARTIALLY_DELIVERED' | 'COMPLETE';

export interface StitchingAvailability {
  styleAutoId: string;
  availablePieces: number;
}

export interface SectionPendingPiecesResponse {
  section: StitchingSectionRef;
  pendingPieces: number;
}

export interface StitchingDashboardResponse {
  totalPiecesInStitching: number;
  piecesDeliveredFromStitching: number;
  pendingOrdersCount: number;
  partiallyDeliveredOrdersCount: number;
  defectivePiecesFinalized: number;
  ordersBySection: SectionPendingPiecesResponse[];
}

export interface StitchingOrder {
  id: string;
  autoId: string;
  orderDate: string;
  stitchingSection: StitchingSectionRef;
  style: StyleRef;
  piecesOrdered: number;
  deliveredPieces: number;
  pendingPieces: number;
  status: StitchingOrderStatus;
  notes: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StitchingDelivery {
  id: string;
  autoId: string;
  deliveryDate: string;
  stitchingOrderId: string;
  stitchingOrderAutoId: string;
  stitchingSection: StitchingSectionRef;
  style: StyleRef;
  piecesDelivered: number;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StitchingOrderCreateRequest {
  orderDate: string;
  stitchingSectionAutoId: string;
  styleAutoId: string;
  piecesOrdered: number;
  notes?: string | null;
}

export interface StitchingOrderStatusUpdateRequest {
  status: StitchingOrderStatus;
}

export interface StitchingDeliveryCreateRequest {
  deliveryDate: string;
  stitchingOrderAutoId: string;
  piecesDelivered: number;
  overrideWarnings: boolean;
}
