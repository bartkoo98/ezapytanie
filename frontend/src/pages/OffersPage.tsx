import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listMyOffers } from '../api/offerApi';
import type { OfferResponse } from '../types/api';
import StatusBadge from '../components/StatusBadge';

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatPrice(price: number | null, currency: string | null) {
  if (price === null) return <span className="text-gray-400 italic text-xs">zapieczętowana</span>;
  return new Intl.NumberFormat('pl-PL', { style: 'currency', currency: currency ?? 'PLN' }).format(price);
}

export default function OffersPage() {
  const [offers, setOffers] = useState<OfferResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    listMyOffers().then(setOffers).finally(() => setLoading(false));
  }, []);

  return (
    <div className="max-w-5xl mx-auto space-y-5">
      <div>
        <h1 className="text-xl font-bold text-gray-900">Moje oferty</h1>
        <p className="text-sm text-gray-400">{offers.length} złożonych ofert</p>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="p-6 space-y-3">
            {[1, 2, 3].map((i) => <div key={i} className="h-14 bg-gray-100 rounded-lg animate-pulse" />)}
          </div>
        ) : offers.length === 0 ? (
          <p className="text-center py-16 text-sm text-gray-400">Nie złożyłeś jeszcze żadnej oferty.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">Zapytanie</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">Cena</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">Status</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">Złożona</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {offers.map((offer) => (
                <tr key={offer.id} className="hover:bg-gray-50">
                  <td className="px-5 py-4 text-sm text-gray-800">
                    {offer.inquiryTitle}
                  </td>
                  <td className="px-4 py-4 font-semibold text-gray-900">
                    {formatPrice(offer.price, offer.currency)}
                  </td>
                  <td className="px-4 py-4">
                    <StatusBadge status={offer.status} type="offer" />
                  </td>
                  <td className="px-4 py-4 text-gray-500">{formatDate(offer.submittedAt)}</td>
                  <td className="px-4 py-4 text-right">
                    <Link
                      to={`/inquiries/${offer.inquiryId}`}
                      className="text-xs text-blue-600 hover:underline"
                    >
                      Zapytanie →
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
