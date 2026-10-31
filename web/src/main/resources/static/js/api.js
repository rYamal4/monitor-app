// { ok: true, data } или { ok: false, error } при ошибке формата; сетевые ошибки пробрасываются
export async function analyze(text) {
  const response = await fetch('api/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: text,
  });
  const body = await response.json().catch(() => null);
  if (response.ok && body?.status === 'ok') return { ok: true, data: body };
  if (body?.status === 'format_error') return { ok: false, error: body };
  throw new Error(`Сервер ответил ${response.status} ${response.statusText}`.trim());
}

// сохранённый ответ из static/mock, чтобы верстать без бэкенда: /?mock=05_long_record
export async function loadMock(name) {
  const response = await fetch(`mock/${encodeURIComponent(name)}.json`);
  if (!response.ok) throw new Error(`Нет файла mock/${name}.json`);
  const body = await response.json();
  return body.status === 'ok' ? { ok: true, data: body } : { ok: false, error: body };
}
