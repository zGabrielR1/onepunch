@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.onepunch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF202B21)
private val Paper = Color(0xFFF6F6F1)
private val Lime = Color(0xFFD6EF7D)
private val Muted = Color(0xFF68745E)
private val Line = Color(0xFFDFE5D5)
private val Br = Locale.forLanguageTag("pt-BR")
private val Muscles = listOf("Todos", "Peito", "Costas", "Pernas", "Ombros", "Braços", "Core")

private enum class Screen(val label: String, val icon: ImageVector) {
    Home("Início", Icons.Outlined.Dashboard),
    Routines("Treinos", Icons.Outlined.FitnessCenter),
    Exercises("Exercícios", Icons.AutoMirrored.Outlined.MenuBook),
    History("Histórico", Icons.Outlined.CalendarMonth),
    Training("Treinando", Icons.Outlined.PlayArrow),
}

private fun fmt(value: Double) =
    NumberFormat.getNumberInstance(Br).apply { maximumFractionDigits = 1 }.format(value)

private fun clock(ms: Long): String {
    val seconds = maxOf(0, ms / 1000)
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}

private fun date(time: Long, includeYear: Boolean = false) =
    Instant.ofEpochMilli(time)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(if (includeYear) "d 'de' MMM 'de' yyyy" else "d 'de' MMM", Br))

private fun day(time: Long) =
    Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()

private fun muscleColor(muscle: String) =
    when (muscle) {
        "Peito" -> Color(0xFFD9EAD0)
        "Costas" -> Color(0xFFDBE4F2)
        "Pernas" -> Color(0xFFEEE1CD)
        "Ombros" -> Color(0xFFE7DEF2)
        "Braços" -> Color(0xFFF0DCD6)
        else -> Color(0xFFD5E9E6)
    }

@Composable
private fun Eyebrow(text: String) {
    Text(
        text,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
        color = Muted,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun Header(title: String, subtitle: String = "") {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            title,
            fontSize = 29.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
        )
        if (subtitle.isNotEmpty())
            Text(subtitle, fontSize = 13.sp, color = Muted, lineHeight = 20.sp)
    }
}

@Composable
private fun Glyph(exercise: Exercise, size: Int = 48) {
    Box(
        Modifier.size(size.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(muscleColor(exercise.muscle)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.FitnessCenter,
            null,
            tint = Color(0xFF61754D),
            modifier = Modifier.size((size * .49).dp),
        )
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(value, fontSize = 25.sp, letterSpacing = (-.6).sp, fontWeight = FontWeight.SemiBold)
        Text(label, fontSize = 11.sp, color = Muted)
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier,
        shape = RoundedCornerShape(13.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    icon: ImageVector = Icons.AutoMirrored.Outlined.ArrowForward,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(9.dp),
    ) {
        Text(text, Modifier.weight(1f))
        Icon(icon, null, Modifier.size(19.dp))
    }
}

@Composable
fun OnepunchApp() {
    val context = LocalContext.current
    val store = remember { StateStore(context) }
    val initial = remember { runCatching { store.load() } }
    val initialDraft = remember { runCatching { store.loadRoutineDraft() } }
    var state by remember { mutableStateOf(initial.getOrDefault(Workspace())) }
    var routineDraft by remember { mutableStateOf(initialDraft.getOrNull()) }
    var draftError by remember {
        mutableStateOf(if (initialDraft.isFailure) "Não foi possível abrir o rascunho. O registro original foi preservado; a edição de rotinas está bloqueada para não substituí-lo." else null)
    }
    var screen by remember { mutableStateOf(Screen.Home) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var picking by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    var templateLibrary by remember { mutableStateOf(false) }
    var archiving by remember { mutableStateOf<Routine?>(null) }
    var showArchived by remember { mutableStateOf(false) }
    var finish by remember { mutableStateOf(false) }
    var finishError by remember { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<Exercise?>(null) }
    var removingSet by remember { mutableStateOf<WorkoutSet?>(null) }
    var restEditing by remember { mutableStateOf<ExerciseSpec?>(null) }
    var replacing by remember { mutableStateOf<ExerciseSpec?>(null) }
    var replacement by remember { mutableStateOf<Exercise?>(null) }
    var replacementError by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<Exercise?>(null) }
    var workoutDetail by remember { mutableStateOf<Workout?>(null) }
    var syncOpen by remember { mutableStateOf(false) }
    var server by remember { mutableStateOf(store.server) }
    var syncing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val catalog = store.catalog
    val lookup = remember { catalog.associateBy { it.id } }
    fun save(next: Workspace, clearDraft: Boolean = false): Boolean {
        return try {
            store.save(next, clearRoutineDraft = clearDraft)
            state = next
            if (clearDraft) routineDraft = null
            true
        } catch (e: Exception) {
            scope.launch { snackbar.showSnackbar(e.message ?: "Não foi possível salvar.") }
            false
        }
    }
    fun saveDraft(next: RoutineDraft): Boolean {
        return try {
            store.saveRoutineDraft(next)
            routineDraft = next
            draftError = null
            true
        } catch (e: Exception) {
            draftError = e.message ?: "Não foi possível salvar o rascunho."
            false
        }
    }
    fun openRoutineEditor(routine: Routine?, isNew: Boolean = routine == null): Boolean {
        if (initialDraft.isFailure) {
            scope.launch { snackbar.showSnackbar("O rascunho original foi preservado. Não é possível substituí-lo.") }
            return false
        }
        if (routineDraft != null) {
            editorOpen = true
            scope.launch { snackbar.showSnackbar("Retome e salve ou descarte o rascunho atual antes de editar outra rotina.") }
            return true
        }
        val original = routine ?: Routine(name = "")
        val saved = saveDraft(RoutineDraft(original = original, isNew = isNew))
        if (saved) editorOpen = true
        return saved
    }
    fun discardRoutineDraft() {
        try {
            store.clearRoutineDraft()
            routineDraft = null
            draftError = null
            editorOpen = false
        } catch (e: Exception) {
            draftError = e.message ?: "Não foi possível descartar o rascunho."
        }
    }
    fun finishRoutineDraft(asCopy: Boolean = false) {
        val currentDraft = routineDraft ?: return
        try {
            val next = commitRoutineDraft(state, currentDraft, asCopy = asCopy)
            if (save(next, clearDraft = true)) {
                showArchived = if (asCopy) false else currentDraft.draft.archived
                editorOpen = false
                draftError = null
            } else draftError = "A rotina não foi salva. Seu rascunho foi mantido; tente novamente."
        } catch (e: Exception) {
            draftError = e.message ?: "Não foi possível salvar a rotina."
        }
    }
    fun begin(routine: Routine?) {
        if (state.active == null) save(state.copy(active = startWorkout(routine, state.workouts)))
        screen = Screen.Training
    }
    fun updateActive(change: (ActiveWorkout) -> ActiveWorkout): Boolean {
        val active = state.active ?: return false
        return save(state.copy(active = change(active)))
    }
    fun setArchived(id: String, archived: Boolean): Boolean {
        val current = state.routines.find { it.id == id } ?: return false
        return save(
            state.copy(
                routines = state.routines.map {
                    if (it.id == id)
                        current.copy(
                            archived = archived,
                            updatedAt = maxOf(System.currentTimeMillis(), current.updatedAt + 1),
                        )
                    else it
                },
            ),
        )
    }
    fun duplicateRoutine(routine: Routine) {
        val copy = routine.copy(
            id = uid(),
            name = routine.name.take(72) + " (cópia)",
            archived = false,
            updatedAt = System.currentTimeMillis(),
        )
        openRoutineEditor(copy, isNew = true)
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = Ink,
                onPrimary = Color.White,
                secondary = Color(0xFF68814B),
                background = Paper,
                surface = Color.White,
                surfaceVariant = Color(0xFFEDF2E3),
                onSurface = Ink,
                outline = Line,
            ),
        typography =
            Typography(
                bodyLarge =
                    androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 15.sp,
                        color = Ink,
                    ),
            ),
    ) {
        if (initial.isFailure) {
            Surface(Modifier.fillMaxSize(), color = Paper) {
                Column(
                    Modifier.safeDrawingPadding().padding(25.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Header(
                        "Seus dados precisam de atenção.",
                        "Não sobrescrevemos o registro local porque não foi possível abri-lo. Seus dados foram preservados neste dispositivo.",
                    )
                }
            }
            return@MaterialTheme
        }
        Scaffold(
            containerColor = Paper,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Icon(Icons.Outlined.FitnessCenter, null, tint = Color(0xFF647D45))
                            Text(
                                if (screen == Screen.Training) "Treino em andamento"
                                else stringResource(R.string.app_name).lowercase(Br) + ".",
                                fontSize = if (screen == Screen.Training) 18.sp else 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-.8).sp,
                            )
                        }
                    },
                    navigationIcon = {
                        if (screen == Screen.Training)
                            IconButton(onClick = { screen = Screen.Home }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar ao início")
                            }
                    },
                    actions = {
                        if (screen != Screen.Training)
                            IconButton(onClick = { syncOpen = true }) {
                                Icon(Icons.Outlined.CloudUpload, "Sincronizar treinos")
                            }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper),
                )
            },
            bottomBar = {
                if (screen != Screen.Training)
                    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                        Screen.entries
                            .filter { it != Screen.Training }
                            .forEach { item ->
                                NavigationBarItem(
                                    selected = screen == item,
                                    onClick = { screen = item },
                                    icon = { Icon(item.icon, null, Modifier.size(22.dp)) },
                                    label = { Text(item.label, fontSize = 10.sp) },
                                    colors =
                                        NavigationBarItemDefaults.colors(
                                            indicatorColor = Color(0xFFE5EDCF),
                                            selectedIconColor = Ink,
                                            selectedTextColor = Ink,
                                            unselectedIconColor = Muted,
                                            unselectedTextColor = Muted,
                                        ),
                                )
                            }
                    }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (screen) {
                    Screen.Home ->
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding =
                                PaddingValues(
                                    22.dp,
                                    16.dp,
                                    22.dp,
                                    if (state.active != null) 100.dp else 25.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(22.dp),
                        ) {
                            if (routineDraft != null || draftError != null)
                                item {
                                    RoutineDraftNotice(routineDraft, draftError) { editorOpen = true }
                                }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                                    Eyebrow(
                                        LocalDate.now()
                                            .format(
                                                DateTimeFormatter.ofPattern(
                                                    "EEEE, d 'de' MMMM",
                                                    Br,
                                                ),
                                            )
                                            .uppercase(Br),
                                    )
                                    Header(
                                        "Vamos fazer acontecer.",
                                        "O próximo passo é seu. A gente cuida do registro.",
                                    )
                                }
                            }
                            item { Hero { screen = Screen.Routines } }
                            item {
                                val week =
                                    state.workouts.filter {
                                        !day(it.endedAt).isBefore(LocalDate.now().minusDays(6)) &&
                                            !day(it.endedAt).isAfter(LocalDate.now())
                                    }
                                Panel {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("Seu ritmo", fontWeight = FontWeight.SemiBold)
                                        Text("Últimos 7 dias", fontSize = 11.sp, color = Muted)
                                    }
                                    Row {
                                        Metric(week.size.toString(), "treinos", Modifier.weight(1f))
                                        Metric(
                                            week.sumOf { it.sets.size }.toString(),
                                            "séries",
                                            Modifier.weight(1f),
                                        )
                                        Metric(
                                            fmt(week.sumOf { volume(it.sets) }),
                                            "kg de volume",
                                            Modifier.weight(1f),
                                        )
                                    }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        (6 downTo 0).forEach { offset ->
                                            val date = LocalDate.now().minusDays(offset.toLong())
                                            val trained = week.any { day(it.endedAt) == date }
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Text(
                                                    date.format(
                                                        DateTimeFormatter.ofPattern("EEEEE", Br),
                                                    ),
                                                    fontSize = 10.sp,
                                                    color = Muted,
                                                )
                                                Box(
                                                    Modifier.size(33.dp)
                                                        .clip(RoundedCornerShape(7.dp))
                                                        .background(if (trained) Lime else Paper),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (trained)
                                                        Icon(
                                                            Icons.Outlined.Check,
                                                            null,
                                                            Modifier.size(18.dp),
                                                        )
                                                    else
                                                        Text(
                                                            date.dayOfMonth.toString(),
                                                            fontSize = 11.sp,
                                                            color = Muted,
                                                        )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Seus próximos treinos",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = { screen = Screen.Routines }) {
                                        Text("Ver todos", fontSize = 11.sp)
                                        Icon(
                                            Icons.Outlined.ArrowOutward,
                                            null,
                                            Modifier.size(15.dp),
                                        )
                                    }
                                }
                            }
                            items(
                                state.routines.filterNot { it.archived }.take(3),
                                key = { it.id },
                            ) { routine ->
                                RoutineCard(
                                    routine,
                                    lookup,
                                    { begin(routine) },
                                    {
                                        openRoutineEditor(routine)
                                    },
                                )
                            }
                            item {
                                Panel {
                                    Text(
                                        "Últimos registros",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (state.workouts.isEmpty()) {
                                        Icon(Icons.Outlined.CalendarMonth, null, tint = Muted)
                                        Text(
                                            "O começo de uma boa história.",
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            "Conclua um treino para ver seu progresso por aqui.",
                                            fontSize = 13.sp,
                                            color = Muted,
                                        )
                                    } else
                                        state.workouts.take(3).forEach { workout ->
                                            HistoryRow(workout) { workoutDetail = workout }
                                        }
                                }
                            }
                        }
                    Screen.Routines ->
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding =
                                PaddingValues(
                                    22.dp,
                                    16.dp,
                                    22.dp,
                                    if (state.active != null) 100.dp else 25.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            item {
                                Header("Meus treinos.", "Rotinas que se encaixam no seu ritmo.")
                            }
                            if (routineDraft != null || draftError != null)
                                item {
                                    RoutineDraftNotice(routineDraft, draftError) { editorOpen = true }
                                }
                            item {
                                PrimaryButton("Criar rotina", Icons.Outlined.Add) {
                                    openRoutineEditor(null)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { templateLibrary = true },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.MenuBook, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Modelos prontos (${store.routineTemplates.size})")
                                }
                            }
                            item {
                                Row(
                                    Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    FilterChip(
                                        selected = !showArchived,
                                        onClick = { showArchived = false },
                                        label = { Text("Ativas (${state.routines.count { !it.archived }})") },
                                    )
                                    FilterChip(
                                        selected = showArchived,
                                        onClick = { showArchived = true },
                                        label = { Text("Arquivadas (${state.routines.count { it.archived }})") },
                                    )
                                }
                            }
                            if (state.routines.none { it.archived == showArchived })
                                item {
                                    Panel {
                                        Text(
                                            if (showArchived) "Nenhuma rotina arquivada."
                                            else "Suas próximas rotinas começam aqui.",
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            if (showArchived)
                                                "Ao arquivar uma rotina, você poderá restaurá-la aqui. Seu histórico continua salvo."
                                            else "Crie uma rotina ou restaure uma das arquivadas para voltar a usá-la.",
                                            color = Muted,
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                            items(state.routines.filter { it.archived == showArchived }, key = { it.id }) {
                                routine ->
                                RoutineCard(
                                    routine,
                                    lookup,
                                    { begin(routine) },
                                    {
                                        openRoutineEditor(routine)
                                    },
                                    onDuplicate = { duplicateRoutine(routine) },
                                    onArchive = { archiving = routine },
                                    onRestore = { setArchived(routine.id, false) },
                                )
                            }
                            item {
                                Text(
                                    "As rotinas iniciais são modelos editáveis, não um plano personalizado. Ajuste-as à sua programação.",
                                    fontSize = 12.sp,
                                    lineHeight = 19.sp,
                                    color = Muted,
                                )
                            }
                            item {
                                OutlinedButton(
                                    onClick = { begin(null) },
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(9.dp),
                                ) {
                                    Icon(Icons.Outlined.Add, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Começar treino livre")
                                }
                            }
                        }
                    Screen.Exercises -> Library(catalog, state.active != null, { detail = it })
                    Screen.History ->
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding =
                                PaddingValues(
                                    22.dp,
                                    16.dp,
                                    22.dp,
                                    if (state.active != null) 100.dp else 25.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            item {
                                Header(
                                    "Cada treino conta.",
                                    "Seu histórico, sem perder uma repetição.",
                                )
                            }
                            if (state.workouts.isEmpty())
                                item {
                                    Panel {
                                        Icon(
                                            Icons.Outlined.CalendarMonth,
                                            null,
                                            Modifier.size(42.dp),
                                            tint = Muted,
                                        )
                                        Text(
                                            "Seu primeiro registro está a um treino de distância.",
                                            fontSize = 23.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            "Suas séries, cargas e evolução vão aparecer aqui.",
                                            color = Muted,
                                            fontSize = 13.sp,
                                        )
                                        PrimaryButton("Escolher meu treino") {
                                            screen = Screen.Routines
                                        }
                                    }
                                }
                            else {
                                item {
                                    Panel {
                                        Row {
                                            Metric(
                                                state.workouts.size.toString(),
                                                "treinos",
                                                Modifier.weight(1f),
                                            )
                                            Metric(
                                                state.workouts.sumOf { it.sets.size }.toString(),
                                                "séries",
                                                Modifier.weight(1f),
                                            )
                                            Metric(
                                                fmt(state.workouts.sumOf { volume(it.sets) }),
                                                "kg",
                                                Modifier.weight(1f),
                                            )
                                        }
                                    }
                                }
                                items(state.workouts, key = { it.id }) { workout ->
                                    Panel { HistoryRow(workout) { workoutDetail = workout } }
                                }
                                item {
                                    Panel {
                                        Eyebrow("MELHORES CARGAS REGISTRADAS")
                                        catalog
                                            .filter { e ->
                                                state.workouts.any { w ->
                                                    w.sets.any { it.exerciseId == e.id }
                                                }
                                            }
                                            .forEach { e ->
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    horizontalArrangement =
                                                        Arrangement.SpaceBetween,
                                                ) {
                                                    Text(
                                                        e.name,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                    Text(
                                                        "${fmt(state.workouts.flatMap { it.sets }.filter { it.exerciseId == e.id }.maxOf { numeric(it.weight) ?: 0.0 })} kg",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                }
                                            }
                                    }
                                }
                            }
                        }
                    Screen.Training ->
                        state.active?.let { active ->
                            val review = remember(active) { reviewWorkout(active) }
                            LazyColumn(
                                Modifier.fillMaxSize().imePadding(),
                                contentPadding = PaddingValues(18.dp, 16.dp, 18.dp, 30.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                item {
                                    Header(
                                        "Treino em andamento.",
                                        "Registre a carga total utilizada. Peso corporal? Use 0 kg.",
                                    )
                                }
                                item {
                                    OutlinedTextField(
                                        value = active.title,
                                        onValueChange = { title -> updateActive { it.copy(title = title.take(80)) } },
                                        label = { Text("Nome deste treino") },
                                        isError = !workoutTitleValid(active.title),
                                        supportingText = {
                                            if (!workoutTitleValid(active.title)) Text("Dê um nome ao treino antes de salvar.")
                                            else Text("Mudar este nome não altera sua rotina.")
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                item { WorkoutProgress(review) }
                                item {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                                    ) {
                                        Metric(
                                            clock(now - active.startedAt),
                                            "tempo",
                                            Modifier.weight(1f),
                                        )
                                        Metric(
                                            completed(active.sets).size.toString(),
                                            "séries",
                                            Modifier.weight(1f),
                                        )
                                        Metric(
                                            fmt(volume(active.sets)),
                                            "kg de volume",
                                            Modifier.weight(1f),
                                        )
                                    }
                                }
                                if ((active.restUntil ?: 0) > now)
                                    item {
                                        Surface(
                                            color = Color(0xFFE7EFDA),
                                            shape = RoundedCornerShape(10.dp),
                                        ) {
                                            Row(
                                                Modifier.fillMaxWidth().padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Timer,
                                                    null,
                                                    tint = Color(0xFF6B844E),
                                                )
                                                Column(Modifier.weight(1f)) {
                                                    Eyebrow("DESCANSO")
                                                    Text(
                                                        clock((active.restUntil ?: now) - now),
                                                        fontSize = 29.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                }
                                                TextButton(
                                                    onClick = {
                                                        updateActive {
                                                            it.copy(
                                                                restUntil =
                                                                    (it.restUntil ?: now) + 30000,
                                                            )
                                                        }
                                                    },
                                                ) {
                                                    Text("+30 s")
                                                }
                                                IconButton(
                                                    onClick = {
                                                        updateActive { it.copy(restUntil = null) }
                                                    },
                                                ) {
                                                    Icon(Icons.Outlined.SkipNext, "Pular descanso")
                                                }
                                            }
                                        }
                                    }
                                items(active.exercises, key = { it.exerciseId }) { spec ->
                                    val e = lookup[spec.exerciseId] ?: return@items
                                    val previous = latestSets(state.workouts, e.id)
                                    var exerciseMenu by remember { mutableStateOf(false) }
                                    Panel {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Glyph(e)
                                            Column(
                                                Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Text(
                                                    e.name,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                Text(
                                                    "${e.equipment} · descanso ${spec.restSeconds}s",
                                                    fontSize = 11.sp,
                                                    color = Muted,
                                                )
                                            }
                                            Box {
                                                IconButton(onClick = { exerciseMenu = true }) {
                                                    Icon(Icons.Outlined.MoreVert, "Opções de ${e.name}")
                                                }
                                                DropdownMenu(
                                                    expanded = exerciseMenu,
                                                    onDismissRequest = { exerciseMenu = false },
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Mover para cima") },
                                                        enabled = active.exercises.firstOrNull()?.exerciseId != e.id,
                                                        onClick = {
                                                            exerciseMenu = false
                                                            updateActive { moveWorkoutExercise(it, e.id, -1) }
                                                        },
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Mover para baixo") },
                                                        enabled = active.exercises.lastOrNull()?.exerciseId != e.id,
                                                        onClick = {
                                                            exerciseMenu = false
                                                            updateActive { moveWorkoutExercise(it, e.id, 1) }
                                                        },
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Substituir séries pendentes") },
                                                        enabled = active.sets.none { it.exerciseId == e.id } ||
                                                            active.sets.any { it.exerciseId == e.id && !it.done },
                                                        onClick = {
                                                            exerciseMenu = false
                                                            replacement = null
                                                            replacementError = null
                                                            replacing = spec
                                                        },
                                                    )
                                                    HorizontalDivider()
                                                    DropdownMenuItem(
                                                        text = { Text("Histórico do exercício") },
                                                        onClick = {
                                                            exerciseMenu = false
                                                            detail = e
                                                        },
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Ajustar descanso") },
                                                        onClick = {
                                                            exerciseMenu = false
                                                            restEditing = spec
                                                        },
                                                    )
                                                    active.sets.filter { it.exerciseId == e.id }
                                                        .forEachIndexed { index, set ->
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text("Remover série ${index + 1}${if (set.done) " (concluída)" else ""}")
                                                                },
                                                                onClick = {
                                                                    exerciseMenu = false
                                                                    removingSet = set
                                                                },
                                                            )
                                                        }
                                                    HorizontalDivider()
                                                    DropdownMenuItem(
                                                        text = { Text("Remover exercício") },
                                                        onClick = {
                                                            exerciseMenu = false
                                                            removing = e
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(
                                                "SÉRIE",
                                                fontSize = 8.sp,
                                                color = Muted,
                                                modifier = Modifier.width(28.dp),
                                            )
                                            Text(
                                                "ANTERIOR",
                                                fontSize = 8.sp,
                                                color = Muted,
                                                modifier = Modifier.width(49.dp),
                                            )
                                            Text(
                                                "KG",
                                                fontSize = 9.sp,
                                                color = Muted,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Text(
                                                "REPS",
                                                fontSize = 9.sp,
                                                color = Muted,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Spacer(Modifier.width(44.dp))
                                        }
                                        active.sets
                                            .filter { it.exerciseId == e.id }
                                            .forEachIndexed { index, set ->
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement =
                                                        Arrangement.spacedBy(6.dp),
                                                ) {
                                                    Text(
                                                        (index + 1).toString(),
                                                        fontSize = 12.sp,
                                                        color = Muted,
                                                        modifier = Modifier.width(28.dp),
                                                    )
                                                    Text(
                                                        previous.getOrNull(index)?.let {
                                                            "${it.weight} × ${it.reps}"
                                                        } ?: "—",
                                                        fontSize = 10.sp,
                                                        color = Muted,
                                                        modifier = Modifier.width(49.dp),
                                                    )
                                                    OutlinedTextField(
                                                        value = set.weight,
                                                        onValueChange = { text ->
                                                            updateActive { a ->
                                                                a.copy(
                                                                    sets =
                                                                        a.sets.map {
                                                                            if (it.id == set.id)
                                                                                it.copy(
                                                                                    weight = text,
                                                                                    done = false,
                                                                                )
                                                                            else it
                                                                        },
                                                                )
                                                            }
                                                        },
                                                        singleLine = true,
                                                        modifier =
                                                            Modifier.weight(1f).semantics {
                                                                contentDescription =
                                                                    "Carga, série ${index + 1}, ${e.name}"
                                                            },
                                                        keyboardOptions =
                                                            KeyboardOptions(
                                                                keyboardType = KeyboardType.Decimal,
                                                            ),
                                                        textStyle =
                                                            LocalTextStyle.current.copy(
                                                                fontSize = 13.sp,
                                                            ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors =
                                                            OutlinedTextFieldDefaults.colors(
                                                                focusedContainerColor =
                                                                    if (set.done) Color(0xFFEDF4DE)
                                                                    else Paper,
                                                                unfocusedContainerColor =
                                                                    if (set.done) Color(0xFFEDF4DE)
                                                                    else Paper,
                                                            ),
                                                    )
                                                    OutlinedTextField(
                                                        value = set.reps,
                                                        onValueChange = { text ->
                                                            updateActive { a ->
                                                                a.copy(
                                                                    sets =
                                                                        a.sets.map {
                                                                            if (it.id == set.id)
                                                                                it.copy(
                                                                                    reps = text,
                                                                                    done = false,
                                                                                )
                                                                            else it
                                                                        },
                                                                )
                                                            }
                                                        },
                                                        singleLine = true,
                                                        modifier =
                                                            Modifier.weight(1f).semantics {
                                                                contentDescription =
                                                                    "Repetições, série ${index + 1}, ${e.name}"
                                                            },
                                                        keyboardOptions =
                                                            KeyboardOptions(
                                                                keyboardType = KeyboardType.Number,
                                                            ),
                                                        textStyle =
                                                            LocalTextStyle.current.copy(
                                                                fontSize = 13.sp,
                                                            ),
                                                        shape = RoundedCornerShape(8.dp),
                                                    )
                                                    FilledIconToggleButton(
                                                        checked = set.done,
                                                        onCheckedChange = {
                                                            updateActive { toggleSet(it, set.id) }
                                                        },
                                                        enabled = set.done || validSet(set),
                                                        modifier = Modifier.size(44.dp),
                                                    ) {
                                                        Icon(
                                                            Icons.Outlined.Check,
                                                            "${if (set.done) "Desmarcar" else "Concluir"} série ${index + 1}",
                                                            Modifier.size(18.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        TextButton(
                                            onClick = {
                                                updateActive {
                                                    it.copy(
                                                        sets =
                                                            it.sets +
                                                                WorkoutSet(
                                                                    exerciseId = e.id,
                                                                    reps = spec.reps.toString(),
                                                                ),
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
                                            Text("Adicionar série", fontSize = 12.sp)
                                        }
                                    }
                                }
                                item {
                                    OutlinedButton(
                                        onClick = { picking = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(9.dp),
                                    ) {
                                        Icon(Icons.Outlined.Add, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Adicionar exercício")
                                    }
                                }
                                item {
                                    OutlinedTextField(
                                        value = active.note,
                                        onValueChange = { text ->
                                            updateActive { it.copy(note = text.take(2000)) }
                                        },
                                        label = { Text("Notas do treino") },
                                        placeholder = { Text("Como foi hoje?") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                }
                                item {
                                    PrimaryButton("Finalizar treino", Icons.Outlined.Check) {
                                        finishError = null
                                        finish = true
                                    }
                                }
                                item {
                                    TextButton(
                                        onClick = { discard = true },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            "Descartar este treino",
                                            color = Color(0xFFA76F5A),
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        } ?: LaunchedEffect(Unit) { screen = Screen.Home }
                }
                if (screen != Screen.Training && state.active != null)
                    Surface(
                        Modifier.align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { screen = Screen.Training },
                        color = Ink,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 4.dp,
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(Icons.Outlined.PlayCircle, null, tint = Lime)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    state.active!!.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "${clock(now - state.active!!.startedAt)} · Retomar treino",
                                    color = Color(0xFFBDCAA8),
                                    fontSize = 11.sp,
                                )
                            }
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Color.White)
                        }
                    }
            }
        }
        if (templateLibrary)
            RoutineTemplateLibrary(
                templates = store.routineTemplates,
                lookup = lookup,
                error = draftError,
                onDismiss = { templateLibrary = false },
                onUse = { template ->
                    if (openRoutineEditor(copyRoutineTemplate(template), isNew = true)) templateLibrary = false
                },
            )
        if (editorOpen)
            routineDraft?.let { session ->
                val current = state.routines.find { it.id == session.original.id }
                RoutineEditor(
                    session = session,
                    catalog = catalog,
                    error = draftError,
                    conflict = if (session.isNew) current != null else current != session.original,
                    onChange = { saveDraft(session.copy(draft = it)) },
                    onDismiss = { discardRoutineDraft() },
                    onKeep = { editorOpen = false },
                    onSave = { finishRoutineDraft() },
                    onSaveAsCopy = { finishRoutineDraft(asCopy = true) },
                )
            }
        archiving?.let { routine ->
            AlertDialog(
                onDismissRequest = { archiving = null },
                title = { Text("Arquivar ${routine.name}?") },
                text = {
                    Text("A rotina sairá da lista de treinos ativos. Você poderá restaurá-la em Arquivadas. Seu histórico e o treino em andamento serão mantidos.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (setArchived(routine.id, true)) archiving = null
                    }) { Text("Arquivar rotina") }
                },
                dismissButton = {
                    TextButton(onClick = { archiving = null }) { Text("Manter ativa") }
                },
            )
        }
        if (picking)
            ExercisePicker(
                catalog,
                state.active?.exercises?.map { it.exerciseId } ?: emptyList(),
                onDismiss = { picking = false },
            ) { exercise ->
                if (updateActive { addExercise(it, ExerciseSpec(exercise.id), state.workouts) }) picking = false
            }
        replacing?.let { original ->
            val selected = replacement
            if (selected == null) {
                ExercisePicker(
                    catalog,
                    state.active?.exercises?.map { it.exerciseId } ?: emptyList(),
                    title = "Substituir exercício",
                    onDismiss = { replacing = null },
                ) { replacement = it }
            } else {
                val oldSets = state.active?.sets?.filter { it.exerciseId == original.exerciseId } ?: emptyList()
                val kept = oldSets.count { it.done }
                val newCount = if (oldSets.isEmpty()) original.sets.coerceAtLeast(1) else oldSets.count { !it.done }
                AlertDialog(
                    onDismissRequest = {
                        replacing = null
                        replacement = null
                        replacementError = null
                    },
                    title = { Text("Usar ${selected.name}?") },
                    text = {
                        Column(
                            Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("As séries pendentes de ${lookup[original.exerciseId]?.name ?: original.exerciseId} serão substituídas por séries de ${selected.name}.")
                            Text("Os valores ainda não concluídos do exercício original serão removidos. As novas séries usam apenas o histórico do exercício escolhido e começam desmarcadas.")
                            Text("$newCount novas séries · descanso ${original.restSeconds}s", fontWeight = FontWeight.SemiBold)
                            if (kept > 0) Text("$kept séries já concluídas serão mantidas no exercício original.")
                            Text("A rotina salva e o descanso em andamento continuam iguais.", color = Muted, fontSize = 12.sp)
                            replacementError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            try {
                                if (updateActive {
                                    replaceWorkoutExercise(it, original.exerciseId, selected.id, state.workouts)
                                }) {
                                    replacing = null
                                    replacement = null
                                    replacementError = null
                                } else replacementError = "Não foi possível salvar a substituição. Seu treino foi mantido."
                            } catch (e: Exception) {
                                replacementError = e.message ?: "Não foi possível substituir o exercício."
                            }
                        }) { Text("Substituir pendentes") }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = {
                                replacing = null
                                replacement = null
                                replacementError = null
                            }) { Text("Cancelar") }
                            TextButton(onClick = {
                                replacement = null
                                replacementError = null
                            }) { Text("Escolher outro") }
                        }
                    },
                )
            }
        }
        detail?.let { exercise ->
            ExerciseDetails(
                exercise = exercise,
                workouts = state.workouts,
                active = state.active,
                onDismiss = { detail = null },
                onWorkout = { workout ->
                    detail = null
                    workoutDetail = workout
                },
                onTrain = {
                    val active = state.active
                    val alreadyPresent = active?.exercises?.any { it.exerciseId == exercise.id } == true
                    val saved = alreadyPresent || save(
                        state.copy(
                            active = addExercise(
                                active ?: startWorkout(null, state.workouts),
                                ExerciseSpec(exercise.id),
                                state.workouts,
                            ),
                        ),
                    )
                    if (saved) {
                        detail = null
                        screen = Screen.Training
                    }
                },
            )
        }
        if (finish)
            state.active?.let { active ->
                WorkoutFinishReview(
                    active = active,
                    lookup = lookup,
                    now = now,
                    error = finishError,
                    onDismiss = {
                        finish = false
                        finishError = null
                    },
                    onSave = {
                        try {
                            if (save(finishWorkout(state))) {
                                finish = false
                                finishError = null
                                screen = Screen.History
                            } else finishError = "Não foi possível salvar. O treino em andamento foi mantido; tente novamente."
                        } catch (e: Exception) {
                            finishError = e.message ?: "Não foi possível finalizar o treino."
                        }
                    },
                )
            }
        if (discard)
            AlertDialog(
                onDismissRequest = { discard = false },
                title = { Text("Descartar este treino?") },
                text = {
                    Text(
                        "As séries deste treino em andamento serão removidas. Seu histórico e suas rotinas continuam salvos.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (save(state.copy(active = null))) {
                                discard = false
                                screen = Screen.Home
                            }
                        },
                    ) {
                        Text("Descartar", color = Color(0xFFAA5646))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { discard = false }) { Text("Manter treino") }
                },
            )
        removing?.let { exercise ->
            AlertDialog(
                onDismissRequest = { removing = null },
                title = { Text("Remover exercício?") },
                text = {
                    Text("As séries de ${exercise.name} serão removidas deste treino em andamento.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (updateActive {
                                it.copy(
                                    exercises =
                                        it.exercises.filterNot { e -> e.exerciseId == exercise.id },
                                    sets = it.sets.filterNot { s -> s.exerciseId == exercise.id },
                                )
                            }) removing = null
                        },
                    ) {
                        Text("Remover")
                    }
                },
                dismissButton = { TextButton(onClick = { removing = null }) { Text("Manter") } },
            )
        }
        removingSet?.let { set ->
            AlertDialog(
                onDismissRequest = { removingSet = null },
                title = { Text("Remover esta série?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(lookup[set.exerciseId]?.name ?: set.exerciseId)
                        Text(
                            if (set.done)
                                "Esta série está concluída. Ao removê-la, ela deixará de contar no volume e no resumo deste treino."
                            else "Os valores desta série serão removidos do treino em andamento.",
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        state.active?.let { active ->
                            val next = active.copy(sets = active.sets.filterNot { it.id == set.id })
                            if (save(state.copy(active = next))) removingSet = null
                        }
                    }) { Text("Remover série") }
                },
                dismissButton = {
                    TextButton(onClick = { removingSet = null }) { Text("Manter série") }
                },
            )
        }
        restEditing?.let { spec ->
            AlertDialog(
                onDismissRequest = { restEditing = null },
                title = { Text("Descanso entre séries") },
                text = {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(lookup[spec.exerciseId]?.name ?: spec.exerciseId)
                        Text("Vale para as próximas séries deste exercício. O timer atual e a rotina original continuam iguais.", fontSize = 13.sp, color = Muted)
                        (listOf(0, 30, 60, 90, 120, 180, 300, 600) + spec.restSeconds)
                            .distinct().sorted().forEach { seconds ->
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        state.active?.let { active ->
                                            val next = active.copy(
                                                exercises = active.exercises.map {
                                                    if (it.exerciseId == spec.exerciseId)
                                                        it.copy(restSeconds = seconds)
                                                    else it
                                                },
                                            )
                                            if (save(state.copy(active = next))) restEditing = null
                                        }
                                    },
                                ) {
                                    Text(
                                        if (seconds == 0) "Sem timer" else "$seconds segundos",
                                        Modifier.weight(1f),
                                    )
                                    if (seconds == spec.restSeconds)
                                        Icon(Icons.Outlined.Check, "Descanso atual")
                                }
                            }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { restEditing = null }) { Text("Cancelar") }
                },
            )
        }
        workoutDetail?.let { workout ->
            FullScreenSheet(workout.title, { workoutDetail = null }) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text(date(workout.endedAt, includeYear = true), color = Muted)
                    Row {
                        Metric(
                            clock(workout.endedAt - workout.startedAt),
                            "tempo",
                            Modifier.weight(1f),
                        )
                        Metric(workout.sets.size.toString(), "séries", Modifier.weight(1f))
                        Metric(fmt(volume(workout.sets)), "kg", Modifier.weight(1f))
                    }
                    workout.sets
                        .groupBy { it.exerciseId }
                        .forEach { (exerciseId, sets) ->
                            Panel {
                                Text(
                                    lookup[exerciseId]?.name ?: exerciseId,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable(enabled = lookup[exerciseId] != null) {
                                        workoutDetail = null
                                        detail = lookup[exerciseId]
                                    },
                                )
                                sets.forEachIndexed { index, set ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("Série ${index + 1}", color = Muted)
                                        Text("${fmt(numeric(set.weight) ?: 0.0)} kg × ${set.reps}")
                                    }
                                }
                            }
                        }
                    if (workout.note.isNotEmpty()) Text(workout.note, color = Muted)
                    Text(
                        if (state.active != null)
                            "Você já tem um treino em andamento. Retome-o antes de repetir outro."
                        else "Repetir cria um novo treino com estas cargas e repetições, sem séries concluídas. Descanso inicial: 90s por exercício; ajuste no menu durante o treino.",
                        color = Muted,
                        fontSize = 12.sp,
                    )
                }
                PrimaryButton(
                    if (state.active != null) "Retomar treino em andamento" else "Repetir este treino",
                    Icons.Outlined.PlayArrow,
                    enabled = state.active != null || completed(workout.sets).isNotEmpty(),
                ) {
                    if (state.active != null || save(repeatWorkout(state, workout))) {
                        workoutDetail = null
                        screen = Screen.Training
                    }
                }
            }
        }
        if (syncOpen)
            AlertDialog(
                onDismissRequest = { if (!syncing) syncOpen = false },
                title = { Text("Seu servidor pessoal") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Seus treinos já estão salvos neste aparelho. Sincronize com a API pessoal para levá-los à web.",
                            fontSize = 13.sp,
                        )
                        OutlinedTextField(
                            value = server,
                            onValueChange = { server = it },
                            label = { Text("Endereço da API") },
                            placeholder = { Text("http://10.0.2.2:8097") },
                            singleLine = true,
                            enabled = !syncing,
                        )
                        if (state.lastSyncedAt != null)
                            Text(
                                "Última sincronização: ${date(state.lastSyncedAt!!)}",
                                fontSize = 11.sp,
                                color = Muted,
                            )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !syncing && server.isNotBlank(),
                        onClick = {
                            syncing = true
                            store.server = server
                            val snapshot = state
                            scope.launch {
                                try {
                                    val remote =
                                        withContext(Dispatchers.IO) {
                                            syncWorkspace(server, snapshot)
                                        }
                                    if (save(mergeWorkspace(state, remote))) {
                                        syncOpen = false
                                        snackbar.showSnackbar("Tudo sincronizado.")
                                    }
                                } catch (e: Exception) {
                                    snackbar.showSnackbar(
                                        "Servidor indisponível. Seus treinos continuam salvos aqui.",
                                    )
                                } finally {
                                    syncing = false
                                }
                            }
                        },
                    ) {
                        Text(if (syncing) "Sincronizando…" else "Sincronizar")
                    }
                },
                dismissButton = {
                    TextButton(enabled = !syncing, onClick = { syncOpen = false }) {
                        Text("Agora não")
                    }
                },
            )
    }
}

@Composable
private fun WorkoutProgress(review: WorkoutReview) {
    val fraction = if (review.totalCount == 0) 0f else review.completedCount.toFloat() / review.totalCount
    Panel {
        Text("${review.completedCount} de ${review.totalCount} séries concluídas", fontWeight = FontWeight.SemiBold)
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(Line).semantics {
                    contentDescription = "${review.completedCount} de ${review.totalCount} séries concluídas"
                },
        ) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(Lime))
        }
        if (review.totalCount == 0)
            Text("Adicione um exercício e registre suas séries para começar.", color = Muted, fontSize = 12.sp)
        else if (review.pendingCount > 0)
            Text("${review.pendingCount} séries ainda não marcadas como concluídas.", color = Muted, fontSize = 12.sp)
        if (review.invalidDoneCount > 0)
            Text(
                "${review.invalidDoneCount} séries marcadas têm dados inválidos e não entrarão no histórico. Revise carga e repetições.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
            )
    }
}

@Composable
private fun WorkoutFinishReview(
    active: ActiveWorkout,
    lookup: Map<String, Exercise>,
    now: Long,
    error: String?,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val review = remember(active) { reviewWorkout(active) }
    FullScreenSheet("Revisar treino", onDismiss) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Header(active.title.ifBlank { "Treino sem nome" }, "Confira as séries que vão entrar no histórico.")
            }
            if (error != null) item { Text(error, color = MaterialTheme.colorScheme.error) }
            item {
                Panel {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric(clock(now - active.startedAt), "tempo", Modifier.weight(1f))
                        Metric(review.completedCount.toString(), "séries a salvar", Modifier.weight(1f))
                    }
                    Metric(fmt(review.totalVolume), "kg de volume")
                    if (review.excludedCount > 0)
                        Text(
                            "${review.excludedCount} séries não serão salvas: ${review.pendingCount} não concluídas e ${review.invalidDoneCount} marcadas com dados inválidos. Volte ao treino para revisá-las.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                        )
                    if (review.completedCount == 0)
                        Text("Conclua ao menos uma série válida antes de salvar.", color = MaterialTheme.colorScheme.error)
                    if (!workoutTitleValid(active.title))
                        Text("Volte ao treino e dê um nome antes de salvar.", color = MaterialTheme.colorScheme.error)
                }
            }
            items(review.exercises, key = { it.exerciseId }) { exercise ->
                val numbers = active.sets.filter { it.exerciseId == exercise.exerciseId }
                    .mapIndexed { index, set -> set.id to index + 1 }.toMap()
                Panel {
                    Text(lookup[exercise.exerciseId]?.name ?: exercise.exerciseId, fontWeight = FontWeight.SemiBold)
                    if (exercise.includedSets.isEmpty())
                        Text("Nenhuma série deste exercício será salva.", color = Muted, fontSize = 13.sp)
                    exercise.includedSets.forEachIndexed { index, set ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Série ${numbers[set.id] ?: index + 1}", color = Muted, fontSize = 13.sp)
                            Text("${fmt(numeric(set.weight) ?: 0.0)} kg × ${set.reps}", fontSize = 13.sp)
                        }
                    }
                    if (exercise.excludedCount > 0)
                        Text("${exercise.excludedCount} séries não entrarão no histórico.", color = Muted, fontSize = 12.sp)
                }
            }
            if (active.note.isNotEmpty()) item {
                Panel {
                    Eyebrow("NOTAS DO TREINO")
                    Text(active.note, fontSize = 14.sp)
                }
            }
        }
        PrimaryButton(
            "Salvar treino", Icons.Outlined.Check,
            enabled = review.completedCount > 0 && workoutTitleValid(active.title),
            onClick = onSave,
        )
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Continuar treinando")
        }
    }
}

@Composable
private fun ExerciseDetails(
    exercise: Exercise,
    workouts: List<Workout>,
    active: ActiveWorkout?,
    onDismiss: () -> Unit,
    onWorkout: (Workout) -> Unit,
    onTrain: () -> Unit,
) {
    val history = remember(workouts, exercise.id) { exerciseHistory(workouts, exercise.id) }
    FullScreenSheet(exercise.name, onDismiss) {
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            item {
                Panel {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Glyph(exercise)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(exercise.muscle, fontWeight = FontWeight.SemiBold)
                            Text(exercise.equipment, color = Muted, fontSize = 12.sp)
                        }
                    }
                    Text(exercise.description, fontSize = 14.sp, lineHeight = 21.sp)
                }
            }
            if (history.isEmpty()) {
                item {
                    Panel {
                        Eyebrow("SEU HISTÓRICO")
                        Text("Seu primeiro registro começa no treino.", fontWeight = FontWeight.SemiBold)
                        Text(
                            "As séries válidas que você concluir e salvar neste exercício aparecerão aqui.",
                            color = Muted,
                            fontSize = 13.sp,
                        )
                    }
                }
            } else {
                item {
                    Panel {
                        Eyebrow("SEUS REGISTROS NESTE EXERCÍCIO")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Metric(history.size.toString(), "sessões", Modifier.weight(1f))
                            Metric(
                                fmt(history.maxOf { session -> session.sets.maxOf { numeric(it.weight) ?: 0.0 } }),
                                "kg · maior carga",
                                Modifier.weight(1f),
                            )
                        }
                        Metric(
                            fmt(history.maxOf { volume(it.sets) }),
                            "kg de volume · maior numa sessão",
                        )
                        Text(
                            "Valores de séries concluídas e salvas. Volume = carga × repetições.",
                            color = Muted,
                            fontSize = 12.sp,
                        )
                    }
                }
                item { Eyebrow("SESSÕES · MAIS RECENTES PRIMEIRO") }
                items(history, key = { it.id }) { session ->
                    Panel {
                        TextButton(onClick = {
                            workouts.firstOrNull { it.id == session.id }?.let(onWorkout)
                        }) {
                            Column(Modifier.weight(1f)) {
                                Text(session.title, fontWeight = FontWeight.SemiBold)
                                Text(date(session.endedAt, includeYear = true), color = Muted, fontSize = 12.sp)
                            }
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Abrir treino completo")
                        }
                        session.sets.forEachIndexed { index, set ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Série ${index + 1}", color = Muted, fontSize = 13.sp)
                                Text("${fmt(numeric(set.weight) ?: 0.0)} kg × ${set.reps}", fontSize = 13.sp)
                            }
                        }
                        Text("${fmt(volume(session.sets))} kg de volume", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
        PrimaryButton(
            when {
                active?.exercises?.any { it.exerciseId == exercise.id } == true -> "Voltar ao treino"
                active != null -> "Adicionar ao treino"
                else -> "Treinar este exercício"
            },
            Icons.Outlined.PlayArrow,
            onClick = onTrain,
        )
    }
}

@Composable
private fun Hero(onClick: () -> Unit) {
    Surface(color = Lime, shape = RoundedCornerShape(13.dp)) {
        Box(Modifier.fillMaxWidth().height(283.dp)) {
            Column(Modifier.padding(25.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "UM TREINO DE CADA VEZ",
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "FORÇA É\nCONSTÂNCIA",
                    fontSize = 42.sp,
                    lineHeight = 40.sp,
                    letterSpacing = (-2).sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Um lugar para planejar, registrar\ne enxergar sua evolução.",
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF657644),
                )
                Button(
                    onClick,
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Ink,
                        ),
                ) {
                    Text("Escolher meu treino", fontSize = 11.sp)
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp))
                }
            }
            Canvas(Modifier.size(105.dp).align(Alignment.BottomEnd).offset(32.dp, 5.dp)) {
                rotate(-30f) {
                    val color = Color(0xFF2B442A)
                    drawRoundRect(
                        color,
                        Offset(size.width * .1f, size.height * .44f),
                        Size(size.width * .8f, size.height * .14f),
                        CornerRadius(5f),
                    )
                    drawRoundRect(
                        color,
                        Offset(size.width * .15f, size.height * .2f),
                        Size(size.width * .2f, size.height * .6f),
                        CornerRadius(10f),
                    )
                    drawRoundRect(
                        color,
                        Offset(size.width * .65f, size.height * .2f),
                        Size(size.width * .2f, size.height * .6f),
                        CornerRadius(10f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    lookup: Map<String, Exercise>,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
) {
    var menuOpen by remember(routine.id) { mutableStateOf(false) }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    routine.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-.8).sp,
                )
                Text(routine.note, fontSize = 12.sp, color = Muted)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, "Editar ${routine.name}", Modifier.size(19.dp))
            }
            if (onDuplicate != null || (!routine.archived && onArchive != null))
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, "Opções de ${routine.name}")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (onDuplicate != null)
                            DropdownMenuItem(
                                text = { Text("Duplicar rotina") },
                                onClick = {
                                    menuOpen = false
                                    onDuplicate()
                                },
                            )
                        if (!routine.archived && onArchive != null)
                            DropdownMenuItem(
                                text = { Text("Arquivar rotina") },
                                onClick = {
                                    menuOpen = false
                                    onArchive()
                                },
                            )
                    }
                }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            routine.exercises
                .mapNotNull { lookup[it.exerciseId]?.muscle }
                .distinct()
                .forEach { muscle ->
                    Surface(color = muscleColor(muscle), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            muscle,
                            Modifier.padding(7.dp, 4.dp),
                            fontSize = 9.sp,
                            color = Color(0xFF5C6E46),
                        )
                    }
                }
        }
        routine.exercises.take(3).forEach { spec ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    lookup[spec.exerciseId]?.name ?: spec.exerciseId,
                    fontSize = 12.sp,
                    color = Color(0xFF78886A),
                    modifier = Modifier.weight(1f),
                )
                Text("${spec.sets} × ${spec.reps}", fontSize = 11.sp, color = Muted)
            }
        }
        if (routine.exercises.size > 3)
            Text("+${routine.exercises.size - 3} exercício", fontSize = 11.sp, color = Muted)
        HorizontalDivider(color = Line)
        Text(
            "${routine.exercises.size} exercícios · ${routine.exercises.sumOf { it.sets }} séries",
            fontSize = 11.sp,
            color = Muted,
        )
        if (routine.archived) {
            Text("Rotina arquivada", color = Muted, fontSize = 12.sp)
            if (onRestore != null)
                PrimaryButton("Restaurar rotina", Icons.Outlined.Refresh, onClick = onRestore)
        } else PrimaryButton("Começar treino", onClick = onStart)
    }
}

@Composable
private fun HistoryRow(workout: Workout, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(color = Paper, shape = RoundedCornerShape(8.dp)) {
            Text(date(workout.endedAt), Modifier.padding(10.dp), fontSize = 10.sp, color = Muted)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(workout.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${workout.sets.size} séries · ${fmt(volume(workout.sets))} kg",
                fontSize = 11.sp,
                color = Muted,
            )
        }
        Icon(Icons.Outlined.ArrowOutward, null, Modifier.size(19.dp), tint = Muted)
    }
}

@Composable
private fun Library(catalog: List<Exercise>, active: Boolean, onSelect: (Exercise) -> Unit) {
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("Todos") }
    val matches = catalog.filter {
        (muscle == "Todos" || it.muscle == muscle) &&
            normalized("${it.name} ${it.equipment}").contains(normalized(query))
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(22.dp, 16.dp, 22.dp, if (active) 100.dp else 25.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        item { Header("Biblioteca de exercícios.", "Encontre seu próximo movimento.") }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                placeholder = { Text("Buscar exercício", fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(9.dp),
            )
        }
        item { MuscleChips(muscle) { muscle = it } }
        if (matches.isEmpty())
            item {
                Text(
                    "Nenhum exercício encontrado. Tente outro termo ou grupo muscular.",
                    color = Muted,
                )
            }
        items(matches, key = { it.id }) { exercise ->
            ExerciseRow(exercise, false) { onSelect(exercise) }
        }
    }
}

@Composable
private fun MuscleChips(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Muscles.forEach {
            FilterChip(
                selected = selected == it,
                onClick = { onSelect(it) },
                label = { Text(it, fontSize = 11.sp) },
                colors =
                    FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE6EED7)),
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected == it,
                        borderColor = Line,
                        selectedBorderColor = Line,
                    ),
            )
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().clickable(enabled = !selected, onClick = onClick),
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Glyph(exercise, 54)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(exercise.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${exercise.muscle} · ${exercise.equipment}", fontSize = 11.sp, color = Muted)
            }
            Icon(
                if (selected) Icons.Outlined.Check else Icons.Outlined.ArrowOutward,
                null,
                Modifier.size(18.dp),
                tint = Muted,
            )
        }
    }
}

@Composable
private fun FullScreenSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Paper) {
            Column(
                Modifier.safeDrawingPadding().padding(20.dp).imePadding(),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Fechar janela") }
                }
                content()
            }
        }
    }
}

@Composable
private fun ExercisePicker(
    catalog: List<Exercise>,
    existing: List<String>,
    title: String = "Adicionar exercício",
    onDismiss: () -> Unit,
    onSelect: (Exercise) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("Todos") }
    FullScreenSheet(title, onDismiss) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text("Buscar exercício") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(9.dp),
        )
        MuscleChips(muscle) { muscle = it }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(
                catalog.filter {
                    (muscle == "Todos" || it.muscle == muscle) &&
                        normalized("${it.name} ${it.equipment}").contains(normalized(query))
                },
                key = { it.id },
            ) { e ->
                ExerciseRow(e, e.id in existing) { onSelect(e) }
            }
        }
    }
}

@Composable
private fun RoutineTemplateLibrary(
    templates: List<Routine>,
    lookup: Map<String, Exercise>,
    error: String?,
    onDismiss: () -> Unit,
    onUse: (Routine) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val shown = templates.filter { template ->
        normalized(template.name + " " + template.note + " " + template.exercises.joinToString(" ") {
            lookup[it.exerciseId]?.let { exercise -> "${exercise.name} ${exercise.muscle} ${exercise.equipment}" } ?: it.exerciseId
        }).contains(normalized(query))
    }
    FullScreenSheet("Modelos de rotina", onDismiss) {
        Text(
            "Exemplos editáveis, não um plano personalizado. Escolha e ajuste os exercícios; não é necessário realizar todos. Usar modelo abre uma nova cópia para editar antes de salvar.",
            color = Muted,
            fontSize = 13.sp,
        )
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Buscar por rotina, exercício ou equipamento") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            if (shown.isEmpty()) item { Text("Nenhum modelo encontrado. Tente outro termo.", color = Muted) }
            items(shown, key = { it.id }) { template ->
                Panel {
                    Text(template.name, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                    Text(template.note, color = Muted, fontSize = 12.sp)
                    Eyebrow("${template.exercises.size} EXERCÍCIOS · VALORES INICIAIS EDITÁVEIS")
                    template.exercises.forEach { spec ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(lookup[spec.exerciseId]?.name ?: spec.exerciseId, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("${spec.sets} × ${spec.reps} · descanso ${spec.restSeconds}s", color = Muted, fontSize = 12.sp)
                        }
                    }
                    PrimaryButton("Usar modelo", Icons.Outlined.Add) { onUse(template) }
                }
            }
        }
    }
}

@Composable
private fun RoutineDraftNotice(draft: RoutineDraft?, error: String?, onResume: () -> Unit) {
    Panel {
        Eyebrow("RASCUNHO DE ROTINA")
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        if (draft != null) {
            Text(draft.draft.name.ifBlank { "Rotina sem nome" }, fontWeight = FontWeight.SemiBold)
            Text(
                "Você tem uma edição guardada neste aparelho. Ela ainda não alterou suas rotinas salvas.",
                color = Muted,
                fontSize = 13.sp,
            )
            PrimaryButton("Retomar edição", Icons.Outlined.Edit, onClick = onResume)
        }
    }
}

@Composable
private fun RoutineEditor(
    session: RoutineDraft,
    catalog: List<Exercise>,
    error: String?,
    conflict: Boolean,
    onChange: (Routine) -> Boolean,
    onDismiss: () -> Unit,
    onKeep: () -> Unit,
    onSave: () -> Unit,
    onSaveAsCopy: () -> Unit,
) {
    val original = session.original
    val draft = session.draft
    var picking by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val lookup = catalog.associateBy { it.id }
    val requestDismiss: () -> Unit = {
        if (draft != original) confirmDiscard = true else onDismiss()
    }
    FullScreenSheet(if (session.isNew) "Criar rotina" else "Editar rotina", requestDismiss) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "Rascunho salvo neste aparelho a cada alteração. Use Continuar depois para retomar mais tarde; ele só entra nas suas rotinas ao salvar.",
                color = Muted,
                fontSize = 12.sp,
            )
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            if (conflict) {
                Text(
                    "A rotina original mudou desde que você abriu este rascunho. Salve como nova rotina para preservar as duas versões.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
                OutlinedButton(onClick = onSaveAsCopy, enabled = routineValid(draft)) {
                    Text("Salvar como nova rotina")
                }
            }
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it.take(80))) },
                label = { Text("Nome da rotina") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(9.dp),
            )
            OutlinedTextField(
                value = draft.note,
                onValueChange = { onChange(draft.copy(note = it.take(200))) },
                label = { Text("Descrição (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(9.dp),
            )
            draft.exercises.forEachIndexed { index, spec ->
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            lookup[spec.exerciseId]?.name ?: spec.exerciseId,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            enabled = index > 0,
                            onClick = {
                                val list = draft.exercises.toMutableList()
                                list[index - 1] = list[index].also { list[index] = list[index - 1] }
                                onChange(draft.copy(exercises = list))
                            },
                        ) {
                            Icon(
                                Icons.Outlined.ArrowUpward,
                                "Mover exercício para cima",
                                Modifier.size(17.dp),
                            )
                        }
                        IconButton(
                            enabled = index < draft.exercises.lastIndex,
                            onClick = {
                                val list = draft.exercises.toMutableList()
                                val next = list[index + 1]
                                list[index + 1] = list[index]
                                list[index] = next
                                onChange(draft.copy(exercises = list))
                            },
                        ) {
                            Icon(
                                Icons.Outlined.ArrowDownward,
                                "Mover exercício para baixo",
                                Modifier.size(17.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                onChange(
                                    draft.copy(
                                        exercises =
                                            draft.exercises.filterNot {
                                                it.exerciseId == spec.exerciseId
                                            },
                                    ),
                                )
                            },
                        ) {
                            Icon(Icons.Outlined.Close, "Remover exercício", Modifier.size(17.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = if (spec.sets == 0) "" else spec.sets.toString(),
                            onValueChange = { value ->
                                onChange(
                                    draft.copy(
                                        exercises =
                                            draft.exercises.mapIndexed { i, e ->
                                                if (i == index)
                                                    e.copy(sets = value.toIntOrNull() ?: 0)
                                                else e
                                            },
                                    ),
                                )
                            },
                            label = { Text("Séries", fontSize = 11.sp) },
                            isError = spec.sets !in 1..20,
                            supportingText = {
                                if (spec.sets !in 1..20) Text("Use 1 a 20 séries")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = if (spec.reps == 0) "" else spec.reps.toString(),
                            onValueChange = { value ->
                                onChange(
                                    draft.copy(
                                        exercises =
                                            draft.exercises.mapIndexed { i, e ->
                                                if (i == index)
                                                    e.copy(reps = value.toIntOrNull() ?: 0)
                                                else e
                                            },
                                    ),
                                )
                            },
                            label = { Text("Reps", fontSize = 11.sp) },
                            isError = spec.reps !in 1..1000,
                            supportingText = {
                                if (spec.reps !in 1..1000) Text("Use 1 a 1000 reps")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { menu = true }) {
                            Icon(Icons.Outlined.Timer, null, Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Descanso: ${spec.restSeconds}s", fontSize = 12.sp)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            listOf(0, 30, 60, 90, 120, 180, 300).forEach { seconds ->
                                DropdownMenuItem(
                                    text = {
                                        Text(if (seconds == 0) "Sem timer" else "$seconds segundos")
                                    },
                                    onClick = {
                                        onChange(
                                            draft.copy(
                                                exercises =
                                                    draft.exercises.mapIndexed { i, e ->
                                                        if (i == index)
                                                            e.copy(restSeconds = seconds)
                                                        else e
                                                    },
                                            ),
                                        )
                                        menu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { picking = true },
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(9.dp),
            ) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Adicionar exercício")
            }
        }
        PrimaryButton("Salvar rotina", Icons.Outlined.Check, routineValid(draft) && !conflict) {
            onSave()
        }
        OutlinedButton(onClick = onKeep, modifier = Modifier.fillMaxWidth()) {
            Text("Continuar depois")
        }
    }
    if (picking)
        ExercisePicker(catalog, draft.exercises.map { it.exerciseId }, onDismiss = { picking = false }) {
            if (onChange(draft.copy(exercises = draft.exercises + ExerciseSpec(it.id)))) picking = false
        }
    if (confirmDiscard)
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Descartar alterações?") },
            text = { Text("Estas alterações ainda não foram aplicadas à rotina. Descartar também remove o rascunho salvo neste aparelho.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onDismiss()
                }) { Text("Descartar alterações") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Continuar editando") }
            },
        )
}
