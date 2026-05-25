import React, { createContext, useContext, useState } from 'react';
import type { LoginResponse, UserRole } from '../types/api';

interface AuthUser {
  email: string;
  role: UserRole;
  accessToken: string;
  fullName: string;
  institutionName: string;
}

interface AuthContextType {
  user: AuthUser | null;
  login: (response: LoginResponse) => void;
  logout: () => void;
  updateUser: (updates: Partial<AuthUser>) => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

function loadStoredUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem('authUser');
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadStoredUser);

  const login = (response: LoginResponse) => {
    const authUser: AuthUser = {
      email: response.email,
      role: response.role,
      accessToken: response.accessToken,
      fullName: response.fullName,
      institutionName: response.institutionName,
    };
    localStorage.setItem('authUser', JSON.stringify(authUser));
    localStorage.setItem('accessToken', response.accessToken);
    setUser(authUser);
  };

  const logout = () => {
    localStorage.removeItem('authUser');
    localStorage.removeItem('accessToken');
    setUser(null);
  };

  const updateUser = (updates: Partial<AuthUser>) => {
    setUser((prev) => {
      if (!prev) return prev;
      const next = { ...prev, ...updates };
      localStorage.setItem('authUser', JSON.stringify(next));
      return next;
    });
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, updateUser, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
