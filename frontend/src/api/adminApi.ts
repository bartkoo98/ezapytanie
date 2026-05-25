import api from './axiosInstance';
import type { UserResponse } from '../types/api';

export const listUsers = () =>
  api.get<UserResponse[]>('/admin/users').then((r) => r.data);
