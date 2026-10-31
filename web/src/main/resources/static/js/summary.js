import { formatDuration, formatNumber, STATE_LABELS, stateColor, stateKey } from './format.js';

export function renderTotals(body, totals) {
  const rows = [
    ['Работа', totals.working_s],
    ['низкое давление', totals.modes_s.low, 'low'],
    ['обычное давление', totals.modes_s.normal, 'normal'],
    ['высокое давление', totals.modes_s.high, 'high'],
    ['Остановка', totals.stopped_s],
    ['Неизвестно', totals.unknown_s],
    ['пропуски', totals.gap_s, 'gap'],
    ['недостоверные данные', totals.invalid_s, 'invalid'],
    ['Длительность записи', totals.span_s],
  ];

  body.replaceChildren(
    ...rows.map(([label, seconds, key]) => {
      const row = document.createElement('tr');
      const name = document.createElement('td');
      if (key) {
        row.className = 'sub';
        name.append(swatch(key));
      }
      name.append(label);
      row.append(name, cell(formatDuration(seconds)), cell(formatNumber(seconds), 'number'));
      return row;
    }),
  );
}

export function renderSegmentsTable(body, segments) {
  body.replaceChildren(
    ...segments.map((segment) => {
      const key = stateKey(segment);
      const row = document.createElement('tr');
      const state = document.createElement('td');
      state.append(swatch(key), STATE_LABELS[key]);
      row.append(
        cell(formatNumber(segment.start_s), 'number'),
        cell(formatNumber(segment.end_s), 'number'),
        cell(formatDuration(segment.end_s - segment.start_s)),
        state,
      );
      return row;
    }),
  );
}

function swatch(key) {
  const element = document.createElement('span');
  element.className = 'swatch';
  element.style.background = stateColor(key);
  return element;
}

function cell(text, className) {
  const td = document.createElement('td');
  if (className) td.className = className;
  td.textContent = text;
  return td;
}
