import { StyleRef } from './common.models';

export interface SpinningDashboardResponse {
  totalDispatchedToSpinning: number;
  totalReceivedFromSpinning: number;
  netPendingAtFactory: number;
}

export interface SpinningOrder {
  id: string;
  autoId: string;
  dispatchDate: string;
  linkedYarnOrderId: string | null;
  linkedYarnOrderAutoId: string | null;
  style: StyleRef;
  quantitySentKgs: number;
  factoryNotes: string | null;
  autoCreated: boolean;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SpinningDelivery {
  id: string;
  autoId: string;
  deliveryDate: string;
  style: StyleRef;
  actualQuantityKgs: number;
  bufferQuantityKgs: number;
  finalQuantityKgs: number;
  notes: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SpinningOrderCreateRequest {
  dispatchDate: string;
  linkedYarnOrderAutoId?: string | null;
  styleAutoId: string;
  quantitySentKgs: number;
  factoryNotes?: string | null;
}

export interface SpinningDeliveryCreateRequest {
  deliveryDate: string;
  styleAutoId: string;
  actualQuantityKgs: number;
  bufferQuantityKgs?: number | null;
  notes?: string | null;
}
