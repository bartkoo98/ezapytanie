import { useEffect, useState } from 'react';
import { listAuditLogs } from '../api/adminApi';
import type { AuditLog } from '../types/api';

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('pl-PL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

export default function AdminAuditPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    listAuditLogs().then(setLogs).finally(() => setLoading(false));
  }, []);

  return (
    <div className="max-w-6xl mx-auto space-y-5">
      <div>
        <h1 className="text-xl font-bold text-gray-900">Logi audytowe</h1>
        <p className="text-sm text-gray-400">{logs.length} zdarzeń</p>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="p-6 space-y-3">
            {[1, 2, 3, 4].map((i) => <div key={i} className="h-12 bg-gray-100 rounded-lg animate-pulse" />)}
          </div>
        ) : logs.length === 0 ? (
          <p className="text-center py-16 text-sm text-gray-400">Brak logów audytowych.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">Czas</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">Aktor</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">Akcja</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">Typ encji</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">ID encji</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">IP</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {logs.map((log) => (
                <tr key={log.id} className="hover:bg-gray-50">
                  <td className="px-5 py-3 text-xs text-gray-500 whitespace-nowrap">{formatDate(log.timestamp)}</td>
                  <td className="px-4 py-3">
                    <p className="text-gray-800">{log.actorEmail}</p>
                    <p className="text-xs text-gray-400">{log.actorRole}</p>
                  </td>
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700">
                      {log.action}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{log.entityType}</td>
                  <td className="px-4 py-3 font-mono text-xs text-gray-400">…{log.entityId.slice(-8)}</td>
                  <td className="px-4 py-3 text-xs text-gray-400">{log.ipAddress}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
