import { GarmentSize, PieceAvailability, StyleRef } from './common.models';

export type PackingStockType = 'PLAIN' | 'PRINTED';

export interface PackingEntry {
  id: string;
  autoId: string;
  packingDate: string;
  style: StyleRef;
  size: GarmentSize;
  stockType: PackingStockType;
  correctlyPackedPieces: number;
  defectivePieces: number;
  totalConsumedPieces: number;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PackingDashboardResponse {
  totalPackedPieces: number;
  totalDefectivePieces: number;
}

export interface PackingCreateRequest {
  packingDate: string;
  styleAutoId: string;
  size: GarmentSize;
  stockType: PackingStockType;
  correctlyPackedPieces: number;
  defectivePieces: number;
}

export type PackingAvailability = PieceAvailability;
