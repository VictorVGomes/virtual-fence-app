package com.victorgomes.geofenceapp.utils

import java.util.Calendar

object HolidayUtils {

    // Fixed-date Brazilian national holidays (month, day).
    private val fixed = setOf(
        1 to 1,   // New Year's Day
        4 to 21,  // Tiradentes
        5 to 1,   // Labour Day
        9 to 7,   // Independence Day
        10 to 12, // Nossa Senhora Aparecida
        11 to 2,  // Finados
        11 to 15, // Proclamação da República
        12 to 25, // Christmas
    )

    // Cache movable holidays per year to avoid recalculating on every event.
    private val movableCache = HashMap<Int, Set<Pair<Int, Int>>>()

    fun isHoliday(ms: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        val month = cal.get(Calendar.MONTH) + 1
        val day   = cal.get(Calendar.DAY_OF_MONTH)
        val year  = cal.get(Calendar.YEAR)
        return (month to day) in fixed || (month to day) in movableFor(year)
    }

    private fun movableFor(year: Int): Set<Pair<Int, Int>> =
        movableCache.getOrPut(year) {
            val easter = easterSunday(year)
            buildSet {
                // Good Friday: 2 days before Easter
                val gf = easter.clone() as Calendar
                gf.add(Calendar.DAY_OF_YEAR, -2)
                add((gf.get(Calendar.MONTH) + 1) to gf.get(Calendar.DAY_OF_MONTH))
                // Corpus Christi: 60 days after Easter
                val cc = easter.clone() as Calendar
                cc.add(Calendar.DAY_OF_YEAR, 60)
                add((cc.get(Calendar.MONTH) + 1) to cc.get(Calendar.DAY_OF_MONTH))
            }
        }

    // Anonymous Gregorian algorithm for Easter Sunday.
    private fun easterSunday(year: Int): Calendar {
        val a = year % 19
        val b = year / 100;  val c = year % 100
        val d = b / 4;       val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4;       val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day   = (h + l - 7 * m + 114) % 31 + 1
        return Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
