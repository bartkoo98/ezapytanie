import api from './axiosInstance';
import type { UserResponse, UpdateProfileRequest } from '../types/api';

export const getProfile = () =>
  api.get<UserResponse>('/users/profile').then((r) => r.data);

export const updateProfile = (data: UpdateProfileRequest) =>
  api.put<UserResponse>('/users/profile', data).then((r) => r.data);
