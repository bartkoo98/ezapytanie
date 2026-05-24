import type { InquiryStatus, OfferStatus } from '../types/api';

interface Props {
  status: InquiryStatus | OfferStatus;
  type?: 'inquiry' | 'offer';
}

export default function StatusBadge({ status, type = 'inquiry' }: Props) {
  let label = '';
  let className = 'bg-gray-100 text-gray-600';

  if (type === 'offer') {
    if (status === 'SUBMITTED')  { label = 'Złożona';   className = 'bg-blue-100 text-blue-700'; }
    else if (status === 'WITHDRAWN') { label = 'Wycofana';  className = 'bg-red-100 text-red-700'; }
    else if (status === 'SELECTED')  { label = 'Wybrana';   className = 'bg-green-100 text-green-700'; }
    else                             { label = 'Odrzucona'; }
  } else {
    if (status === 'PUBLISHED')  { label = 'Aktywne';        className = 'bg-green-100 text-green-700'; }
    else if (status === 'CLOSED')    { label = 'Zamknięte';     className = 'bg-blue-100 text-blue-700'; }
    else if (status === 'CANCELLED') { label = 'Anulowane';     className = 'bg-red-100 text-red-700'; }
    else                             { label = 'Zarchiwizowane'; }
  }

  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${className}`}>
      {label}
    </span>
  );
}
