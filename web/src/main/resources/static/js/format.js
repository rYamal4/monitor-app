const numberFormat = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 3 });

export function formatNumber(value) {
  return value === null || value === undefined ? '—' : numberFormat.format(value);
}

// 1130 -> "18 мин 50 с", 3725 -> "1 ч 2 мин 5 с"
export function formatDuration(seconds) {
  if (!seconds) return '0 с';
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const rest = Math.round((seconds % 60) * 1000) / 1000;
  const parts = [];
  if (hours) parts.push(`${hours} ч`);
  if (minutes) parts.push(`${minutes} мин`);
  if (rest || parts.length === 0) parts.push(`${numberFormat.format(rest)} с`);
  return parts.join(' ');
}

export const STATE_LABELS = {
  low: 'Работа, низкое давление',
  normal: 'Работа, обычное давление',
  high: 'Работа, высокое давление',
  stopped: 'Остановка',
  gap: 'Пропуск',
  invalid: 'Недостоверные данные',
};

export function stateKey(segment) {
  return segment.state === 'working' ? segment.mode : segment.state;
}

export function token(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

export function stateColor(key) {
  return token(`--state-${key}`);
}
