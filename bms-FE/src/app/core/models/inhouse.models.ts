import { DiaRef, GarmentSize, ReadyToStitchBreakdownResponse, StyleRef } from './common.models';

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
  ratePerPiece?: number | null;
}

export interface CuttingEntry {
  id: string;
  autoId: string;
  cuttingDate: string;
  totalQuantityUsedKgs: number;
  totalOutputPieces: number;
  pcsPerKg: number | null;
  totalPrice?: number;
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

export interface InHouseSplitRequest {
  diaAutoId: string;
  quantityKgs: number;
}

export interface InHouseSplitBatchRequest {
  splits: InHouseSplitRequest[];
}

export interface CuttingAvailability {
  splits: Array<{
    diaAutoId: string;
    quantityKgs: number;
  }>;
}

export interface InHouseSplitUpdateRequest {
  diaAutoId: string;
  quantityKgs: number;
}

export interface CuttingRowRequest {
  diaAutoId: string;
  styleAutoId: string;
  size: GarmentSize;
  quantityUsedKgs: number;
  ratePerPiece?: number | null;
}

export interface CuttingCreateRequest {
  cuttingDate: string;
  rows: CuttingRowRequest[];
  totalOutputPieces: number;
  notes?: string | null;
}

export interface CuttingUpdateRequest {
  rows: CuttingRowRequest[];
  totalOutputPieces: number;
  notes?: string | null;
}

export interface ExistingStockCreateRequest {
  entryDate: string;
  diaAutoId: string;
  styleAutoId: string;
  quantityKgs: number;
  notes?: string | null;
}

export interface ExistingStockResponse {
  id: string;
  autoId: string;
  entryDate: string;
  dia: DiaRef;
  style: StyleRef;
  quantityKgs: number;
  notes: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CuttingAvailability {
  diaAutoId: string;
  styleAutoId: string;
  availableQuantityKgs: number;
}
