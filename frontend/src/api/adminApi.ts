import api from './axiosInstance';
import type { UserResponse, AuditLog } from '../types/api';

export const listUsers = () =>
  api.get<UserResponse[]>('/admin/users').then((r) => r.data);

export const listAuditLogs = (params?: {
  entityId?: string;
  actorId?: string;
  action?: string;
  from?: string;
  to?: string;
}) => api.get<AuditLog[]>('/admin/audit-logs', { params }).then((r) => r.data);
