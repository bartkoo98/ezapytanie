import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Calendar,
  Tag,
  AlertCircle,
  Eye,
  EyeOff,
  CheckCircle2,
  X,
} from 'lucide-react';
import {
  getInquiry,
  listOffersForInquiry,
  cancelInquiry,
  acceptOffer,
  listQuestionsForInquiry,
  askQuestion,
  answerQuestion,
} from '../api/inquiryApi';
import { submitOffer } from '../api/offerApi';
import { useAuth } from '../context/AuthContext';
import type { InquiryResponse, OfferResponse, InquiryQuestionResponse } from '../types/api';
import StatusBadge from '../components/StatusBadge';

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('pl-PL', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatPrice(price: number | null, currency: string | null) {
  if (price === null) return null;
  return new Intl.NumberFormat('pl-PL', {
    style: 'currency',
    currency: currency ?? 'PLN',
  }).format(price);
}

export default function InquiryDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [inquiry, setInquiry] = useState<InquiryResponse | null>(null);
  const [offers, setOffers] = useState<OfferResponse[]>([]);
  const [questions, setQuestions] = useState<InquiryQuestionResponse[]>([]);
  const [loadingInquiry, setLoadingInquiry] = useState(true);
  const [loadingOffers, setLoadingOffers] = useState(true);
  const [loadingQuestions, setLoadingQuestions] = useState(true);
  const [error, setError] = useState('');

  const [showCancelModal, setShowCancelModal] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelling, setCancelling] = useState(false);
  const [accepting, setAccepting] = useState<string | null>(null);

  const [showJustificationModal, setShowJustificationModal] = useState(false);
  const [pendingOfferId, setPendingOfferId] = useState<string | null>(null);
  const [justificationText, setJustificationText] = useState('');

  const [offerPrice, setOfferPrice] = useState('');
  const [offerNotes, setOfferNotes] = useState('');
  const [submittingOffer, setSubmittingOffer] = useState(false);
  const [offerSuccess, setOfferSuccess] = useState(false);

  const [questionText, setQuestionText] = useState('');
  const [askingQuestion, setAskingQuestion] = useState(false);
  const [answeringId, setAnsweringId] = useState<string | null>(null);
  const [answerText, setAnswerText] = useState('');
  const [answeringQuestion, setAnsweringQuestion] = useState(false);

  const bidsRevealed = inquiry?.status !== 'PUBLISHED';

  useEffect(() => {
    if (!id) return;
    getInquiry(id)
      .then(setInquiry)
      .catch(() => setError('Nie znaleziono zapytania.'))
      .finally(() => setLoadingInquiry(false));

    listOffersForInquiry(id)
      .then(setOffers)
      .finally(() => setLoadingOffers(false));

    listQuestionsForInquiry(id)
      .then(setQuestions)
      .finally(() => setLoadingQuestions(false));
  }, [id]);

  const handleCancel = async () => {
    if (!id || !cancelReason.trim()) return;
    setCancelling(true);
    try {
      const updated = await cancelInquiry(id, { cancellationReason: cancelReason });
      setInquiry(updated);
      setShowCancelModal(false);
    } catch (err: unknown) {
      setError((err as { message?: string })?.message ?? 'Nie udało się anulować.');
    } finally {
      setCancelling(false);
    }
  };

  const handleOpenJustificationModal = (offerId: string) => {
    setPendingOfferId(offerId);
    setJustificationText('');
    setShowJustificationModal(true);
  };

  const handleConfirmAcceptOffer = async () => {
    if (!id || !pendingOfferId) return;
    setAccepting(pendingOfferId);
    setShowJustificationModal(false);
    try {
      const updated = await acceptOffer(id, pendingOfferId, justificationText);
      setInquiry(updated);
      const refreshed = await listOffersForInquiry(id);
      setOffers(refreshed);
    } catch (err: unknown) {
      setError((err as { message?: string })?.message ?? 'Nie udało się wybrać oferty.');
    } finally {
      setAccepting(null);
      setPendingOfferId(null);
    }
  };

  const handleSubmitOffer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    setSubmittingOffer(true);
    setError('');
    try {
      await submitOffer({
        inquiryId: id,
        price: Number(offerPrice),
        notes: offerNotes,
      });
      setOfferSuccess(true);
      const refreshed = await listOffersForInquiry(id);
      setOffers(refreshed);
    } catch (err: unknown) {
      setError((err as { message?: string })?.message ?? 'Nie udało się złożyć oferty.');
    } finally {
      setSubmittingOffer(false);
    }
  };

  const handleAskQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !questionText.trim()) return;
    setAskingQuestion(true);
    try {
      const q = await askQuestion(id, { questionText });
      setQuestions((prev) => [...prev, q]);
      setQuestionText('');
    } catch (err: unknown) {
      setError((err as { message?: string })?.message ?? 'Nie udało się zadać pytania.');
    } finally {
      setAskingQuestion(false);
    }
  };

  const handleAnswerQuestion = async (questionId: string) => {
    if (!id || !answerText.trim()) return;
    setAnsweringQuestion(true);
    try {
      const updated = await answerQuestion(id, questionId, { answerText });
      setQuestions((prev) => prev.map((q) => (q.id === questionId ? updated : q)));
      setAnsweringId(null);
      setAnswerText('');
    } catch (err: unknown) {
      setError((err as { message?: string })?.message ?? 'Nie udało się odpowiedzieć na pytanie.');
    } finally {
      setAnsweringQuestion(false);
    }
  };

  if (loadingInquiry) {
    return (
      <div className="max-w-4xl mx-auto space-y-4 animate-pulse">
        <div className="h-8 bg-gray-200 rounded w-1/3" />
        <div className="h-40 bg-gray-200 rounded-xl" />
      </div>
    );
  }

  if (!inquiry) {
    return (
      <div className="max-w-4xl mx-auto text-center py-20 text-gray-400">
        <AlertCircle size={32} className="mx-auto mb-2 opacity-40" />
        <p>{error || 'Nie znaleziono zapytania.'}</p>
        <Link to="/inquiries" className="mt-3 inline-block text-sm text-blue-600 hover:underline">
          ← Wróć do listy
        </Link>
      </div>
    );
  }

  const canCancel = user?.role === 'CLIENT' && inquiry.status === 'PUBLISHED';
  const canSelectWinner = user?.role === 'CLIENT' && inquiry.status === 'CLOSED';
  const canSubmitOffer = user?.role === 'CONTRACTOR' && inquiry.status === 'PUBLISHED';
  const canAskQuestion = user?.role === 'CONTRACTOR' && inquiry.status === 'PUBLISHED';
  const canAnswerQuestion = user?.role === 'CLIENT';

  return (
    <div className="max-w-4xl mx-auto space-y-5">
      {showCancelModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-md bg-white rounded-2xl shadow-xl p-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-4">Anuluj zapytanie</h2>
            <p className="text-sm text-gray-500 mb-4">
              Podaj powód anulowania. Ta operacja jest nieodwracalna.
            </p>
            <textarea
              rows={3}
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              placeholder="Powód anulowania…"
              className="w-full px-3 py-2.5 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-red-400 resize-none mb-4"
            />
            <div className="flex gap-3">
              <button
                onClick={() => setShowCancelModal(false)}
                className="flex-1 py-2.5 text-sm font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-lg"
              >
                Wróć
              </button>
              <button
                disabled={!cancelReason.trim() || cancelling}
                onClick={handleCancel}
                className="flex-1 py-2.5 text-sm font-semibold text-white bg-red-600 hover:bg-red-700 disabled:opacity-60 rounded-lg"
              >
                {cancelling ? 'Anulowanie…' : 'Potwierdź anulowanie'}
              </button>
            </div>
          </div>
        </div>
      )}

      {showJustificationModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-md bg-white rounded-2xl shadow-xl p-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-1">Wybierz ofertę</h2>
            <p className="text-sm text-gray-500 mb-4">
              Uzasadnienie wyboru jest widoczne tylko dla Ciebie i służy jako notatka wewnętrzna.
            </p>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Uzasadnienie wyboru (opcjonalne)
            </label>
            <textarea
              rows={3}
              value={justificationText}
              onChange={(e) => setJustificationText(e.target.value)}
              placeholder="np. Najlepsza relacja ceny do jakości, terminowość realizacji…"
              className="w-full px-3 py-2.5 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-green-500 resize-none mb-4"
            />
            <div className="flex gap-3">
              <button
                onClick={() => setShowJustificationModal(false)}
                className="flex-1 py-2.5 text-sm font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-lg"
              >
                Anuluj
              </button>
              <button
                onClick={handleConfirmAcceptOffer}
                className="flex-1 py-2.5 text-sm font-semibold text-white bg-green-600 hover:bg-green-700 rounded-lg"
              >
                Potwierdź wybór
              </button>
            </div>
          </div>
        </div>
      )}

      <button
        onClick={() => navigate('/inquiries')}
        className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800"
      >
        <ArrowLeft size={16} />
        Wróć
      </button>

      {error && (
        <div className="flex items-center gap-2 rounded-lg bg-red-50 border border-red-200 px-3 py-2.5 text-sm text-red-700">
          <AlertCircle size={15} className="shrink-0" />
          <span className="flex-1">{error}</span>
          <button onClick={() => setError('')}><X size={14} /></button>
        </div>
      )}

      <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
        <div className="flex items-start justify-between gap-4">
          <div>
            <div className="mb-1">
              <StatusBadge status={inquiry.status} />
            </div>
            <h1 className="text-xl font-bold text-gray-900">{inquiry.title}</h1>
          </div>
          {canCancel && (
            <button
              onClick={() => setShowCancelModal(true)}
              className="flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-red-600 border border-red-200 rounded-lg hover:bg-red-50 shrink-0"
            >
              <X size={15} />
              Anuluj
            </button>
          )}
        </div>

        <p className="text-sm text-gray-600 leading-relaxed">{inquiry.description}</p>

        <div className="flex flex-wrap gap-4 pt-2 border-t border-gray-100">
          <div className="flex items-center gap-2 text-sm text-gray-500">
            <Tag size={14} className="text-gray-400" />
            <span>{inquiry.category}</span>
          </div>
          <div className="flex items-center gap-2 text-sm text-gray-500">
            <Calendar size={14} className="text-gray-400" />
            <span>Termin: {formatDate(inquiry.deadline)}</span>
          </div>
        </div>

        {inquiry.cancellationReason && (
          <div className="flex items-start gap-2 rounded-lg bg-red-50 border border-red-100 px-3 py-2 text-sm text-red-700">
            <AlertCircle size={14} className="shrink-0 mt-0.5" />
            Powód anulowania: {inquiry.cancellationReason}
          </div>
        )}

        {inquiry.selectionJustification && user?.role === 'CLIENT' && (
          <div className="rounded-lg bg-blue-50 border border-blue-100 px-3 py-2 text-sm text-blue-800">
            <span className="font-medium">Uzasadnienie wyboru: </span>
            {inquiry.selectionJustification}
          </div>
        )}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="mb-4">
          <h2 className="text-base font-semibold text-gray-800">Oferty ({offers.length})</h2>
          <p className="text-xs text-gray-400 flex items-center gap-1 mt-0.5">
            {bidsRevealed ? (
              <><Eye size={12} /> Oferty są widoczne</>
            ) : (
              <><EyeOff size={12} /> Oferty są zapieczętowane do zakończenia terminu</>
            )}
          </p>
        </div>

        {loadingOffers ? (
          <div className="space-y-3">
            {[1, 2].map((i) => <div key={i} className="h-16 bg-gray-100 rounded-lg animate-pulse" />)}
          </div>
        ) : offers.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-8">Brak złożonych ofert.</p>
        ) : (
          <div className="space-y-3">
            {offers.map((offer) => {
              const isWinner = inquiry.winnerOfferId === offer.id;
              return (
                <div
                  key={offer.id}
                  className={`rounded-lg border p-4 ${isWinner ? 'border-green-300 bg-green-50' : 'border-gray-200'}`}
                >
                  <div className="flex items-center justify-between">
                    <div className="space-y-1">
                      {bidsRevealed && offer.contractorName ? (
                        <p className="text-sm font-medium text-gray-800">{offer.contractorName}</p>
                      ) : (
                        <p className="text-sm text-gray-400 italic">Wykonawca ukryty</p>
                      )}

                      {bidsRevealed && offer.price !== null ? (
                        <p className="text-lg font-bold text-gray-900">
                          {formatPrice(offer.price, offer.currency)}
                        </p>
                      ) : (
                        <p className="text-sm text-gray-400 italic">Cena zapieczętowana</p>
                      )}

                      {bidsRevealed && offer.notes && (
                        <p className="text-xs text-gray-500">{offer.notes}</p>
                      )}

                      <StatusBadge status={offer.status} type="offer" />
                    </div>

                    <div className="flex items-center gap-2">
                      {isWinner && (
                        <span className="flex items-center gap-1 text-xs font-semibold text-green-700">
                          <CheckCircle2 size={14} />
                          Wybrana
                        </span>
                      )}
                      {canSelectWinner && offer.status === 'SUBMITTED' && !inquiry.winnerOfferId && (
                        <button
                          disabled={accepting === offer.id}
                          onClick={() => handleOpenJustificationModal(offer.id)}
                          className="px-3 py-1.5 text-xs font-semibold text-white bg-green-600 hover:bg-green-700 disabled:opacity-60 rounded-lg transition-colors"
                        >
                          {accepting === offer.id ? 'Wybieranie…' : 'Wybierz ofertę'}
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {canSubmitOffer && (
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h2 className="text-base font-semibold text-gray-800 mb-4">Złóż ofertę</h2>
          {offerSuccess ? (
            <div className="flex items-center gap-2 rounded-lg bg-green-50 border border-green-200 px-4 py-3 text-sm text-green-700">
              <CheckCircle2 size={16} />
              Oferta złożona pomyślnie.
            </div>
          ) : (
            <form onSubmit={handleSubmitOffer} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Cena (PLN)</label>
                <input
                  type="number"
                  required
                  min="0"
                  step="0.01"
                  value={offerPrice}
                  onChange={(e) => setOfferPrice(e.target.value)}
                  placeholder="np. 5000.00"
                  className="w-full px-3 py-2.5 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Uwagi (opcjonalnie)</label>
                <textarea
                  rows={2}
                  value={offerNotes}
                  onChange={(e) => setOfferNotes(e.target.value)}
                  placeholder="Dodatkowe informacje…"
                  className="w-full px-3 py-2.5 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                />
              </div>
              <button
                type="submit"
                disabled={submittingOffer}
                className="px-4 py-2.5 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-60 rounded-lg transition-colors"
              >
                {submittingOffer ? 'Składanie…' : 'Złóż ofertę'}
              </button>
            </form>
          )}
        </div>
      )}

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h2 className="text-base font-semibold text-gray-800 mb-4">
          Pytania i odpowiedzi ({questions.length})
        </h2>

        {loadingQuestions ? (
          <div className="space-y-3">
            {[1, 2].map((i) => <div key={i} className="h-12 bg-gray-100 rounded-lg animate-pulse" />)}
          </div>
        ) : questions.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-6">Brak pytań do tego zapytania.</p>
        ) : (
          <ul className="space-y-4">
            {questions.map((q) => (
              <li key={q.id} className="border border-gray-100 rounded-lg p-4">
                <p className="text-sm font-medium text-gray-800">P: {q.questionText}</p>
                {q.answerText ? (
                  <p className="mt-2 text-sm text-gray-700 bg-blue-50 rounded-lg px-3 py-2">
                    O: {q.answerText}
                  </p>
                ) : (
                  <p className="mt-1 text-xs text-gray-400 italic">Brak odpowiedzi</p>
                )}
                {canAnswerQuestion && !q.answerText && answeringId !== q.id && (
                  <button
                    onClick={() => { setAnsweringId(q.id); setAnswerText(''); }}
                    className="mt-2 text-xs font-medium text-blue-600 hover:underline"
                  >
                    Odpowiedz
                  </button>
                )}
                {answeringId === q.id && (
                  <div className="mt-3 space-y-2">
                    <textarea
                      rows={2}
                      value={answerText}
                      onChange={(e) => setAnswerText(e.target.value)}
                      placeholder="Treść odpowiedzi…"
                      className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                    />
                    <div className="flex gap-2">
                      <button
                        disabled={!answerText.trim() || answeringQuestion}
                        onClick={() => handleAnswerQuestion(q.id)}
                        className="px-3 py-1.5 text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-60 rounded-lg"
                      >
                        {answeringQuestion ? 'Zapisywanie…' : 'Zapisz odpowiedź'}
                      </button>
                      <button
                        onClick={() => setAnsweringId(null)}
                        className="px-3 py-1.5 text-xs font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-lg"
                      >
                        Anuluj
                      </button>
                    </div>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}

        {canAskQuestion && (
          <form onSubmit={handleAskQuestion} className="mt-4 space-y-2 border-t border-gray-100 pt-4">
            <label className="block text-sm font-medium text-gray-700">
              Zadaj pytanie zamawiającemu
            </label>
            <textarea
              rows={2}
              required
              value={questionText}
              onChange={(e) => setQuestionText(e.target.value)}
              placeholder="Treść pytania…"
              className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
            />
            <button
              type="submit"
              disabled={!questionText.trim() || askingQuestion}
              className="px-4 py-2 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-60 rounded-lg"
            >
              {askingQuestion ? 'Wysyłanie…' : 'Zadaj pytanie'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
