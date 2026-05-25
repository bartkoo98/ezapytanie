import { NavLink, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  FileText,
  ReceiptText,
  Users,
  CircleUser,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import type { UserRole } from '../types/api';

interface NavItem {
  icon: React.ElementType;
  label: string;
  to: string;
  roles: UserRole[];
}

const navItems: NavItem[] = [
  { icon: LayoutDashboard, label: 'Przegląd', to: '/', roles: ['ADMIN', 'CLIENT', 'CONTRACTOR'] },
  { icon: FileText, label: 'Zapytania', to: '/inquiries', roles: ['CLIENT', 'CONTRACTOR'] },
  { icon: ReceiptText, label: 'Moje oferty', to: '/offers', roles: ['CONTRACTOR'] },
  { icon: Users, label: 'Użytkownicy', to: '/admin/users', roles: ['ADMIN'] },
  { icon: CircleUser, label: 'Mój profil', to: '/profile', roles: ['CLIENT', 'CONTRACTOR'] },
];

export default function Sidebar() {
  const { user } = useAuth();
  const location = useLocation();

  const visible = navItems.filter((item) => user && item.roles.includes(user.role));

  return (
    <aside className="w-56 shrink-0 bg-white border-r border-gray-200 flex flex-col">
      <div className="px-5 py-5 border-b border-gray-100">
        <span className="text-xl font-bold">
          <span className="text-blue-600">e</span>
          <span className="text-gray-800">Zapytanie</span>
        </span>
      </div>

      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        {visible.map((item) => {
          const isActive =
            item.to === '/'
              ? location.pathname === '/'
              : location.pathname.startsWith(item.to);

          return (
            <NavLink
              key={item.label}
              to={item.to}
              className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-blue-50 text-blue-700'
                  : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
              }`}
            >
              <item.icon size={17} className={isActive ? 'text-blue-600' : 'text-gray-400'} />
              {item.label}
            </NavLink>
          );
        })}
      </nav>

      <div className="px-4 py-4 border-t border-gray-100">
        <p className="text-xs text-gray-400 text-center">eZapytanie © 2026</p>
      </div>
    </aside>
  );
}
