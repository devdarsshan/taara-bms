import { GarmentSize, ReadyToStitchBreakdownResponse, StitchingSectionRef, StyleRef } from './common.models';

export type StitchingOrderStatus = 'PENDING' | 'PARTIALLY_DELIVERED' | 'COMPLETE' | 'AUTO_CLOSED';

export interface StitchingAvailability {
  styleAutoId: string;
  size: GarmentSize;
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
  readyToStitchPieces: number;
  readyToStitchBreakdown: ReadyToStitchBreakdownResponse[];
  ordersBySection: SectionPendingPiecesResponse[];
}

export interface StitchingOrderRow {
  id: string;
  stitchingSection: StitchingSectionRef;
  style: StyleRef;
  size: GarmentSize;
  piecesTaken: number;
  ratePerPiece?: number | null;
  totalPrice?: number | null;
}

export interface StitchingOrder {
  id: string;
  autoId: string;
  orderDate: string;
  expectedSize: GarmentSize;
  expectedPieces: number;
  deliveredPieces: number;
  pendingPieces: number;
  totalPiecesTaken: number;
  status: StitchingOrderStatus;
  notes: string | null;
  rows: StitchingOrderRow[];
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StitchingDelivery {
  id: string;
  autoId: string;
  deliveryDate: string;
  stitchingSection: StitchingSectionRef;
  style: StyleRef;
  size: GarmentSize;
  piecesDelivered: number;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StitchingOrderRowRequest {
  stitchingSectionAutoId?: string;
  styleAutoId: string;
  size: GarmentSize;
  piecesTaken: number;
  ratePerPiece?: number | null;
}

export interface StitchingOrderCreateRequest {
  orderDate: string;
  expectedSize: GarmentSize;
  expectedPieces: number;
  rows: StitchingOrderRowRequest[];
  notes?: string | null;
}

export interface StitchingOrderUpdateRequest {
  orderDate: string;
  expectedSize: GarmentSize;
  expectedPieces: number;
  rows: StitchingOrderRowRequest[];
  notes?: string | null;
}

export interface StitchingOrderStatusUpdateRequest {
  status: StitchingOrderStatus;
}

export interface StitchingDeliveryCreateRequest {
  deliveryDate: string;
  stitchingSectionAutoId: string;
  styleAutoId: string;
  size: GarmentSize;
  piecesDelivered: number;
}
