import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Clock } from 'lucide-react';
import { listInquiries } from '../api/inquiryApi';
import { useAuth } from '../context/AuthContext';
import type { InquiryResponse } from '../types/api';
import StatusBadge from '../components/StatusBadge';

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const days = Math.floor(diff / 86_400_000);
  if (days === 0) return 'dzisiaj';
  if (days === 1) return 'wczoraj';
  if (days < 7) return `${days} dni temu`;
  if (days < 30) return `${Math.floor(days / 7)} tyg. temu`;
  return `${Math.floor(days / 30)} mies. temu`;
}

function statusDotColor(status: string): string {
  if (status === 'PUBLISHED') return 'bg-green-500';
  if (status === 'CLOSED') return 'bg-blue-500';
  if (status === 'CANCELLED') return 'bg-red-400';
  return 'bg-gray-400';
}

function formatDeadline(iso: string): string {
  const date = new Date(iso);
  const diff = date.getTime() - Date.now();
  const days = Math.ceil(diff / 86_400_000);
  if (days < 0) return 'Termin minął';
  if (days === 0) return 'Dzisiaj';
  if (days === 1) return 'Jutro';
  return `Za ${days} dni`;
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [inquiries, setInquiries] = useState<InquiryResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    listInquiries()
      .then(setInquiries)
      .finally(() => setLoading(false));
  }, []);

  const active = inquiries.filter((i) => i.status === 'PUBLISHED').length;

  const recent = [...inquiries]
    .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
    .slice(0, 5);

  const displayName = user?.institutionName || user?.email || '';

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Witaj, {displayName}</h1>
        {active > 0 && (
          <p className="mt-1 text-sm text-gray-500">Aktywne zapytania: {active}</p>
        )}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-base font-semibold text-gray-800">Ostatnia aktywność</h2>
            <p className="text-xs text-gray-400">Oś czasu Twoich postępowań</p>
          </div>
          <Link
            to="/inquiries"
            className="text-xs font-medium text-blue-600 hover:underline"
          >
            Zobacz wszystkie →
          </Link>
        </div>

        {loading ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-10 bg-gray-100 rounded-lg animate-pulse" />
            ))}
          </div>
        ) : recent.length === 0 ? (
          <div className="text-center py-10 text-gray-400">
            <Clock size={32} className="mx-auto mb-2 opacity-40" />
            <p className="text-sm">Brak postępowań. Utwórz pierwsze zapytanie.</p>
            {user?.role === 'CLIENT' && (
              <Link
                to="/inquiries"
                className="mt-3 inline-block text-sm text-blue-600 hover:underline"
              >
                Utwórz zapytanie →
              </Link>
            )}
          </div>
        ) : (
          <ul className="divide-y divide-gray-100">
            {recent.map((inquiry) => (
              <li key={inquiry.id}>
                <Link
                  to={`/inquiries/${inquiry.id}`}
                  className="flex items-center gap-3 py-3 hover:bg-gray-50 rounded-lg px-2 -mx-2 transition-colors"
                >
                  <span
                    className={`h-2 w-2 rounded-full shrink-0 ${statusDotColor(inquiry.status)}`}
                  />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-800 truncate">{inquiry.title}</p>
                    <p className="text-xs text-gray-400">
                      {inquiry.category} · {timeAgo(inquiry.updatedAt)}
                    </p>
                  </div>
                  <div className="flex items-center gap-3 shrink-0">
                    <StatusBadge status={inquiry.status} />
                    {inquiry.status === 'PUBLISHED' && (
                      <span className="text-xs text-gray-400">{formatDeadline(inquiry.deadline)}</span>
                    )}
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
