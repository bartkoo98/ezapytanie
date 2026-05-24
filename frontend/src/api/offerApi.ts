import api from './axiosInstance';
import type { OfferResponse, SubmitOfferRequest } from '../types/api';

export const submitOffer = (data: SubmitOfferRequest) =>
  api.post<OfferResponse>('/offers', data).then((r) => r.data);

export const listMyOffers = () =>
  api.get<OfferResponse[]>('/offers/mine').then((r) => r.data);
