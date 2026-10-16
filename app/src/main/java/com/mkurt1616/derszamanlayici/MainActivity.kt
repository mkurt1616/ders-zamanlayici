package com.mkurt1616.derszamanlayici

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DersZamanlayiciApp()
        }
    }
}

@Composable
fun DersZamanlayiciApp() {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    val state = remember(nowMs) { getCurrentScheduleState(nowMs) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7CC9FF),
            secondary = Color(0xFFB9E3FF),
            background = Color(0xFF0B1020),
            surface = Color(0xFF121A2B),
            onPrimary = Color(0xFF07111D),
            onBackground = Color(0xFFEAF5FF),
            onSurface = Color(0xFFEAF5FF)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B1020))
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.dayName,
                        fontSize = 18.sp,
                        color = Color(0xFFBFCFEA),
                        letterSpacing = 1.8.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = state.lessonName,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEAF5FF),
                        textAlign = TextAlign.Center,
                        lineHeight = 54.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = state.detail,
                        fontSize = 22.sp,
                        color = Color(0xFF7CC9FF),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121A2B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 26.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "KALAN SÜRE",
                                fontSize = 14.sp,
                                color = Color(0xFFBFCFEA),
                                letterSpacing = 1.4.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${state.remainingMinutes} dk",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEAF5FF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Bitiş: ${state.endText}",
                        fontSize = 18.sp,
                        color = Color(0xFFBFCFEA),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private data class ScheduleSegment(
    val type: String,
    val title: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val detail: String,
    val endText: String,
    val lessonNumber: Int? = null
)

private data class ScheduleState(
    val dayName: String,
    val lessonName: String,
    val detail: String,
    val remainingMinutes: Int,
    val endText: String
)

private fun getCurrentScheduleState(nowMs: Long): ScheduleState {
    val calendar = Calendar.getInstance(Locale("tr", "TR"))
    calendar.timeInMillis = nowMs

    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val dayName = when (dayOfWeek) {
        Calendar.MONDAY -> "PAZARTESİ"
        Calendar.TUESDAY -> "SALI"
        Calendar.WEDNESDAY -> "ÇARŞAMBA"
        Calendar.THURSDAY -> "PERŞEMBE"
        Calendar.FRIDAY -> "CUMA"
        Calendar.SATURDAY -> "CUMARTESİ"
        else -> "PAZAR"
    }

    val segments = when (dayOfWeek) {
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY -> mondayToThursdaySchedule()
        Calendar.FRIDAY -> fridaySchedule()
        else -> emptyList()
    }

    val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

    val current = segments.firstOrNull { nowMinutes >= it.startMinutes && nowMinutes < it.endMinutes }

    if (current != null) {
        val remaining = max(0, current.endMinutes - nowMinutes)
        val displayName = if (current.type == "break") "TENEFFÜS" else current.title
        return ScheduleState(
            dayName = dayName,
            lessonName = displayName,
            detail = if (current.type == "break") current.detail else current.detail,
            remainingMinutes = remaining,
            endText = timeFromMinutes(current.endMinutes)
        )
    }

    val next = segments.firstOrNull { nowMinutes < it.startMinutes }
    if (next != null) {
        val remaining = next.startMinutes - nowMinutes
        return ScheduleState(
            dayName = dayName,
            lessonName = "SONRAKİ",
            detail = next.detail,
            remainingMinutes = remaining,
            endText = timeFromMinutes(next.startMinutes)
        )
    }

    return ScheduleState(
        dayName = dayName,
        lessonName = "GÜN BİTTİ",
        detail = "Program tamamlandı",
        remainingMinutes = 0,
        endText = "-"
    )
}

private fun mondayToThursdaySchedule(): List<ScheduleSegment> {
    val dayStart = 8 * 60 + 30
    val segments = mutableListOf<ScheduleSegment>()

    val blocks = listOf(
        Triple(1, 2, 80),
        Triple(3, 4, 80),
        Triple(5, 5, 40),
        Triple(6, 7, 80),
        Triple(8, 9, 80),
        Triple(10, 10, 40)
    )

    var cursor = dayStart
    for ((from, to, duration) in blocks) {
        val blockEnd = cursor + duration
        val lessonLabel = if (from == to) "Ders $from" else "Ders ${from}-${to}"
        segments.add(
            ScheduleSegment(
                type = "lesson",
                title = "Ders $from",
                startMinutes = cursor,
                endMinutes = blockEnd,
                detail = "$lessonLabel · ${timeFromMinutes(cursor)} - ${timeFromMinutes(blockEnd)}",
                endText = timeFromMinutes(blockEnd),
                lessonNumber = from
            )
        )

        if (from != to) {
            val secondLessonStart = cursor + (duration / 2)
            val secondLessonEnd = blockEnd
            segments.add(
                ScheduleSegment(
                    type = "lesson",
                    title = "Ders $to",
                    startMinutes = secondLessonStart,
                    endMinutes = secondLessonEnd,
                    detail = "$lessonLabel · ${timeFromMinutes(secondLessonStart)} - ${timeFromMinutes(secondLessonEnd)}",
                    endText = timeFromMinutes(secondLessonEnd),
                    lessonNumber = to
                )
            )
        }

        if (to == 10) {
            break
        }

        val breakStart = blockEnd
        val breakEnd = when (from) {
            2 -> breakStart + 20
            4 -> breakStart + 20
            5 -> breakStart + 45
            7 -> breakStart + 20
            9 -> breakStart + 20
            else -> breakStart + 20
        }

        segments.add(
            ScheduleSegment(
                type = "break",
                title = "TENEFFÜS",
                startMinutes = breakStart,
                endMinutes = breakEnd,
                detail = "Teneffüs · ${timeFromMinutes(breakStart)} - ${timeFromMinutes(breakEnd)}",
                endText = timeFromMinutes(breakEnd)
            )
        )
        cursor = breakEnd
    }

    return segments
}

private fun fridaySchedule(): List<ScheduleSegment> {
    val segments = mutableListOf<ScheduleSegment>()

    val blockDefinitions = listOf(
        1 to Pair(8 * 60 + 30, 10 * 60),
        3 to Pair(10 * 60 + 20, 11 * 60 + 40),
        5 to Pair(12 * 60, 13 * 60 + 15),
        7 to Pair(13 * 60 + 15 + 50, 15 * 60 + 25),
        9 to Pair(15 * 60 + 25 + 25, 17 * 60 + 15)
    )

    for ((lessonNumber, pair) in blockDefinitions) {
        val start = pair.first
        val end = pair.second
        val display = "Ders $lessonNumber"
        segments.add(
            ScheduleSegment(
                type = "lesson",
                title = display,
                startMinutes = start,
                endMinutes = end,
                detail = "${display} · ${timeFromMinutes(start)} - ${timeFromMinutes(end)}",
                endText = timeFromMinutes(end),
                lessonNumber = lessonNumber
            )
        )

        val nextBreakStart = end
        val nextBreakEnd = when (lessonNumber) {
            1 -> start + 90 + 20
            3 -> start + 80 + 20
            5 -> start + 75 + 50
            7 -> start + 80 + 25
            else -> end
        }

        if (lessonNumber != 9) {
            segments.add(
                ScheduleSegment(
                    type = "break",
                    title = "TENEFFÜS",
                    startMinutes = nextBreakStart,
                    endMinutes = nextBreakEnd,
                    detail = "Teneffüs · ${timeFromMinutes(nextBreakStart)} - ${timeFromMinutes(nextBreakEnd)}",
                    endText = timeFromMinutes(nextBreakEnd)
                )
            )
        }
    }

    return segments
}

private fun timeFromMinutes(totalMinutes: Int): String {
    val hour = totalMinutes / 60
    val minute = totalMinutes % 60
    val h = hour.toString().padStart(2, '0')
    val m = minute.toString().padStart(2, '0')
    return "$h:$m"
}
