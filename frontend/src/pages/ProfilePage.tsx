import { useEffect, useState } from 'react';
import { CheckCircle, AlertCircle } from 'lucide-react';
import { getProfile, updateProfile } from '../api/userApi';
import { useAuth } from '../context/AuthContext';
import type { UserResponse } from '../types/api';

const roleLabel: Record<string, string> = {
  ADMIN: 'Administrator',
  CLIENT: 'Zamawiający',
  CONTRACTOR: 'Wykonawca',
};

function ReadOnlyField({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div>
      <label className="block text-xs font-medium text-gray-500 mb-1">{label}</label>
      <p className="px-3 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-sm text-gray-700">
        {value || '—'}
      </p>
    </div>
  );
}

export default function ProfilePage() {
  const { updateUser } = useAuth();

  const [profile, setProfile] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const [fullName, setFullName] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [address, setAddress] = useState('');

  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    getProfile()
      .then((data) => {
        setProfile(data);
        setFullName(data.fullName ?? '');
        setContactPhone(data.companyDetails?.contactPhone ?? '');
        setAddress(data.companyDetails?.address ?? '');
      })
      .catch(() => setError('Nie udało się załadować profilu.'))
      .finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    setSaving(true);
    try {
      const updated = await updateProfile({ fullName, contactPhone, address });
      setProfile(updated);
      setFullName(updated.fullName ?? '');
      setContactPhone(updated.companyDetails?.contactPhone ?? '');
      setAddress(updated.companyDetails?.address ?? '');
      updateUser({ fullName: updated.fullName });
      setSuccess(true);
    } catch (err: unknown) {
      setError((err as { message?: string })?.message ?? 'Nie udało się zapisać zmian.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto space-y-4 animate-pulse">
        <div className="h-6 bg-gray-200 rounded w-40" />
        <div className="h-64 bg-gray-100 rounded-xl" />
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="text-xl font-bold text-gray-900">Mój profil</h1>
        <p className="text-sm text-gray-400">Zarządzaj swoimi danymi osobowymi i kontaktowymi.</p>
      </div>

      {/* Read-only section */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
        <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wide">
          Dane rejestrowe (tylko do odczytu)
        </h2>
        <div className="grid grid-cols-2 gap-4">
          <ReadOnlyField label="Adres e-mail" value={profile?.email} />
          <ReadOnlyField label="Rola" value={roleLabel[profile?.role ?? ''] ?? profile?.role} />
          <ReadOnlyField label="Nazwa instytucji" value={profile?.institutionName} />
          <ReadOnlyField label="NIP" value={profile?.companyDetails?.nip} />
          <ReadOnlyField label="REGON" value={profile?.companyDetails?.regon} />
          <ReadOnlyField label="E-mail kontaktowy" value={profile?.companyDetails?.contactEmail} />
        </div>
      </div>

      {/* Editable section */}
      <form onSubmit={handleSubmit} className="bg-white rounded-xl border border-gray-200 p-6 space-y-5">
        <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wide">
          Dane edytowalne
        </h2>

        {success && (
          <div className="flex items-center gap-2 rounded-lg bg-green-50 border border-green-200 px-3 py-2.5 text-sm text-green-700">
            <CheckCircle size={15} className="shrink-0" />
            Profil został zaktualizowany pomyślnie.
          </div>
        )}

        {error && (
          <div className="flex items-center gap-2 rounded-lg bg-red-50 border border-red-200 px-3 py-2.5 text-sm text-red-700">
            <AlertCircle size={15} className="shrink-0" />
            {error}
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Imię i nazwisko</label>
          <input
            required
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            className="w-full px-3 py-2.5 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Numer telefonu</label>
          <input
            required
            value={contactPhone}
            onChange={(e) => setContactPhone(e.target.value)}
            placeholder="+48 000 000 000"
            className="w-full px-3 py-2.5 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Adres siedziby</label>
          <input
            required
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            placeholder="ul. Przykładowa 1, 00-001 Warszawa"
            className="w-full px-3 py-2.5 rounded-lg border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <div className="flex justify-end pt-1">
          <button
            type="submit"
            disabled={saving}
            className="px-5 py-2.5 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-60 rounded-lg transition-colors"
          >
            {saving ? 'Zapisywanie…' : 'Zapisz zmiany'}
          </button>
        </div>
      </form>
    </div>
  );
}
