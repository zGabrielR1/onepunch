import { useEffect, useId, useRef, useState } from "react";
import {
  ArrowUpRight,
  ArrowRight,
  ArrowLeft,
  Plus,
  Play,
  Check,
  ChevronRight,
  ChevronDown,
  X,
  Trash2,
  Search,
  Dumbbell,
  LayoutDashboard,
  CalendarDays,
  BookOpen,
  Clock3,
  Flame,
  Target,
  MoreHorizontal,
  CloudUpload,
  WifiOff,
  Pause,
  SkipForward,
  CheckCircle2,
  Timer,
  GripVertical,
  TrendingUp,
  Pencil,
  RotateCcw,
} from "lucide-react";
import catalog from "../../shared/catalog.json";
import seeds from "../../shared/routines.json";
import { filterExercises } from "./lib/catalog.mjs";
import {
  STORAGE_KEY,
  id,
  emptyState,
  readState,
  validSet,
  completedSets,
  isAssisted,
  loadUnit,
  volume,
  routineValid,
  latestSets,
  startWorkout,
  addExercise,
  toggleSet,
  adjustRest,
  startRest,
  finishWorkout,
  mergeSynced,
  fmt,
  clock,
  muscleColor,
  weekDays,
  dayKey,
} from "./lib/model.mjs";
const brand = __APP_BRAND__;
const exerciseById = Object.fromEntries(catalog.map((e) => [e.id, e]));
const groups = [
  "Todos",
  "Peito",
  "Costas",
  "Pernas",
  "Ombros",
  "Braços",
  "Core",
];
const nav = [
  ["home", "Visão geral", LayoutDashboard],
  ["routines", "Meus treinos", Dumbbell],
  ["exercises", "Exercícios", BookOpen],
  ["history", "Histórico", CalendarDays],
];
const normalize = (s) =>
  s
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
const date = (t) =>
  new Date(t).toLocaleDateString("pt-BR", { day: "numeric", month: "short" });
function Glyph({ muscle = "Peito", size = 48 }) {
  return (
    <span
      className="exercise-glyph"
      style={{ width: size, height: size, background: muscleColor(muscle) }}
    >
      <svg viewBox="0 0 60 64" fill="none" aria-hidden="true">
        <circle cx="30" cy="10" r="5" fill="#424a40" />
        <path
          d="M20 20 14 25 10 41M40 20 46 25 50 41M22 21 21 37 24 56M38 21 39 37 36 56M22 21h16M24 37h12"
          stroke="#424a40"
          strokeWidth="4.5"
          strokeLinecap="round"
        />
        {muscle === "Peito" ? (
          <path d="M22 21h16v9H22z" fill="#608b3d" />
        ) : muscle === "Pernas" ? (
          <path
            d="m22 37 3 18m13-18-3 18"
            stroke="#9b7845"
            strokeWidth="6"
            strokeLinecap="round"
          />
        ) : muscle === "Ombros" ? (
          <>
            <circle cx="21" cy="22" r="5" fill="#8b6bab" />
            <circle cx="39" cy="22" r="5" fill="#8b6bab" />
          </>
        ) : muscle === "Braços" ? (
          <path
            d="m15 26-4 13m34-13 4 13"
            stroke="#aa7064"
            strokeWidth="6"
            strokeLinecap="round"
          />
        ) : muscle === "Core" ? (
          <rect x="24" y="29" width="12" height="11" rx="3" fill="#568d80" />
        ) : (
          <path d="M22 21h16l-4 13h-8z" fill="#6383aa" />
        )}
      </svg>
    </span>
  );
}
function Modal({ title, onClose, children, wide = false }) {
  const ref = useRef(null);
  useEffect(() => {
    ref.current.showModal();
    return () => ref.current?.close();
  }, []);
  return (
    <dialog
      ref={ref}
      className={wide ? "modal wide" : "modal"}
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="modal-head">
        <h2>{title}</h2>
        <button
          className="icon-btn"
          aria-label="Fechar janela"
          onClick={onClose}
        >
          <X size={20} />
        </button>
      </div>
      {children}
    </dialog>
  );
}
function EquipmentFilter({ value, onChange }) {
  const selectId = useId();
  return (
    <div className="equipment-filter">
      <label htmlFor={selectId}>Equipamento</label>
      <select
        id={selectId}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        <option value="Todos">Todos os equipamentos</option>
        {[...new Set(catalog.map((exercise) => exercise.equipment))]
          .sort()
          .map((equipment) => (
            <option key={equipment} value={equipment}>
              {equipment}
            </option>
          ))}
      </select>
    </div>
  );
}
function ExercisePicker({ onSelect, onClose, existing = [] }) {
  const [search, setSearch] = useState("");
  const [group, setGroup] = useState("Todos");
  const [equipment, setEquipment] = useState("Todos");
  const shown = filterExercises(catalog, search, group, equipment);
  return (
    <Modal title="Adicionar exercício" onClose={onClose}>
      <label className="search-field">
        <Search size={18} />
        <input
          autoFocus
          aria-label="Buscar exercício para adicionar"
          placeholder="Nome, músculo ou equipamento"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </label>
      <div className="chip-row">
        {groups.map((g) => (
          <button
            key={g}
            className={g === group ? "chip on" : "chip"}
            aria-pressed={g === group}
            onClick={() => setGroup(g)}
          >
            {g}
          </button>
        ))}
      </div>
      <EquipmentFilter value={equipment} onChange={setEquipment} />
      <div className="filter-summary">
        <span role="status">
          {shown.length} de {catalog.length} exercícios
        </span>
        {search || group !== "Todos" || equipment !== "Todos" ? (
          <button
            className="text-button"
            onClick={() => {
              setSearch("");
              setGroup("Todos");
              setEquipment("Todos");
            }}
          >
            Limpar filtros
          </button>
        ) : null}
      </div>
      <div className="picker-list">
        {shown.map((e) => (
          <button
            key={e.id}
            className="exercise-list-row"
            disabled={existing.includes(e.id)}
            onClick={() => onSelect(e.id)}
          >
            <Glyph muscle={e.muscle} />
            <span>
              <strong>{e.name}</strong>
              <small>
                {e.muscle} · {e.equipment}
                {existing.includes(e.id) ? " · Já adicionado" : ""}
              </small>
            </span>
            {existing.includes(e.id) ? <Check size={18} /> : <Plus size={18} />}
          </button>
        ))}
        {!shown.length ? (
          <p className="empty-text">
            Nenhum exercício encontrado. Tente outro termo ou grupo muscular.
          </p>
        ) : null}
      </div>
    </Modal>
  );
}
function RoutineEditor({ routine, onSave, onClose }) {
  const [draft, setDraft] = useState(() =>
    structuredClone(
      routine || {
        id: id(),
        name: "",
        note: "",
        exercises: [],
        updatedAt: Date.now(),
        archived: false,
      },
    ),
  );
  const [picking, setPicking] = useState(false);
  const update = (index, key, value) =>
    setDraft((s) => ({
      ...s,
      exercises: s.exercises.map((e, i) =>
        i === index ? { ...e, [key]: +value } : e,
      ),
    }));
  return (
    <>
      <Modal
        title={routine ? "Editar rotina" : "Criar rotina"}
        onClose={onClose}
        wide
      >
        <div className="editor-fields">
          <label>
            Nome da rotina
            <input
              autoFocus
              placeholder="Ex.: Superior A"
              maxLength={80}
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
            />
          </label>
          <label>
            Descrição <span className="muted">(opcional)</span>
            <input
              placeholder="Qual é o foco deste treino?"
              maxLength={200}
              value={draft.note}
              onChange={(e) => setDraft({ ...draft, note: e.target.value })}
            />
          </label>
        </div>
        <div className="editor-exercises">
          {draft.exercises.map((spec, index) => (
            <div className="editor-exercise" key={spec.exerciseId}>
              <div className="editor-exercise-top">
                <Glyph
                  muscle={exerciseById[spec.exerciseId].muscle}
                  size={40}
                />
                <strong>{exerciseById[spec.exerciseId].name}</strong>
                <button
                  className="icon-btn"
                  disabled={index === 0}
                  aria-label={
                    "Mover para cima: " + exerciseById[spec.exerciseId].name
                  }
                  onClick={() =>
                    setDraft((s) => {
                      const exercises = [...s.exercises];
                      [exercises[index - 1], exercises[index]] = [
                        exercises[index],
                        exercises[index - 1],
                      ];
                      return { ...s, exercises };
                    })
                  }
                >
                  <ArrowLeft size={16} className="rotate" />
                </button>
                <button
                  className="icon-btn danger"
                  aria-label={
                    "Remover exercício: " + exerciseById[spec.exerciseId].name
                  }
                  onClick={() =>
                    setDraft({
                      ...draft,
                      exercises: draft.exercises.filter((_, i) => i !== index),
                    })
                  }
                >
                  <X size={16} />
                </button>
              </div>
              <div className="spec-fields">
                <label>
                  Séries
                  <input
                    type="number"
                    min="1"
                    max="20"
                    value={spec.sets}
                    onChange={(e) => update(index, "sets", e.target.value)}
                  />
                </label>
                <label>
                  Repetições
                  <input
                    type="number"
                    min="1"
                    max="1000"
                    value={spec.reps}
                    onChange={(e) => update(index, "reps", e.target.value)}
                  />
                </label>
                <label>
                  Descanso
                  <select
                    value={spec.restSeconds}
                    onChange={(e) =>
                      update(index, "restSeconds", e.target.value)
                    }
                  >
                    {[0, 30, 60, 90, 120, 180, 300].map((t) => (
                      <option value={t} key={t}>
                        {t ? t + " s" : "Sem timer"}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
            </div>
          ))}
        </div>
        <button
          className="button outline full"
          onClick={() => setPicking(true)}
        >
          <Plus size={18} />
          Adicionar exercício
        </button>
        <div className="modal-footer">
          <p className="muted">Defina ao menos um exercício para salvar.</p>
          <button
            className="button dark"
            disabled={!routineValid(draft)}
            onClick={() =>
              onSave({
                ...draft,
                name: draft.name.trim(),
                updatedAt: Date.now(),
              })
            }
          >
            Salvar rotina
            <Check size={17} />
          </button>
        </div>
      </Modal>
      {picking ? (
        <ExercisePicker
          existing={draft.exercises.map((e) => e.exerciseId)}
          onClose={() => setPicking(false)}
          onSelect={(exerciseId) => {
            setDraft({
              ...draft,
              exercises: [
                ...draft.exercises,
                { exerciseId, sets: 3, reps: 10, restSeconds: 90 },
              ],
            });
            setPicking(false);
          }}
        />
      ) : null}
    </>
  );
}
function WorkoutDetail({ workout, onClose }) {
  const ids = [...new Set(workout.sets.map((s) => s.exerciseId))];
  return (
    <Modal title={workout.title} onClose={onClose}>
      <div className="summary-stats">
        <span>
          <strong>{clock((workout.endedAt - workout.startedAt) / 1000)}</strong>
          <small>Duração</small>
        </span>
        <span>
          <strong>{workout.sets.length}</strong>
          <small>Séries</small>
        </span>
        <span>
          <strong>{fmt(volume(workout))} kg</strong>
          <small>Volume</small>
        </span>
      </div>
      <p className="muted">
        {new Date(workout.endedAt).toLocaleString("pt-BR")}
      </p>
      {ids.map((exerciseId) => (
        <section className="history-exercise" key={exerciseId}>
          <h3>{exerciseById[exerciseId]?.name || exerciseId}</h3>
          {workout.sets
            .filter((s) => s.exerciseId === exerciseId)
            .map((s, i) => (
              <div key={s.id}>
                <span>Série {i + 1}</span>
                <strong>
                  {fmt(s.weight)} {loadUnit(s.exerciseId)} × {s.reps}
                </strong>
                <Check size={15} />
              </div>
            ))}
        </section>
      ))}
      {workout.note ? <p className="workout-note">{workout.note}</p> : null}
    </Modal>
  );
}
const RoutineCard = ({ routine, index = 0, onEdit, onStart }) => (
  <article className="routine-card">
    <div className="routine-card-top">
      <span className="routine-number">
        {String(index + 1).padStart(2, "0")}
      </span>
      <button
        className="icon-btn"
        aria-label={"Editar " + routine.name}
        onClick={() => onEdit(routine)}
      >
        <Pencil size={17} />
      </button>
    </div>
    <h3>{routine.name}</h3>
    <p>{routine.note || "Sua rotina, do seu jeito."}</p>
    <div className="routine-muscles">
      {[
        ...new Set(
          routine.exercises.map((e) => exerciseById[e.exerciseId]?.muscle),
        ),
      ].map((m) => (
        <span key={m} style={{ background: muscleColor(m) }}>
          {m}
        </span>
      ))}
    </div>
    <ul className="routine-preview">
      {routine.exercises.slice(0, 3).map((e) => (
        <li key={e.exerciseId}>
          <span>{exerciseById[e.exerciseId]?.name}</span>
          <b>
            {e.sets} × {e.reps}
          </b>
        </li>
      ))}
      {routine.exercises.length > 3 ? (
        <li className="muted">+{routine.exercises.length - 3} exercício</li>
      ) : null}
    </ul>
    <div className="routine-meta">
      <span>
        <Dumbbell size={14} />
        {routine.exercises.length} exercícios
      </span>
      <span>{routine.exercises.reduce((n, e) => n + e.sets, 0)} séries</span>
    </div>
    <button className="button dark full" onClick={() => onStart(routine)}>
      Começar treino
      <ArrowUpRight size={18} />
    </button>
  </article>
);
function App() {
  const [initial] = useState(() => {
    try {
      return { state: readState(localStorage.getItem(STORAGE_KEY), seeds) };
    } catch (error) {
      return { state: emptyState(seeds), error: String(error) };
    }
  });
  const [state, setState] = useState(initial.state);
  const stateRef = useRef(state);
  stateRef.current = state;
  const [view, setView] = useState("home");
  const [modal, setModal] = useState(null);
  const [toast, setToast] = useState("");
  const [storageError, setStorageError] = useState("");
  const [search, setSearch] = useState("");
  const [group, setGroup] = useState("Todos");
  const [equipment, setEquipment] = useState("Todos");
  const libraryExercises = filterExercises(catalog, search, group, equipment);
  const [now, setNow] = useState(Date.now());
  const [syncing, setSyncing] = useState(false);
  const [progressExercise, setProgressExercise] = useState("");
  useEffect(() => {
    if (initial.error) return;
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
      setStorageError("");
    } catch {
      setStorageError(
        "Não foi possível salvar neste navegador. Libere espaço antes de fechar o app.",
      );
    }
  }, [state]);
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(""), 4500);
    return () => clearTimeout(timer);
  }, [toast]);
  const routines = state.routines.filter((r) => !r.archived);
  const active = state.active;
  const days = weekDays(state.workouts, now);
  const weekWorkouts = state.workouts.filter((w) =>
    days.some((d) => dayKey(d.time) === dayKey(w.endedAt)),
  );
  const totalSets = weekWorkouts.reduce(
    (n, w) => n + completedSets(w).length,
    0,
  );
  const totalVolume = weekWorkouts.reduce((n, w) => n + volume(w), 0);
  const begin = (routine) => {
    if (active) {
      setToast("Seu treino em andamento foi retomado.");
      setView("session");
      return;
    }
    setState((s) => ({ ...s, active: startWorkout(routine, s.workouts) }));
    setView("session");
    window.scrollTo(0, 0);
  };
  const updateActive = (fn) =>
    setState((s) => ({ ...s, active: fn(s.active) }));
  const saveRoutine = (r) => {
    setState((s) => ({
      ...s,
      routines: s.routines.some((x) => x.id === r.id)
        ? s.routines.map((x) => (x.id === r.id ? r : x))
        : [...s.routines, r],
    }));
    setModal(null);
    setToast("Rotina salva. Pronta para seu próximo treino.");
  };
  const sync = async () => {
    setSyncing(true);
    try {
      const response = await fetch("/api/sync", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          routines: state.routines,
          workouts: state.workouts,
        }),
        signal: AbortSignal.timeout(12000),
      });
      const data = await response.json();
      if (!response.ok)
        throw Error(data.error || "Não foi possível sincronizar.");
      if (!Array.isArray(data.routines) || !Array.isArray(data.workouts))
        throw Error("Resposta inválida do servidor.");
      // Merge against the current workspace after the request, including any
      // workout finished while waiting. Validate here so errors reach this catch
      // instead of escaping from a deferred React state updater.
      const merged = mergeSynced(stateRef.current, data);
      setState(merged);
      setToast("Tudo sincronizado. Seu treino em andamento continua aqui.");
    } catch (error) {
      setToast(
        error.message?.startsWith("Conflito no histórico:")
          ? error.message
          : "Não foi possível sincronizar. Seus treinos continuam salvos neste dispositivo.",
      );
    } finally {
      setSyncing(false);
    }
  };
  const pageNav = (next) => {
    setView(next);
    setSearch("");
    window.scrollTo(0, 0);
  };

  if (initial.error)
    return (
      <main className="recovery">
        <Dumbbell />
        <h1>Seus dados precisam de atenção.</h1>
        <p>
          Não sobrescrevemos o registro local porque seu formato não pôde ser
          lido. Guarde o conteúdo da chave {STORAGE_KEY} antes de iniciar um
          novo espaço.
        </p>
        <button
          className="button outline"
          onClick={() => {
            navigator.clipboard
              ?.writeText(localStorage.getItem(STORAGE_KEY) || "")
              .then(() => setToast("Registro copiado."));
          }}
        >
          Copiar registro para recuperação
        </button>
        <p>
          Para preservar seus dados, este espaço não será reiniciado
          automaticamente.
        </p>
      </main>
    );
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a
          href="#inicio"
          className="brand"
          onClick={(e) => {
            e.preventDefault();
            pageNav("home");
          }}
        >
          <span className="brand-mark">
            <Dumbbell size={24} />
          </span>
          {brand.name}
          <span className="brand-dot">.</span>
        </a>
        <span className="sidebar-label">Seu espaço de treino</span>
        <nav aria-label="Navegação principal">
          {nav.map(([key, label, Icon]) => (
            <button
              key={key}
              className={view === key ? "nav-item active" : "nav-item"}
              aria-current={view === key ? "page" : undefined}
              onClick={() => pageNav(key)}
            >
              <Icon size={19} />
              <span>{label}</span>
              {view === key ? <span className="nav-dot" /> : null}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="offline-status">
            <span />
            Pronto, mesmo offline
          </div>
          <div className="profile">
            <span className="avatar">EU</span>
            <div>
              <strong>Seu espaço</strong>
              <small>Seu treino. Seu ritmo.</small>
            </div>
          </div>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <div className="mobile-brand">
            <Dumbbell size={21} />
            <strong>{brand.name}.</strong>
          </div>
          <div className="breadcrumb">
            Seu espaço <ChevronRight size={13} />
            <span>
              {view === "session"
                ? "Treino em andamento"
                : nav.find((n) => n[0] === view)?.[1]}
            </span>
          </div>
          <div className="top-actions">
            <button
              className="sync-btn"
              disabled={syncing}
              onClick={sync}
              aria-label="Sincronizar treinos"
              title={
                state.lastSyncedAt
                  ? "Última sincronização: " +
                    new Date(state.lastSyncedAt).toLocaleString("pt-BR")
                  : "Sincronização com seu servidor pessoal"
              }
            >
              <CloudUpload size={18} />
              <span>{syncing ? "Sincronizando…" : "Sincronizar"}</span>
            </button>
            <span className="top-avatar">EU</span>
          </div>
        </header>
        {storageError ? (
          <div className="storage-warning" role="alert">
            {storageError}
          </div>
        ) : null}
        <main
          className={
            view === "session" ? "main-content training-page" : "main-content"
          }
        >
          {view === "home" ? (
            <>
              <div className="page-heading">
                <div>
                  <p className="eyebrow">
                    {new Date(now).toLocaleDateString("pt-BR", {
                      weekday: "long",
                      day: "numeric",
                      month: "long",
                    })}
                  </p>
                  <h1>
                    Vamos fazer acontecer<span>.</span>
                  </h1>
                  <p className="page-description">
                    O próximo passo é seu. A gente cuida do registro.
                  </p>
                </div>
                <button className="button dark" onClick={() => begin(null)}>
                  <Plus size={18} />
                  Treino livre
                </button>
              </div>
              <section className="dashboard-top">
                <div className="hero">
                  <div className="hero-copy">
                    <h2>
                      Força é<br />
                      constância.
                    </h2>
                    <p>
                      Um lugar para planejar, registrar
                      <br />e enxergar sua evolução.
                    </p>
                    <button
                      className="button light"
                      onClick={() => pageNav("routines")}
                    >
                      Escolher meu treino
                      <ArrowRight size={18} />
                    </button>
                  </div>
                  <div className="hero-art" aria-hidden="true">
                    <span className="orbit orbit-one" />
                    <span className="orbit orbit-two" />
                    <div className="weight-graphic">
                      <i />
                      <b />
                      <em />
                      <strong />
                      <span />
                    </div>
                  </div>
                </div>
                <div className="week-card">
                  <div className="section-line">
                    <h2>Seu ritmo</h2>
                    <span>Últimos 7 dias</span>
                  </div>
                  <div className="week-count">
                    <strong>{weekWorkouts.length}</strong>
                    <span>
                      treinos
                      <br />
                      concluídos
                    </span>
                    <span className="stat-icon">
                      <TrendingUp size={23} />
                    </span>
                  </div>
                  <div className="week-days">
                    {days.map((d, i) => (
                      <div
                        key={d.time}
                        className={d.count ? "week-day trained" : "week-day"}
                      >
                        <span>
                          {new Date(d.time).toLocaleDateString("pt-BR", {
                            weekday: "narrow",
                          })}
                        </span>
                        <b>
                          {d.count ? (
                            <Check size={17} />
                          ) : (
                            new Date(d.time).getDate()
                          )}
                        </b>
                        <i className={i === 6 ? "today" : ""} />
                      </div>
                    ))}
                  </div>
                  <div className="week-bottom">
                    <span>
                      <strong>{totalSets}</strong> séries
                    </span>
                    <span>
                      <strong>{fmt(totalVolume)}</strong> kg de volume
                    </span>
                  </div>
                </div>
              </section>
              {active ? (
                <button
                  className="resume-card"
                  onClick={() => pageNav("session")}
                >
                  <span className="pulsing-dot" />
                  <div>
                    <strong>Seu treino está esperando</strong>
                    <span>
                      {active.title} · {completedSets(active).length} séries
                      registradas
                    </span>
                  </div>
                  <span>
                    Retomar
                    <ArrowRight size={18} />
                  </span>
                </button>
              ) : null}
              <section className="routines-section">
                <div className="section-heading">
                  <div>
                    <h2>Seus próximos treinos</h2>
                  </div>
                  <button
                    className="text-button"
                    onClick={() => pageNav("routines")}
                  >
                    Ver todos
                    <ArrowUpRight size={17} />
                  </button>
                </div>
                <div className="routine-grid">
                  {routines.slice(0, 3).map((r, i) => (
                    <RoutineCard
                      key={r.id}
                      routine={r}
                      index={i}
                      onEdit={(routine) =>
                        setModal({ type: "routine", routine })
                      }
                      onStart={begin}
                    />
                  ))}
                </div>
              </section>
              <section className="dashboard-bottom">
                <div className="activity-card">
                  <div className="section-heading">
                    <h2>Volume da semana</h2>
                    <span className="unit-tag">KG</span>
                  </div>
                  <div
                    className="volume-chart"
                    aria-label="Volume de treino nos últimos sete dias"
                  >
                    {days.map((d) => (
                      <div key={d.time}>
                        <div className="bar-track">
                          <i
                            style={{
                              height: d.volume
                                ? Math.max(
                                    12,
                                    (d.volume /
                                      Math.max(
                                        ...days.map((x) => x.volume),
                                        1,
                                      )) *
                                      100,
                                  ) + "%"
                                : "3px",
                            }}
                            data-empty={!d.volume}
                          />
                        </div>
                        <span>
                          {new Date(d.time)
                            .toLocaleDateString("pt-BR", { weekday: "short" })
                            .replace(".", "")}
                        </span>
                      </div>
                    ))}
                  </div>
                  {!weekWorkouts.length ? (
                    <p className="chart-note">
                      Seu gráfico começa no primeiro treino. Vamos nessa?
                    </p>
                  ) : (
                    <p className="chart-note">
                      Volume = carga × repetições das séries concluídas.
                    </p>
                  )}
                </div>
                <div className="recent-card">
                  <div className="section-heading">
                    <h2>Últimos registros</h2>
                    <button
                      className="icon-btn"
                      aria-label="Ver histórico"
                      onClick={() => pageNav("history")}
                    >
                      <ArrowUpRight size={20} />
                    </button>
                  </div>
                  {state.workouts.length ? (
                    state.workouts.slice(0, 3).map((w) => (
                      <button
                        className="history-row"
                        key={w.id}
                        onClick={() =>
                          setModal({ type: "workout", workout: w })
                        }
                      >
                        <span className="history-date">
                          <b>{new Date(w.endedAt).getDate()}</b>
                          <small>
                            {new Date(w.endedAt).toLocaleDateString("pt-BR", {
                              month: "short",
                            })}
                          </small>
                        </span>
                        <span>
                          <strong>{w.title}</strong>
                          <small>
                            {w.sets.length} séries · {fmt(volume(w))} kg
                          </small>
                        </span>
                        <ChevronRight size={17} />
                      </button>
                    ))
                  ) : (
                    <div className="empty-history">
                      <span>
                        <CalendarDays size={25} />
                      </span>
                      <strong>O começo de uma boa história.</strong>
                      <p>
                        Conclua um treino para ver
                        <br />
                        seu progresso por aqui.
                      </p>
                    </div>
                  )}
                </div>
              </section>
            </>
          ) : null}
          {view === "routines" ? (
            <>
              <div className="page-heading">
                <div>
                  <h1>
                    Meus treinos<span>.</span>
                  </h1>
                  <p className="page-description">
                    Rotinas que se encaixam no seu ritmo.
                  </p>
                </div>
                <button
                  className="button dark"
                  onClick={() => setModal({ type: "routine" })}
                >
                  <Plus size={18} />
                  Criar rotina
                </button>
              </div>
              <div className="routine-grid">
                <button
                  className="create-routine-card"
                  onClick={() => setModal({ type: "templates" })}
                >
                  <span>
                    <BookOpen size={26} />
                  </span>
                  <strong>Modelos prontos ({seeds.length})</strong>
                  <p>Explore os exercícios e edite uma nova cópia.</p>
                </button>
                {routines.map((r, i) => (
                  <RoutineCard
                    key={r.id}
                    routine={r}
                    index={i}
                    onEdit={(routine) => setModal({ type: "routine", routine })}
                    onStart={begin}
                  />
                ))}
                <button
                  className="create-routine-card"
                  onClick={() => setModal({ type: "routine" })}
                >
                  <span>
                    <Plus size={26} />
                  </span>
                  <strong>Seu treino, do seu jeito</strong>
                  <p>Monte uma rotina do zero.</p>
                </button>
              </div>
              <div className="quiet-note">
                <BookOpen size={18} />
                <p>
                  As rotinas iniciais são modelos editáveis, não um plano
                  personalizado. Escolha e ajuste os exercícios; não é
                  necessário realizar todos.
                </p>
              </div>
            </>
          ) : null}
          {view === "exercises" ? (
            <>
              <div className="page-heading">
                <div>
                  <h1>
                    Biblioteca de exercícios<span>.</span>
                  </h1>
                  <p className="page-description">
                    Encontre seu próximo movimento.
                  </p>
                </div>
                <span className="count-badge">{catalog.length} exercícios</span>
              </div>
              <div className="library-toolbar">
                <label className="search-field">
                  <Search size={18} />
                  <input
                    aria-label="Buscar na biblioteca"
                    placeholder="Nome, músculo ou equipamento"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                  />
                </label>
                <div className="chip-row">
                  {groups.map((g) => (
                    <button
                      key={g}
                      className={group === g ? "chip on" : "chip"}
                      aria-pressed={group === g}
                      onClick={() => setGroup(g)}
                    >
                      {g}
                    </button>
                  ))}
                </div>
              </div>
              <EquipmentFilter value={equipment} onChange={setEquipment} />
              <div className="filter-summary">
                <span role="status">
                  {libraryExercises.length} de {catalog.length} exercícios
                </span>
                {search || group !== "Todos" || equipment !== "Todos" ? (
                  <button
                    className="text-button"
                    onClick={() => {
                      setSearch("");
                      setGroup("Todos");
                      setEquipment("Todos");
                    }}
                  >
                    Limpar filtros
                  </button>
                ) : null}
              </div>
              <div className="exercise-grid">
                {libraryExercises.map((e) => (
                  <button
                    className="exercise-card"
                    key={e.id}
                    onClick={() => setModal({ type: "exercise", exercise: e })}
                  >
                    <Glyph muscle={e.muscle} size={86} />
                    <div>
                      <span className="muscle-label">{e.muscle}</span>
                      <h2>{e.name}</h2>
                      <p>{e.equipment}</p>
                    </div>
                    <ArrowUpRight size={18} />
                  </button>
                ))}
              </div>
              {!libraryExercises.length ? (
                <div className="empty-state">
                  <Search />
                  <h2>Nenhum exercício por aqui.</h2>
                  <p>Experimente outro nome ou remova os filtros.</p>
                  <button
                    className="button outline"
                    onClick={() => {
                      setGroup("Todos");
                      setSearch("");
                      setEquipment("Todos");
                    }}
                  >
                    Limpar busca
                  </button>
                </div>
              ) : null}
            </>
          ) : null}
          {view === "history" ? (
            <>
              <div className="page-heading">
                <div>
                  <h1>
                    Cada treino conta<span>.</span>
                  </h1>
                  <p className="page-description">
                    Seu histórico, sem perder uma repetição.
                  </p>
                </div>
                <span className="count-badge">
                  {state.workouts.length} treinos
                </span>
              </div>
              {state.workouts.length ? (
                <>
                  <div className="history-stat-grid">
                    <div>
                      <span>Treinos concluídos</span>
                      <strong>{state.workouts.length}</strong>
                    </div>
                    <div>
                      <span>Séries registradas</span>
                      <strong>
                        {state.workouts.reduce((n, w) => n + w.sets.length, 0)}
                      </strong>
                    </div>
                    <div>
                      <span>Volume acumulado</span>
                      <strong>
                        {fmt(state.workouts.reduce((n, w) => n + volume(w), 0))}
                        <small> kg</small>
                      </strong>
                    </div>
                  </div>
                  <section className="progress-card">
                    <div>
                      <h2>Seu melhor registro</h2>
                    </div>
                    <label className="sr-only" htmlFor="progress-exercise">
                      Exercício para consultar progresso
                    </label>
                    <select
                      id="progress-exercise"
                      value={progressExercise}
                      onChange={(e) => setProgressExercise(e.target.value)}
                    >
                      <option value="">Escolher exercício</option>
                      {catalog
                        .filter((e) =>
                          !isAssisted(e.id) && state.workouts.some((w) =>
                            w.sets.some((s) => s.exerciseId === e.id),
                          ),
                        )
                        .map((e) => (
                          <option key={e.id} value={e.id}>
                            {e.name}
                          </option>
                        ))}
                    </select>
                    {progressExercise ? (
                      <div className="personal-best">
                        <strong>
                          {fmt(
                            Math.max(
                              ...state.workouts.flatMap((w) =>
                                w.sets
                                  .filter(
                                    (s) => s.exerciseId === progressExercise,
                                  )
                                  .map((s) => s.weight),
                              ),
                            ),
                          )}{" "}
                          kg
                        </strong>
                        <span>
                          Maior carga registrada
                          <br />
                          nas suas séries concluídas
                        </span>
                      </div>
                    ) : null}
                  </section>
                  <section className="history-list">
                    {state.workouts.map((w) => (
                      <button
                        key={w.id}
                        className="history-row"
                        onClick={() =>
                          setModal({ type: "workout", workout: w })
                        }
                      >
                        <span className="history-date">
                          <b>{new Date(w.endedAt).getDate()}</b>
                          <small>
                            {new Date(w.endedAt).toLocaleDateString("pt-BR", {
                              month: "short",
                            })}
                          </small>
                        </span>
                        <span>
                          <strong>{w.title}</strong>
                          <small>
                            {w.sets.length} séries ·{" "}
                            {clock((w.endedAt - w.startedAt) / 1000)} min ·{" "}
                            {fmt(volume(w))} kg
                          </small>
                        </span>
                        <ArrowUpRight size={19} />
                      </button>
                    ))}
                  </section>
                </>
              ) : (
                <div className="empty-state history-empty-large">
                  <CalendarDays size={42} />

                  <h2>
                    Seu primeiro registro
                    <br />
                    está a um treino de distância.
                  </h2>
                  <p>Suas séries, cargas e evolução vão aparecer aqui.</p>
                  <button
                    className="button dark"
                    onClick={() => pageNav("routines")}
                  >
                    Escolher meu treino
                    <ArrowRight size={18} />
                  </button>
                </div>
              )}
            </>
          ) : null}
          {view === "session" && active ? (
            <>
              <div className="session-heading">
                <button className="text-button" onClick={() => pageNav("home")}>
                  <ArrowLeft size={17} />
                  Voltar ao início
                </button>
                <span className="live-session">
                  <span />
                  EM ANDAMENTO
                </span>
              </div>
              <div className="page-heading">
                <div>
                  <h1>
                    {active.title}
                    <span>.</span>
                  </h1>
                  <p className="page-description">
                    Registre a carga total utilizada. Peso corporal? Use 0 kg.
                  </p>
                </div>
                <button
                  className="button dark"
                  onClick={() => setModal({ type: "finish" })}
                >
                  Finalizar treino
                  <Check size={18} />
                </button>
              </div>
              <div className="session-metrics">
                <span>
                  <Clock3 size={17} />
                  <b>{clock((now - active.startedAt) / 1000)}</b>tempo
                </span>
                <span>
                  <Dumbbell size={17} />
                  <b>{fmt(volume(active))}</b>kg de volume
                </span>
                <span>
                  <CheckCircle2 size={17} />
                  <b>{completedSets(active).length}</b>séries
                </span>
              </div>
              {active.restUntil && active.restUntil > now ? (
                <div
                  className="rest-timer"
                  role="timer"
                  aria-label="Tempo restante de descanso"
                >
                  <Timer size={23} />
                  <div>
                    <span>RESPIRA. A PRÓXIMA VEM AÍ.</span>
                    <strong>{clock((active.restUntil - now) / 1000)}</strong>
                  </div>
                  <button
                    className="text-button"
                    aria-label="Diminuir descanso em 30 segundos"
                    onClick={() => updateActive((a) => adjustRest(a, -30))}
                  >
                    −30 s
                  </button>
                  <button
                    className="text-button"
                    onClick={() =>
                      updateActive((a) => adjustRest(a, 30))
                    }
                  >
                    +30 s
                  </button>
                  <button
                    className="icon-btn"
                    aria-label="Pular descanso"
                    onClick={() =>
                      updateActive((a) => ({ ...a, restUntil: null }))
                    }
                  >
                    <SkipForward size={19} />
                  </button>
                </div>
              ) : null}
              <div className="session-exercises">
                {active.exercises.map((spec, index) => {
                  const e = exerciseById[spec.exerciseId];
                  const previous = latestSets(state.workouts, e.id);
                  return (
                    <section className="session-exercise" key={e.id}>
                      <div className="session-exercise-heading">
                        <Glyph muscle={e.muscle} />
                        <div>
                          <span className="eyebrow">
                            EXERCÍCIO {String(index + 1).padStart(2, "0")}
                          </span>
                          <h2>{e.name}</h2>
                          <p>
                            {e.muscle} · {e.equipment}{" "}
                            <span>Descanso: {spec.restSeconds}s</span>
                          </p>
                        </div>
                        <button
                          className="icon-btn danger"
                          aria-label={"Remover " + e.name + " do treino"}
                          onClick={() =>
                            setModal({ type: "remove-exercise", exercise: e })
                          }
                        >
                          <X size={19} />
                        </button>
                      </div>
                      <button
                        className="text-button manual-rest"
                        disabled={Number(spec.restSeconds) <= 0}
                        aria-label={"Iniciar ou reiniciar descanso de " + e.name}
                        onClick={() => updateActive((a) => startRest(a, e.id))}
                      >
                        <Timer size={16} /> Iniciar descanso · {spec.restSeconds}s
                      </button>
                      {isAssisted(e.id) ? (
                        <p className="validation-message">
                          Registre kg de assistência da máquina. Estas séries contam no histórico,
                          mas não no volume nem nos recordes de carga.
                        </p>
                      ) : null}
                      <div className="sets-table">
                        <div className="sets-table-head">
                          <span>SÉRIE</span>
                          <span>ANTERIOR</span>
                          <span>{isAssisted(e.id) ? "ASSIST. KG" : "KG"}</span>
                          <span>REPS</span>
                          <span>FEITO</span>
                        </div>
                        {active.sets
                          .filter((s) => s.exerciseId === e.id)
                          .map((set, i) => (
                            <div
                              className={
                                set.done ? "set-row completed" : "set-row"
                              }
                              key={set.id}
                            >
                              <span className="set-number">{i + 1}</span>
                              <span className="previous-set">
                                {previous[i]
                                  ? `${fmt(previous[i].weight)} × ${previous[i].reps}`
                                  : "—"}
                              </span>
                              <label>
                                <span className="sr-only">
                                  {isAssisted(e.id) ? "Assistência em kg" : "Carga"}, série {i + 1}, {e.name}
                                </span>
                                <input
                                  inputMode="decimal"
                                  placeholder="0"
                                  value={set.weight}
                                  aria-invalid={
                                    set.weight !== "" && !validSet(set)
                                  }
                                  onChange={(event) =>
                                    updateActive((a) => ({
                                      ...a,
                                      sets: a.sets.map((s) =>
                                        s.id === set.id
                                          ? {
                                              ...s,
                                              weight: event.target.value,
                                              done: false,
                                            }
                                          : s,
                                      ),
                                    }))
                                  }
                                />
                              </label>
                              <label>
                                <span className="sr-only">
                                  Repetições, série {i + 1}, {e.name}
                                </span>
                                <input
                                  inputMode="numeric"
                                  value={set.reps}
                                  onChange={(event) =>
                                    updateActive((a) => ({
                                      ...a,
                                      sets: a.sets.map((s) =>
                                        s.id === set.id
                                          ? {
                                              ...s,
                                              reps: event.target.value,
                                              done: false,
                                            }
                                          : s,
                                      ),
                                    }))
                                  }
                                />
                              </label>
                              <button
                                className={
                                  set.done ? "set-check done" : "set-check"
                                }
                                disabled={!set.done && !validSet(set)}
                                aria-label={
                                  (set.done ? "Desmarcar" : "Concluir") +
                                  " série " +
                                  (i + 1) +
                                  " de " +
                                  e.name
                                }
                                aria-pressed={set.done}
                                onClick={() =>
                                  updateActive((a) => toggleSet(a, set.id))
                                }
                              >
                                <Check size={18} />
                              </button>
                            </div>
                          ))}
                      </div>
                      <button
                        className="add-set"
                        onClick={() =>
                          updateActive((a) => ({
                            ...a,
                            sets: [
                              ...a.sets,
                              {
                                id: id(),
                                exerciseId: e.id,
                                weight: "",
                                reps: String(spec.reps),
                                done: false,
                              },
                            ],
                          }))
                        }
                      >
                        <Plus size={16} />
                        Adicionar série
                      </button>
                    </section>
                  );
                })}
              </div>
              <button
                className="button outline full add-exercise"
                onClick={() => setModal({ type: "picker" })}
              >
                <Plus size={18} />
                Adicionar exercício
              </button>
              <label className="session-note">
                Notas do treino
                <textarea
                  placeholder="Como foi hoje?"
                  maxLength={2000}
                  value={active.note}
                  onChange={(e) =>
                    updateActive((a) => ({ ...a, note: e.target.value }))
                  }
                />
              </label>
              <button
                className="discard-button"
                onClick={() => setModal({ type: "discard" })}
              >
                Descartar este treino
              </button>
            </>
          ) : null}
        </main>
        <footer className="app-footer">
          <span>{brand.name}.</span>
          <span>{brand.tagline}</span>
          <span>
            Feito para o seu ritmo <span className="footer-dot" />
          </span>
        </footer>
      </div>
      {active && view !== "session" ? (
        <button className="floating-session" onClick={() => pageNav("session")}>
          <span className="pulsing-dot" />
          <span>
            <strong>{active.title}</strong>
            <small>
              {clock((now - active.startedAt) / 1000)} · Retomar treino
            </small>
          </span>
          <Play size={18} />
        </button>
      ) : null}
      {view !== "session" ? (
        <nav className="mobile-nav" aria-label="Navegação mobile">
          {nav.map(([key, label, Icon]) => (
            <button
              key={key}
              aria-current={view === key ? "page" : undefined}
              onClick={() => pageNav(key)}
              className={view === key ? "on" : ""}
            >
              <Icon size={20} />
              <span>
                {key === "home"
                  ? "Início"
                  : key === "routines"
                    ? "Treinos"
                    : label}
              </span>
            </button>
          ))}
        </nav>
      ) : null}
      {toast ? (
        <div className="toast" role="status">
          <CheckCircle2 size={18} />
          {toast}
        </div>
      ) : null}
      {modal?.type === "routine" ? (
        <RoutineEditor
          routine={modal.routine}
          onSave={saveRoutine}
          onClose={() => setModal(null)}
        />
      ) : null}
      {modal?.type === "templates" ? (
        <Modal title="Modelos de rotina" onClose={() => setModal(null)} wide>
          <p className="modal-description">
            Exemplos editáveis, não um plano personalizado. Usar modelo abre uma
            nova cópia; suas rotinas salvas continuam iguais.
          </p>
          {seeds.map((template) => (
            <details className="routine-template" key={template.id}>
              <summary>
                {template.name} · {template.exercises.length} exercícios
              </summary>
              <p className="modal-description">{template.note}</p>
              <ul>
                {template.exercises.map((spec) => (
                  <li key={spec.exerciseId}>
                    {exerciseById[spec.exerciseId]?.name || spec.exerciseId}
                    {" — "}
                    {spec.sets} × {spec.reps} · descanso {spec.restSeconds}s
                  </li>
                ))}
              </ul>
              <button
                className="button dark full"
                onClick={() =>
                  setModal({
                    type: "routine",
                    routine: {
                      ...structuredClone(template),
                      id: id(),
                      archived: false,
                      updatedAt: Date.now(),
                    },
                  })
                }
              >
                Usar modelo <Plus size={18} />
              </button>
            </details>
          ))}
        </Modal>
      ) : null}
      {modal?.type === "picker" && active ? (
        <ExercisePicker
          existing={active.exercises.map((e) => e.exerciseId)}
          onClose={() => setModal(null)}
          onSelect={(exerciseId) => {
            updateActive((a) => addExercise(a, exerciseId, {}, state.workouts));
            setModal(null);
          }}
        />
      ) : null}
      {modal?.type === "exercise" ? (
        <Modal title={modal.exercise.name} onClose={() => setModal(null)}>
          <div className="exercise-detail">
            <Glyph muscle={modal.exercise.muscle} size={130} />
            <span className="chip on">
              {modal.exercise.muscle} · {modal.exercise.equipment}
            </span>
            <p>{modal.exercise.description}</p>
            <p className="muted">
              Use a biblioteca para montar suas rotinas. O registro de carga é
              sempre em kg.
            </p>
          </div>
          <button
            className="button dark full"
            onClick={() => {
              const e = modal.exercise;
              setModal(null);
              if (active) {
                updateActive((a) => addExercise(a, e.id, {}, state.workouts));
                setView("session");
              } else {
                setState((s) => ({
                  ...s,
                  active: addExercise(startWorkout(null), e.id, {}, s.workouts),
                }));
                setView("session");
              }
            }}
          >
            <Plus size={18} />
            {active
              ? "Adicionar ao treino em andamento"
              : "Iniciar treino com este exercício"}
          </button>
        </Modal>
      ) : null}
      {modal?.type === "workout" ? (
        <WorkoutDetail workout={modal.workout} onClose={() => setModal(null)} />
      ) : null}
      {modal?.type === "finish" && active ? (
        <Modal title="Mais um passo dado." onClose={() => setModal(null)}>
          <p className="modal-description">
            Revise seu registro antes de encerrar. Só as séries marcadas como
            concluídas serão salvas.
          </p>
          <div className="summary-stats">
            <span>
              <strong>{clock((now - active.startedAt) / 1000)}</strong>
              <small>Duração</small>
            </span>
            <span>
              <strong>{completedSets(active).length}</strong>
              <small>Séries concluídas</small>
            </span>
            <span>
              <strong>{fmt(volume(active))} kg</strong>
              <small>Volume</small>
            </span>
          </div>
          {!completedSets(active).length ? (
            <p className="validation-message">
              Conclua pelo menos uma série para salvar este treino.
            </p>
          ) : null}
          <button
            className="button dark full"
            disabled={!completedSets(active).length}
            onClick={() => {
              try {
                const finished = finishWorkout(stateRef.current);
                setState(finished);
                setModal(null);
                setView("history");
                setToast("Treino salvo. Bom trabalho!");
              } catch (error) {
                setToast(String(error.message || error));
              }
            }}
          >
            Salvar treino
            <Check size={18} />
          </button>
          <button className="text-button full" onClick={() => setModal(null)}>
            Continuar treinando
          </button>
        </Modal>
      ) : null}
      {modal?.type === "discard" ? (
        <Modal title="Descartar este treino?" onClose={() => setModal(null)}>
          <p className="modal-description">
            As séries deste treino em andamento serão removidas. Seus treinos
            concluídos e suas rotinas continuam salvos.
          </p>
          <button
            className="button danger-button full"
            onClick={() => {
              setState((s) => ({ ...s, active: null }));
              setModal(null);
              setView("home");
            }}
          >
            Descartar treino
          </button>
          <button className="text-button full" onClick={() => setModal(null)}>
            Voltar ao treino
          </button>
        </Modal>
      ) : null}
      {modal?.type === "remove-exercise" ? (
        <Modal title="Remover exercício?" onClose={() => setModal(null)}>
          <p className="modal-description">
            As séries de {modal.exercise.name} serão removidas deste treino em
            andamento.
          </p>
          <button
            className="button danger-button full"
            onClick={() => {
              const exerciseId = modal.exercise.id;
              updateActive((a) => ({
                ...a,
                exercises: a.exercises.filter(
                  (e) => e.exerciseId !== exerciseId,
                ),
                sets: a.sets.filter((s) => s.exerciseId !== exerciseId),
              }));
              setModal(null);
            }}
          >
            Remover exercício
          </button>
          <button className="text-button full" onClick={() => setModal(null)}>
            Manter exercício
          </button>
        </Modal>
      ) : null}
    </div>
  );
}
export default App;
