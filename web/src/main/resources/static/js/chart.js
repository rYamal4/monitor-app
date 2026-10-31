import { formatDuration, formatNumber, STATE_LABELS, stateColor, stateKey, token } from './format.js';

const echarts = window.echarts;

// положение полос в px, рассчитано на высоту .chart = 600px
const LAYOUT = {
  pressure: { top: 40, height: 175 },
  rpm: { top: 262, height: 175 },
  state: { top: 482, height: 26 },
  zoom: { bottom: 10, height: 22 },
};

let chart = null;

export function renderChart(container, data) {
  if (!chart) {
    chart = echarts.init(container);
    window.addEventListener('resize', () => chart.resize());
  }
  chart.resize();
  chart.setOption(buildOption(data), true);
}

function buildOption({ samples, segments }) {
  const c = {
    series: token('--series-1'),
    text: token('--text'),
    muted: token('--text-muted'),
    grid: token('--grid'),
    axis: token('--axis'),
    background: token('--background'),
    border: token('--border'),
    washGap: token('--wash-gap'),
    washInvalid: token('--wash-invalid'),
  };
  const tMin = samples[0].t_s;
  const tMax = samples[samples.length - 1].t_s;
  const span = tMax - tMin;

  // после точки, с которой начинается пропуск, ставим null, чтобы линия оборвалась
  const gapStarts = new Set(segments.filter((s) => s.state === 'gap').map((s) => s.start_s));
  const lineData = (field) => {
    const points = [];
    for (const sample of samples) {
      points.push([sample.t_s, sample[field]]);
      if (gapStarts.has(sample.t_s)) points.push([sample.t_s, null]);
    }
    return points;
  };

  const shaded = segments.filter((s) => s.state === 'gap' || s.state === 'invalid');
  const markArea = (withLabels) => ({
    silent: true,
    data: shaded.map((s) => [
      {
        xAxis: s.start_s,
        itemStyle: { color: s.state === 'gap' ? c.washGap : c.washInvalid },
        label: {
          show: withLabels && s.end_s - s.start_s >= span * 0.03, // узкие участки не подписываем
          position: 'insideTop',
          color: c.text,
          fontSize: 11,
          formatter: `${s.state === 'gap' ? 'пропуск' : 'недостоверно'}\n${formatDuration(s.end_s - s.start_s)}`,
        },
      },
      { xAxis: s.end_s },
    ]),
  });

  const lineSeries = (name, field, axisIndex, withLabels) => ({
    name,
    type: 'line',
    xAxisIndex: axisIndex,
    yAxisIndex: axisIndex,
    data: lineData(field),
    step: 'end', // значение держится до следующей точки
    showSymbol: false,
    symbolSize: 8,
    connectNulls: false,
    lineStyle: { width: 2, color: c.series },
    itemStyle: { color: c.series },
    emphasis: { disabled: true },
    markArea: markArea(withLabels),
  });

  const keys = segments.map(stateKey);
  const stateSeries = {
    name: 'Состояние',
    type: 'custom',
    xAxisIndex: 2,
    yAxisIndex: 2,
    data: segments.map((s) => [s.start_s, s.end_s]),
    encode: { x: [0, 1] },
    renderItem: (params, api) => {
      const [x0, top] = api.coord([api.value(0), 1]);
      const [x1, bottom] = api.coord([api.value(1), 0]);
      const width = x1 - x0;
      const rect = echarts.graphic.clipRectByRect(
        { x: x0, y: top, width: width > 3 ? width - 2 : Math.max(width, 1), height: bottom - top },
        params.coordSys,
      );
      return rect && { type: 'rect', shape: rect, style: { fill: stateColor(keys[params.dataIndex]) } };
    },
  };

  const xAxis = (gridIndex) => ({
    type: 'value',
    gridIndex,
    min: tMin,
    max: tMax,
    axisLine: { lineStyle: { color: c.axis } },
    axisTick: { show: false },
    splitLine: { show: gridIndex !== 2, lineStyle: { color: c.grid } },
    axisLabel: { show: gridIndex === 2, color: c.muted, formatter: (v) => formatNumber(v) },
    name: gridIndex === 2 ? 'время, с' : '',
    nameLocation: 'middle',
    nameGap: 26,
    nameTextStyle: { color: c.muted },
  });

  const yAxis = (gridIndex) => ({
    type: 'value',
    gridIndex,
    axisLine: { show: false },
    splitLine: { lineStyle: { color: c.grid } },
    axisLabel: { color: c.muted, formatter: (v) => formatNumber(v) },
  });

  const title = (text, top) => ({ text, top, left: 4, textStyle: { color: c.text, fontSize: 13, fontWeight: 600 } });
  const grid = ({ top, height }) => ({ top, height, left: 56, right: 24 });

  return {
    animation: false,
    textStyle: { fontFamily: 'system-ui, -apple-system, "Segoe UI", sans-serif' },
    title: [
      title('Давление, МПа', LAYOUT.pressure.top - 32),
      title('Обороты, об/мин', LAYOUT.rpm.top - 32),
      title('Состояние', LAYOUT.state.top - 26),
    ],
    grid: [grid(LAYOUT.pressure), grid(LAYOUT.rpm), grid(LAYOUT.state)],
    xAxis: [xAxis(0), xAxis(1), xAxis(2)],
    yAxis: [yAxis(0), yAxis(1), { type: 'value', gridIndex: 2, min: 0, max: 1, show: false }],
    axisPointer: { link: [{ xAxisIndex: 'all' }], lineStyle: { color: c.axis } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line' },
      backgroundColor: c.background,
      borderColor: c.border,
      textStyle: { color: c.text, fontSize: 13 },
      formatter: (params) => tooltip(params, samples, segments),
    },
    dataZoom: [
      { type: 'inside', xAxisIndex: [0, 1, 2], filterMode: 'none' },
      {
        type: 'slider',
        xAxisIndex: [0, 1, 2],
        filterMode: 'none',
        bottom: LAYOUT.zoom.bottom,
        height: LAYOUT.zoom.height,
        borderColor: c.border,
        textStyle: { color: c.muted },
        labelFormatter: (v) => `${formatNumber(Math.round(v))} с`,
      },
    ],
    series: [lineSeries('Давление', 'pressure_mpa', 0, true), lineSeries('Обороты', 'rpm', 1, false), stateSeries],
  };
}

function tooltip(params, samples, segments) {
  const t = params.length ? params[0].axisValue : null;
  if (t === null || t === undefined) return '';

  const segment = segments.find((s) => t >= s.start_s && t < s.end_s) ?? segments[segments.length - 1];
  const key = stateKey(segment);
  const header = `<b>${formatNumber(t)} с</b> (${formatDuration(t)})`;
  const state = `<span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:${stateColor(key)};margin-right:6px"></span>${STATE_LABELS[key]}`;

  if (segment.state === 'gap') {
    return `${header}<br>${state}, данных нет`;
  }
  const sample = sampleAt(samples, t);
  return (
    `${header}<br>` +
    `Давление: <b>${formatNumber(sample.pressure_mpa)}</b> МПа<br>` +
    `Обороты: <b>${formatNumber(sample.rpm)}</b> об/мин<br>` +
    (sample.valid ? '' : 'valid = false<br>') +
    state
  );
}

// последняя точка с t_s <= t
function sampleAt(samples, t) {
  let low = 0;
  let high = samples.length - 1;
  while (low < high) {
    const mid = Math.ceil((low + high) / 2);
    if (samples[mid].t_s <= t) low = mid;
    else high = mid - 1;
  }
  return samples[low];
}
