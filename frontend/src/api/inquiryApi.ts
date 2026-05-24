import api from './axiosInstance';
import type {
  InquiryResponse,
  CreateInquiryRequest,
  CancelInquiryRequest,
  OfferResponse,
  InquiryQuestionResponse,
  AskQuestionRequest,
  AnswerQuestionRequest,
  PageResponse,
} from '../types/api';

export const listInquiries = (page = 0, size = 10) =>
  api.get<PageResponse<InquiryResponse>>('/inquiries', { params: { page, size } }).then((r) => r.data);

export const getInquiry = (id: string) =>
  api.get<InquiryResponse>(`/inquiries/${id}`).then((r) => r.data);

export const createInquiry = (data: CreateInquiryRequest) =>
  api.post<InquiryResponse>('/inquiries', data).then((r) => r.data);

export const cancelInquiry = (id: string, data: CancelInquiryRequest) =>
  api.patch<InquiryResponse>(`/inquiries/${id}/cancel`, data).then((r) => r.data);

export const listOffersForInquiry = (id: string) =>
  api.get<OfferResponse[]>(`/inquiries/${id}/offers`).then((r) => r.data);

export const acceptOffer = (inquiryId: string, offerId: string, selectionJustification: string) =>
  api.patch<InquiryResponse>(`/inquiries/${inquiryId}/accept-offer/${offerId}`, { selectionJustification }).then((r) => r.data);

export const listQuestionsForInquiry = (id: string) =>
  api.get<InquiryQuestionResponse[]>(`/inquiries/${id}/questions`).then((r) => r.data);

export const askQuestion = (inquiryId: string, data: AskQuestionRequest) =>
  api.post<InquiryQuestionResponse>(`/inquiries/${inquiryId}/questions`, data).then((r) => r.data);

export const answerQuestion = (inquiryId: string, questionId: string, data: AnswerQuestionRequest) =>
  api.put<InquiryQuestionResponse>(`/inquiries/${inquiryId}/questions/${questionId}/answer`, data).then((r) => r.data);
