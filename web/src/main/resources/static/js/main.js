import { analyze, loadMock } from './api.js';
import { renderChart } from './chart.js';
import { renderFailure, renderFormatError } from './errors.js';
import { formatNumber, STATE_LABELS, stateColor } from './format.js';
import { renderSegmentsTable, renderTotals } from './summary.js';

const el = (id) => document.getElementById(id);

const fileInput = el('file-input');
const errorBox = el('error');
const result = el('result');

fileInput.addEventListener('change', () => {
  if (fileInput.files.length) handleFile(fileInput.files[0]);
});

// файл можно просто перетащить на страницу
document.addEventListener('dragover', (event) => event.preventDefault());
document.addEventListener('drop', (event) => {
  event.preventDefault();
  const file = event.dataTransfer.files[0];
  if (file) handleFile(file);
});

// /?mock=05_long_record
const mock = new URLSearchParams(location.search).get('mock');
if (mock) {
  el('file-name').textContent = `mock/${mock}.json`;
  loadMock(mock).then(show).catch(showFailure);
}

async function handleFile(file) {
  el('file-name').textContent = file.name;
  try {
    show(await analyze(await file.text()));
  } catch (failure) {
    showFailure(failure);
  }
}

function show(outcome) {
  errorBox.hidden = true;
  result.hidden = true;
  if (!outcome.ok) {
    renderFormatError(errorBox, outcome.error);
    return;
  }

  const data = outcome.data;
  result.hidden = false;
  el('dataset-title').textContent = `Запись ${data.dataset_id} (точек: ${formatNumber(data.sample_count)})`;
  renderTotals(el('totals-body'), data.totals);
  renderSegmentsTable(el('segments-body'), data.segments);

  // меньше двух точек - нет ни одного интервала, рисовать нечего
  const hasIntervals = data.samples.length >= 2;
  el('chart-block').hidden = !hasIntervals;
  const note = el('chart-note');
  note.hidden = hasIntervals;
  if (hasIntervals) {
    renderLegend();
    renderChart(el('chart'), data);
  } else {
    note.textContent = data.samples.length === 0
      ? 'В записи нет точек, строить нечего.'
      : 'В записи одна точка, интервалов нет, поэтому все длительности 0.';
  }
}

function showFailure(failure) {
  result.hidden = true;
  renderFailure(errorBox, failure);
}

function renderLegend() {
  el('legend').replaceChildren(
    ...Object.entries(STATE_LABELS).map(([key, label]) => {
      const item = document.createElement('li');
      const swatch = document.createElement('span');
      swatch.className = 'swatch';
      swatch.style.background = stateColor(key);
      item.append(swatch, label);
      return item;
    }),
  );
}
