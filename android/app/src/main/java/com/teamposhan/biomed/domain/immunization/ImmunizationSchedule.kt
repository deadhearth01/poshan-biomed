package com.teamposhan.biomed.domain.immunization

/**
 * National Immunization Schedule (India) — Government schedule.
 * IAP doses are marked so the UI can show both.
 */
data class VaccineDose(
    val id: String,
    val name: String,
    val dueAgeDays: Int,
    val group: String,
    val iapOnly: Boolean = false,
)

enum class DoseStatus { GIVEN, OVERDUE, DUE_SOON, UPCOMING }

data class DoseState(
    val dose: VaccineDose,
    val status: DoseStatus,
)

object ImmunizationSchedule {

    private const val WEEK = 7
    private const val MONTH = 30

    val doses: List<VaccineDose> = listOf(
        VaccineDose("bcg", "BCG", 0, "At birth"),
        VaccineDose("opv0", "bOPV-0", 0, "At birth"),
        VaccineDose("hepb0", "Hepatitis B birth dose", 0, "At birth"),

        VaccineDose("opv1", "bOPV-1", 6 * WEEK, "6 weeks"),
        VaccineDose("penta1", "Pentavalent-1", 6 * WEEK, "6 weeks"),
        VaccineDose("fipv1", "fIPV-1", 6 * WEEK, "6 weeks"),
        VaccineDose("rvv1", "RVV-1", 6 * WEEK, "6 weeks"),
        VaccineDose("pcv1", "PCV-1", 6 * WEEK, "6 weeks"),

        VaccineDose("opv2", "bOPV-2", 10 * WEEK, "10 weeks"),
        VaccineDose("penta2", "Pentavalent-2", 10 * WEEK, "10 weeks"),
        VaccineDose("rvv2", "RVV-2", 10 * WEEK, "10 weeks"),

        VaccineDose("opv3", "bOPV-3", 14 * WEEK, "14 weeks"),
        VaccineDose("penta3", "Pentavalent-3", 14 * WEEK, "14 weeks"),
        VaccineDose("fipv2", "fIPV-2", 14 * WEEK, "14 weeks"),
        VaccineDose("rvv3", "RVV-3", 14 * WEEK, "14 weeks"),
        VaccineDose("pcv2", "PCV-2", 14 * WEEK, "14 weeks"),

        VaccineDose("mr1", "MR-1", 9 * MONTH, "9–11 months"),
        VaccineDose("je1", "JE-1 (endemic areas)", 9 * MONTH, "9–11 months"),
        VaccineDose("pcvb", "PCV Booster", 9 * MONTH, "9–11 months"),
        VaccineDose("fipv3", "fIPV-3", 9 * MONTH, "9–11 months"),
        VaccineDose("vita1", "Vitamin A (1st dose)", 9 * MONTH, "9–11 months"),

        VaccineDose("mr2", "MR-2", 16 * MONTH, "16–23 months"),
        VaccineDose("je2", "JE-2 (endemic areas)", 16 * MONTH, "16–23 months"),
        VaccineDose("dptb1", "DPT Booster-1", 16 * MONTH, "16–23 months"),
        VaccineDose("opvb", "bOPV Booster", 16 * MONTH, "16–23 months"),
        VaccineDose("vita2", "Vitamin A (2nd–9th dose)", 16 * MONTH, "16–23 months"),

        VaccineDose("dptb2", "DPT Booster-2", 5 * 12 * MONTH, "5–6 years"),
        VaccineDose("td10", "Td", 10 * 12 * MONTH, "10 years"),
        VaccineDose("td16", "Td", 16 * 12 * MONTH, "16 years"),

        // IAP additions commonly given alongside the national schedule
        VaccineDose("iap_ipv", "IPV (IAP)", 6 * WEEK, "IAP", iapOnly = true),
        VaccineDose("iap_hib", "Hib (IAP)", 6 * WEEK, "IAP", iapOnly = true),
        VaccineDose("iap_var1", "Varicella-1 (IAP)", 15 * MONTH, "IAP", iapOnly = true),
        VaccineDose("iap_hepa1", "Hepatitis A-1 (IAP)", 12 * MONTH, "IAP", iapOnly = true),
        VaccineDose("iap_infl", "Influenza (IAP)", 6 * MONTH, "IAP", iapOnly = true),
    )

    fun statusFor(ageDays: Int, given: Set<String>, includeIap: Boolean = false): List<DoseState> =
        doses.filter { includeIap || !it.iapOnly }.map { dose ->
            val status = when {
                given.contains(dose.id) -> DoseStatus.GIVEN
                ageDays > dose.dueAgeDays + 30 -> DoseStatus.OVERDUE
                ageDays >= dose.dueAgeDays - 15 -> DoseStatus.DUE_SOON
                else -> DoseStatus.UPCOMING
            }
            DoseState(dose, status)
        }

    /** Fraction of eligible (due) doses that are overdue — feeds the composite score. */
    fun overdueFraction(ageDays: Int, given: Set<String>): Float {
        val due = doses.filter { !it.iapOnly && ageDays > it.dueAgeDays + 30 }
        if (due.isEmpty()) return 0f
        val overdue = due.count { !given.contains(it.id) }
        return overdue.toFloat() / due.size
    }

    fun overdueCount(ageDays: Int, given: Set<String>): Int =
        doses.count { !it.iapOnly && ageDays > it.dueAgeDays + 30 && !given.contains(it.id) }
}
