export type UserRole = 'ADMIN' | 'CLIENT' | 'CONTRACTOR';
export type InquiryStatus = 'PUBLISHED' | 'CLOSED' | 'CANCELLED' | 'ARCHIVED';
export type OfferStatus = 'SUBMITTED' | 'WITHDRAWN' | 'SELECTED' | 'REJECTED';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  institutionName: string;
  role: UserRole;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  email: string;
  role: UserRole;
  fullName: string;
  institutionName: string;
}

export interface InquiryResponse {
  id: string;
  title: string;
  description: string;
  category: string;
  clientId: string;
  status: InquiryStatus;
  deadline: string;
  winnerOfferId: string | null;
  cancellationReason: string | null;
  selectionJustification: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface InquiryQuestionResponse {
  id: string;
  questionText: string;
  answerText: string | null;
  createdAt: string;
  answeredAt: string | null;
}

export interface AskQuestionRequest {
  questionText: string;
}

export interface AnswerQuestionRequest {
  answerText: string;
}

export interface CreateInquiryRequest {
  title: string;
  description: string;
  category: string;
  deadline: string;
}

export interface CancelInquiryRequest {
  cancellationReason: string;
}

export interface OfferResponse {
  id: string;
  inquiryId: string;
  inquiryTitle: string;
  contractorId: string;
  contractorName: string | null;
  price: number | null;
  currency: string | null;
  notes: string | null;
  status: OfferStatus;
  submittedAt: string;
  updatedAt: string;
}

export interface SubmitOfferRequest {
  inquiryId: string;
  price: number;
  notes: string;
}

export interface UserResponse {
  id: string;
  email: string;
  fullName: string;
  institutionName: string;
  role: UserRole;
  active: boolean;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface AuditLog {
  id: string;
  timestamp: string;
  actorId: string;
  actorRole: string;
  actorEmail: string;
  action: string;
  entityType: string;
  entityId: string;
  details: Record<string, unknown>;
  ipAddress: string;
  userAgent: string;
}

export interface ErrorResponse {
  code: string;
  message: string;
}
