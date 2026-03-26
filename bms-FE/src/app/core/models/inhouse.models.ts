import { DiaRef, GarmentSize, StyleRef } from './common.models';

export type SplitStatus = 'PENDING' | 'PARTIALLY_SPLIT' | 'FULLY_SPLIT';
export type CuttingStatus = 'IN_PROGRESS' | 'COMPLETED';

export interface InHouseDashboardResponse {
  totalFabricInStock: number;
  cuttingInProgressCount: number;
  totalPiecesCut: number;
  readyToStitchPieces: number;
  stitchedPlainStockTotal: number;
  printedStockTotal: number;
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

export interface CuttingRow {
  id?: string;
  dia: DiaRef;
  style: StyleRef;
  size: GarmentSize;
  quantityUsedKgs: number;
  outputPieces: number | null;
}

export interface CuttingEntry {
  id: string;
  autoId: string;
  cuttingDate: string;
  totalQuantityUsedKgs: number;
  totalOutputPieces: number;
  pcsPerKg: number | null;
  status: CuttingStatus;
  notes: string | null;
  rows: CuttingRow[];
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StitchedStock {
  style: StyleRef;
  size: GarmentSize;
  plainPieces: number;
  printedPieces: number;
  latestTransactionDate: string | null;
  defectivePieces: number;
}

export interface InHouseSplitBatchRequest {
  splits: Array<{
    diaAutoId: string;
    quantityKgs: number;
  }>;
}

export interface CuttingRowRequest {
  diaAutoId: string;
  styleAutoId: string;
  size: GarmentSize;
  quantityUsedKgs: number;
  outputPieces?: number | null;
}

export interface CuttingCreateRequest {
  cuttingDate: string;
  rows: CuttingRowRequest[];
  notes?: string | null;
}

export interface CuttingUpdateRequest {
  rows: CuttingRowRequest[];
  notes?: string | null;
}

export interface CuttingAvailability {
  diaAutoId: string;
  styleAutoId: string;
  availableQuantityKgs: number;
}
