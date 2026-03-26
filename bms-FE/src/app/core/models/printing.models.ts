import { GarmentSize, PieceAvailability, StitchingSectionRef, StyleRef } from './common.models';
import { StitchingOrderStatus } from './stitching.models';

export interface PrintingOrder {
  id: string;
  autoId: string;
  orderDate: string;
  printingSection: StitchingSectionRef;
  style: StyleRef;
  size: GarmentSize;
  piecesOrdered: number;
  deliveredPieces: number;
  pendingPieces: number;
  status: StitchingOrderStatus;
  notes: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PrintingDelivery {
  id: string;
  autoId: string;
  deliveryDate: string;
  printingOrderAutoId: string;
  printingSection: StitchingSectionRef;
  style: StyleRef;
  size: GarmentSize;
  piecesDelivered: number;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PrintingDashboardResponse {
  totalPiecesInPrinting: number;
  deliveredPiecesFromPrinting: number;
  pendingOrdersCount: number;
  defectivePiecesFinalized: number;
}

export interface PrintingOrderCreateRequest {
  orderDate: string;
  printingSectionAutoId: string;
  styleAutoId: string;
  size: GarmentSize;
  piecesOrdered: number;
  notes?: string | null;
}

export interface PrintingDeliveryCreateRequest {
  deliveryDate: string;
  printingOrderAutoId: string;
  piecesDelivered: number;
}

export type PrintingAvailability = PieceAvailability;
