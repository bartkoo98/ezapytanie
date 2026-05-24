import api from './axiosInstance';
import type { LoginRequest, LoginResponse, RegisterRequest } from '../types/api';

export const login = (data: LoginRequest) =>
  api.post<LoginResponse>('/auth/login', data).then((r) => r.data);

export const register = (data: RegisterRequest) =>
  api.post('/auth/register', data).then((r) => r.data);
