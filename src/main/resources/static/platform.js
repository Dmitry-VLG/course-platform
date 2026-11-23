'use strict';

const catalog = document.querySelector('#catalog');
const detail = document.querySelector('#detail');
const student = document.querySelector('#student');
const notice = document.querySelector('#notice');
let currentCourse;
let generation = 0;
let noticeTimer;

function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text != null) node.textContent = text;
  if (className) node.className = className;
  return node;
}

function message(text) {
  notice.textContent = text;
  notice.hidden = false;
  clearTimeout(noticeTimer);
  noticeTimer = setTimeout(() => { notice.hidden = true; }, 6500);
}

async function api(path, method = 'GET', body) {
  const res = await fetch('/api' + path, {
    method, headers: body ? {'Content-Type': 'application/json'} : {},
    body: body ? JSON.stringify(body) : undefined
  });
  if (res.status === 204) return;
  const data = await res.json();
  if (!res.ok) throw new Error(data.detail || 'Не удалось выполнить запрос');
  return data;
}

function action(label, handler) {
  const button = element('button', label);
  button.type = 'button';
  button.onclick = async () => {
    button.disabled = true;
    try { await handler(); } catch (e) { message(e.message); }
    finally { button.disabled = false; }
  };
  return button;
}

function taskBlock(task, work, enrolled, studentId) {
  const block = element('details');
  block.append(element('summary', task.title), element('p', task.description),
    element('p', 'Срок: ' + task.dueDate + ' · До ' + task.maxScore + ' баллов', 'meta'));
  if (work) {
    block.append(element('p', 'Ваше решение: ' + work.content));
    block.append(element('p', work.status === 'SUBMITTED' ? 'Ожидает проверки' : 'Оценка: ' + work.score + ' из ' + task.maxScore));
    if (work.feedback) block.append(element('p', work.feedback));
  } else if (enrolled) {
    const label = element('label', 'Текст решения');
    const answer = element('textarea');
    answer.required = true;
    label.append(answer);
    block.append(label, action('Отправить решение', async () => {
      if (!answer.reportValidity() || !answer.value.trim()) return;
      await api('/assignments/' + task.id + '/submit', 'POST', {studentId, content: answer.value});
      message('Решение отправлено');
      await showCourse(currentCourse);
    }));
  }
  return block;
}

async function renderQuiz(container, id, studentId) {
  const quiz = await api('/quizzes/' + id);
  const form = element('form');
  form.append(element('h4', quiz.title));
  if (quiz.timeLimit) form.append(element('p', 'Время по плану: ' + quiz.timeLimit + ' мин', 'meta'));
  for (const q of quiz.questions) {
    const field = element('fieldset');
    field.append(element('legend', q.text));
    for (const option of q.options) {
      const label = element('label');
      const input = element('input');
      input.type = q.type === 'SINGLE_CHOICE' ? 'radio' : 'checkbox';
      input.name = String(q.id);
      input.value = option.id;
      label.append(input, document.createTextNode(' ' + option.name));
      field.append(label);
    }
    form.append(field);
  }
  const send = action('Завершить тест', async () => {
    const data = new FormData(form);
    const answers = {};
    for (const q of quiz.questions) {
      answers[q.id] = data.getAll(String(q.id)).map(Number);
      if (!answers[q.id].length) throw new Error('Ответьте на вопрос «' + q.text + '»');
    }
    const result = await api('/quizzes/' + id + '/attempts', 'POST', {studentId, answers});
    message('Результат: ' + result.score + ' из ' + quiz.questions.length);
    await showCourse(currentCourse);
  });
  form.addEventListener('submit', e => e.preventDefault());
  form.append(send);
  container.replaceChildren(form);
}

async function showCourse(id) {
  currentCourse = id;
  const version = ++generation;
  detail.setAttribute('aria-busy', 'true');
  detail.replaceChildren(element('p', 'Загрузка курса…'));
  const studentId = Number(student.value);
  const forStudent = path => studentId ? api('/users/' + studentId + path) : Promise.resolve([]);
  const courseData = await Promise.all([
    api('/courses/' + id), forStudent('/courses'), forStudent('/submissions'), forStudent('/quiz-results')
  ]).catch(error => {
    if (version === generation) {
      detail.replaceChildren(element('p', 'Не удалось загрузить курс.'));
      detail.removeAttribute('aria-busy');
      throw error;
    }
  });
  if (version !== generation) return;
  const [data, registrations, works, results] = courseData;
  const c = data.course;
  const enrolled = registrations.some(e => e.course.id === id);
  detail.replaceChildren(element('p', c.category.name, 'meta'), element('h2', c.title));
  if (c.description) detail.append(element('p', c.description));
  detail.append(element('p', [c.teacher.name, c.duration, c.startDate].filter(Boolean).join(' · '), 'meta'));
  if (studentId) detail.append(action(enrolled ? 'Отписаться от курса' : 'Записаться на курс', async () => {
    await api('/courses/' + id + '/enroll?user=' + studentId, enrolled ? 'DELETE' : 'POST');
    await showCourse(currentCourse);
  }));
  for (const mod of data.modules) {
    const section = element('section');
    section.append(element('h3', mod.orderIndex + '. ' + mod.title));
    for (const lesson of mod.lessons) {
      section.append(element('h4', lesson.title), element('p', lesson.content));
      if (lesson.videoUrl && /^https?:\/\//i.test(lesson.videoUrl)) {
        const link = element('a', 'Видео к уроку');
        link.href = lesson.videoUrl;
        section.append(link);
      }
      for (const task of lesson.assignments) {
        section.append(taskBlock(task, works.find(w => w.assignmentId === task.id), enrolled, studentId));
      }
    }
    if (mod.quizId) {
      if (enrolled) {
        const quiz = element('div');
        quiz.append(action('Открыть тест', () => renderQuiz(quiz, mod.quizId, studentId)));
        section.append(quiz);
      }
      for (const result of results.filter(r => r.quizId === mod.quizId)) {
        section.append(element('p', new Date(result.takenAt).toLocaleString('ru-RU') + ' · Баллы: ' + result.score, 'meta'));
      }
    }
    detail.append(section);
  }
  detail.removeAttribute('aria-busy');
  for (const btn of catalog.children) btn.setAttribute('aria-current', String(Number(btn.dataset.id) === id));
}

async function init() {
  const [users, courses] = await Promise.all([api('/users'), api('/courses')]);
  for (const u of users.filter(u => u.role === 'STUDENT')) student.add(new Option(u.name, u.id));
  student.onchange = () => currentCourse && showCourse(currentCourse).catch(e => message(e.message));
  if (!courses.length) { detail.append(element('p', 'Курсов пока нет.')); return; }
  for (const c of courses) {
    const btn = action(c.title, () => showCourse(c.id));
    btn.dataset.id = c.id;
    catalog.append(btn);
  }
  await showCourse(courses[0].id);
}

init().catch(e => message(e.message));
