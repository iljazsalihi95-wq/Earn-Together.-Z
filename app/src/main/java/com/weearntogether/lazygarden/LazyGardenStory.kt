package com.weearntogether.lazygarden

data class GardenStoryStep(
    val level: Int,
    val speaker: String,
    val before: String,
    val buildTask: String,
    val after: String,
    val cost: Int,
    val sceneLabel: String
)

object LazyGardenStory {
    private const val CAMPAIGN_LEVELS = 250
    private val areas = listOf(
        "Entrance Garden","Fountain Court","Rose Terrace","Orchard Walk","Greenhouse","Hidden Courtyard","Riverside","Moon Garden","Old Estate","Lakeside",
        "Butterfly Garden","Herb Court","Woodland Path","Sun Terrace","Kitchen Garden","Water Garden","Hill Garden","Secret Grove","Winter Garden","Grand Promenade",
        "Magnolia Court","Library Garden","Cedar Walk","Festival Lawn","Grand Vista"
    )
    private val features = listOf(
        "garden gate","stone path","flower bed","wooden bench","fountain","rose border","garden lanterns","pond","relaxing corner","central display"
    )
    private val actions = listOf(
        "Restore","Rebuild","Plant","Repair","Clean and restore","Renew","Install","Revive","Create","Complete"
    )

    /** 25 original areas × 10 restoration beats = a distinct story/build task for every campaign level. */
    val steps: List<GardenStoryStep> = (1..CAMPAIGN_LEVELS).map { level ->
        val areaIndex = (level - 1) / 10
        val beat = (level - 1) % 10
        val area = areas[areaIndex]
        val feature = features[beat]
        val action = actions[beat]
        val speaker = if (level % 2 == 0) "Mia" else "Alex"
        val chapter = areaIndex + 1
        val before = when (beat) {
            0 -> "$area has been closed for years. Let's open this part of the estate carefully."
            1 -> "The old route through $area is damaged and almost lost under the growth."
            2 -> "$area needs color and life before it can feel welcoming again."
            3 -> "There is nowhere comfortable to stop and enjoy $area yet."
            4 -> "We found an old water feature here. It can become the focus of $area again."
            5 -> "The restored structure needs softer planting around it."
            6 -> "$area disappears into darkness after sunset."
            7 -> "This neglected corner can become a calm water-and-plant feature."
            8 -> "We have room for one special place where visitors can slow down."
            else -> "One final restoration will bring Chapter $chapter together as a complete garden."
        }
        val task = "$action the $feature in $area"
        val after = when (beat) {
            0 -> "$area is open again. The next restoration can finally begin."
            1 -> "The route is clear and the whole area feels connected again."
            2 -> "Fresh planting has brought color back to $area."
            3 -> "Now there is a proper place to stop and enjoy the view."
            4 -> "Water and stone give $area a strong new centerpiece."
            5 -> "The planting ties the restored features together beautifully."
            6 -> "Warm light now carries through $area after sunset."
            7 -> "This corner feels peaceful and alive again."
            8 -> "The new retreat gives $area a character of its own."
            else -> "Chapter $chapter is restored. $area now feels like a finished part of the estate."
        }
        // Restoration is part of campaign progression, so one successful level must always fund its
        // own mandatory build task. Costs rise more slowly than level rewards and chapter finales
        // remain slightly more expensive without ever turning progression into a paywall.
        val cost = 35 + (level - 1) * 4 + if (beat == 9) 20 else 0
        GardenStoryStep(level,speaker,before,task,after,cost,area)
    }

    fun get(level: Int): GardenStoryStep = steps[(level - 1).coerceIn(0, steps.lastIndex)]
}
