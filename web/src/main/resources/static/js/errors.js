const CATEGORY_LABELS = {
  invalid_json: 'файл не является корректным JSON',
  missing_field: 'нет обязательного поля',
  wrong_type: 'неверный тип значения',
  invalid_value: 'недопустимое значение',
  unsupported_version: 'неподдерживаемая версия формата',
  non_increasing_time: 'время не возрастает',
};

export function renderFormatError(container, error) {
  container.replaceChildren();

  const title = document.createElement('h2');
  title.textContent = 'Ошибка формата, файл не принят';

  const message = document.createElement('p');
  message.textContent = error.message;

  const details = document.createElement('dl');
  addRow(details, 'Причина', CATEGORY_LABELS[error.category] ?? error.category);
  if (error.path) addRow(details, 'Где', error.path, true);

  const note = document.createElement('p');
  note.className = 'muted';
  note.textContent = 'Итоги для такого файла не считаются.';

  container.append(title, message, details, note);
  container.hidden = false;
}

export function renderFailure(container, failure) {
  container.replaceChildren();
  const title = document.createElement('h2');
  title.textContent = 'Не удалось обработать файл';
  const message = document.createElement('p');
  message.textContent = failure.message;
  container.append(title, message);
  container.hidden = false;
}

function addRow(list, term, value, asCode = false) {
  const dt = document.createElement('dt');
  dt.textContent = term;
  const dd = document.createElement('dd');
  if (asCode) {
    const code = document.createElement('code');
    code.textContent = value;
    dd.append(code);
  } else {
    dd.textContent = value;
  }
  list.append(dt, dd);
}
