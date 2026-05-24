import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Search, Clock, ChevronRight, X, AlertCircle } from 'lucide-react';
import { listInquiries, createInquiry } from '../api/inquiryApi';
import { useAuth } from '../context/AuthContext';
import type { InquiryResponse, InquiryStatus } from '../types/api';
import StatusBadge from '../components/StatusBadge';

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('pl-PL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

const STATUS_FILTERS: { value: InquiryStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'Wszystkie' },
  { value: 'PUBLISHED', label: 'Aktywne' },
  { value: 'CLOSED', label: 'Zamknięte' },
  { value: 'CANCELLED', label: 'Anulowane' },
  { value: 'ARCHIVED', label: 'Zarchiwizowane' },
];

interface CreateModalProps {
  onClose: () => void;
  onCreated: (inquiry: InquiryResponse) => void;
}

function CreateModal({ onClose, onCreated }: CreateModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('');
  const [deliveryLocation, setDeliveryLocation] = useState('');
  const [termsAndConditions, setTermsAndConditions] = useState('');
  const [deadline, setDeadline] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const created = await createInquiry({
        title,
        description,
        category,
        deliveryLocation: deliveryLocation || undefined,
        termsAndConditions: termsAndConditions || undefined,
        deadline: new Date(deadline).toISOString(),
      });
      onCreated(created);
    } catch (err: unknown) {
      setError((err as { message?: string })?.message ?? 'Nie udało się utworzyć zapytania.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
      <div className="w-full max-w-lg bg-white rounded-2xl shadow-xl p-6">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-semibold text-gray-800">Nowe zapytanie ofertowe</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X size={20} />
          </button>
        </div>

        {error && (
          <div className="mb-4 flex items-center gap-2 rounded-lg bg-red-50 border border-red-200 px-3 py-2.5 text-sm text-red-700">
            <AlertCircle size={15} className="shrink-0" />
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Tytuł</label>
            <input
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="np. Dostawa materiałów biurowych"
              className="w-full px-3 py-2.5 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Opis</label>
            <textarea
              required
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Szczegółowy opis przedmiotu zapytania…"
              className="w-full px-3 py-2.5 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Miejsce realizacji</label>
            <input
              type="text"
              value={deliveryLocation}
              onChange={(e) => setDeliveryLocation(e.target.value)}
              placeholder="np. Warszawa, ul. Przykładowa 1"
              className="w-full px-3 py-2.5 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Warunki realizacji i płatności</label>
            <textarea
              rows={2}
              value={termsAndConditions}
              onChange={(e) => setTermsAndConditions(e.target.value)}
              placeholder="np. Płatność 30 dni od dostawy, gwarancja 12 miesięcy…"
              className="w-full px-3 py-2.5 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Kategoria</label>
              <input
                required
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                placeholder="np. Dostawy"
                className="w-full px-3 py-2.5 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Termin składania ofert</label>
              <input
                type="datetime-local"
                required
                value={deadline}
                onChange={(e) => setDeadline(e.target.value)}
                min={new Date(Date.now() + 86_400_000).toISOString().slice(0, 16)}
                className="w-full px-3 py-2.5 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 text-sm font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
            >
              Anuluj
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 py-2.5 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-60 rounded-lg transition-colors"
            >
              {loading ? 'Tworzenie…' : 'Utwórz zapytanie'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function InquiryListPage() {
  const { user } = useAuth();
  const [inquiries, setInquiries] = useState<InquiryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<InquiryStatus | 'ALL'>('ALL');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    setLoading(true);
    listInquiries(currentPage)
      .then((data) => {
        setInquiries(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .finally(() => setLoading(false));
  }, [currentPage]);

  const filtered = inquiries.filter((i) => {
    const matchesStatus = statusFilter === 'ALL' || i.status === statusFilter;
    const matchesSearch =
      search === '' ||
      i.title.toLowerCase().includes(search.toLowerCase()) ||
      i.category.toLowerCase().includes(search.toLowerCase());
    return matchesStatus && matchesSearch;
  });

  const handleCreated = (created: InquiryResponse) => {
    setInquiries((prev) => [created, ...prev]);
    setShowCreateModal(false);
  };

  const handleStatusFilter = (value: InquiryStatus | 'ALL') => {
    setStatusFilter(value);
    setCurrentPage(0);
  };

  return (
    <div className="max-w-5xl mx-auto space-y-5">
      {showCreateModal && (
        <CreateModal onClose={() => setShowCreateModal(false)} onCreated={handleCreated} />
      )}

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">
            {user?.role === 'CONTRACTOR' ? 'Dostępne zapytania' : 'Moje zapytania'}
          </h1>
          <p className="text-sm text-gray-400">
            {totalElements} postępowań
          </p>
        </div>
        {user?.role === 'CLIENT' && (
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold rounded-lg transition-colors"
          >
            <Plus size={16} />
            Nowe zapytanie
          </button>
        )}
      </div>

      <div className="flex items-center gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Szukaj po tytule lub kategorii…"
            className="w-full pl-9 pr-4 py-2 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <div className="flex gap-1">
          {STATUS_FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => handleStatusFilter(f.value)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                statusFilter === f.value
                  ? 'bg-blue-600 text-white'
                  : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="p-6 space-y-3">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-14 bg-gray-100 rounded-lg animate-pulse" />
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-16 text-gray-400">
            <Clock size={32} className="mx-auto mb-2 opacity-40" />
            <p className="text-sm">
              {search || statusFilter !== 'ALL'
                ? 'Brak wyników dla wybranych filtrów.'
                : 'Brak zapytań. Utwórz pierwsze postępowanie.'}
            </p>
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">
                  Tytuł
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">
                  Kategoria
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">
                  Termin
                </th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filtered.map((inquiry) => (
                <tr key={inquiry.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-5 py-4">
                    <p className="font-medium text-gray-800 truncate max-w-xs">{inquiry.title}</p>
                    <p className="text-xs text-gray-400 mt-0.5">
                      Dodano {formatDate(inquiry.createdAt)}
                    </p>
                  </td>
                  <td className="px-4 py-4 text-gray-500">{inquiry.category}</td>
                  <td className="px-4 py-4">
                    <StatusBadge status={inquiry.status} />
                  </td>
                  <td className="px-4 py-4 text-gray-500 whitespace-nowrap">
                    {formatDate(inquiry.deadline)}
                  </td>
                  <td className="px-4 py-4 text-right">
                    <Link
                      to={`/inquiries/${inquiry.id}`}
                      className="inline-flex items-center gap-1 text-xs font-medium text-blue-600 hover:underline"
                    >
                      Szczegóły
                      <ChevronRight size={14} />
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-5 py-3 border-t border-gray-100">
            <button
              disabled={currentPage === 0}
              onClick={() => setCurrentPage(currentPage - 1)}
              className="px-3 py-1.5 text-sm font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 disabled:opacity-40 disabled:cursor-not-allowed rounded-lg"
            >
              Poprzednia
            </button>
            <span className="text-sm text-gray-500">
              Strona {currentPage + 1} z {totalPages}
            </span>
            <button
              disabled={currentPage >= totalPages - 1}
              onClick={() => setCurrentPage(currentPage + 1)}
              className="px-3 py-1.5 text-sm font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 disabled:opacity-40 disabled:cursor-not-allowed rounded-lg"
            >
              Następna
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
