import { DiaRef, StyleRef } from './common.models';

export type SplitStatus = 'PENDING' | 'PARTIALLY_SPLIT' | 'FULLY_SPLIT';
export type CuttingStatus = 'IN_PROGRESS' | 'COMPLETED';

export interface InHouseDashboardResponse {
  totalFabricInStock: number;
  cuttingInProgressCount: number;
  totalPiecesCut: number;
  readyToStitchPieces: number;
  stitchedStockTotal: number;
  defectiveStockTotal: number;
}

export interface InHouseDelivery {
  id: string;
  autoId: string;
  deliveryDate: string;
  spinningDeliveryId: string;
  spinningDeliveryAutoId: string;
  style: StyleRef;
  quantityKgs: number;
  allocatedQuantityKgs: number;
  splitStatus: SplitStatus;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InHouseSplit {
  id: string;
  autoId: string;
  deliveryId: string;
  deliveryAutoId: string;
  dia: DiaRef;
  style: StyleRef;
  quantityKgs: number;
  canDelete: boolean;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InHouseStock {
  dia: DiaRef;
  style: StyleRef;
  availableQuantityKgs: number;
}

export interface CuttingEntry {
  id: string;
  autoId: string;
  cuttingDate: string;
  dia: DiaRef;
  style: StyleRef;
  quantityUsedKgs: number;
  outputPieces: number | null;
  status: CuttingStatus;
  notes: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StitchedStock {
  style: StyleRef;
  latestTransactionDate: string | null;
  goodPieces: number;
  defectivePieces: number;
}

export interface InHouseSplitBatchRequest {
  splits: Array<{
    diaAutoId: string;
    quantityKgs: number;
  }>;
}

export interface CuttingCreateRequest {
  cuttingDate: string;
  diaAutoId: string;
  styleAutoId: string;
  quantityUsedKgs: number;
  outputPieces?: number | null;
  notes?: string | null;
}

export interface CuttingUpdateRequest {
  outputPieces?: number | null;
  notes?: string | null;
}

export interface CuttingAvailability {
  diaAutoId: string;
  styleAutoId: string;
  availableQuantityKgs: number;
}
