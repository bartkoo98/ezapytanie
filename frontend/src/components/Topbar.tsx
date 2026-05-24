import { LogOut } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const roleLabel: Record<string, string> = {
  ADMIN: 'Administrator',
  CLIENT: 'Zamawiający',
  CONTRACTOR: 'Wykonawca',
};

function getInitials(name: string): string {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

export default function Topbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!user) return null;

  const initials = getInitials(user.fullName || user.email);
  const role = roleLabel[user.role] ?? user.role;

  return (
    <header className="h-14 bg-white border-b border-gray-200 flex items-center justify-end px-6 shrink-0">
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2.5">
          <div className="h-8 w-8 rounded-full bg-blue-600 flex items-center justify-center text-white text-xs font-semibold">
            {initials}
          </div>
          <div className="text-sm leading-tight">
            <p className="font-medium text-gray-800">{user.institutionName || user.email}</p>
            <p className="text-xs text-gray-400">{role}</p>
          </div>
        </div>

        <button
          onClick={handleLogout}
          className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-red-600 transition-colors"
          title="Wyloguj się"
        >
          <LogOut size={16} />
        </button>
      </div>
    </header>
  );
}
