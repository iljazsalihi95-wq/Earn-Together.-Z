package com.weearntogether.lazygarden

data class GardenStoryStep(
    val level: Int,
    val speaker: String,
    val before: String,
    val buildTask: String,
    val after: String,
    val cost: Int,
    val sceneLabel: String,
    val chapter: Int,
    val beat: Int,
    val chapterProgressPercent: Int
)

object LazyGardenStory {
    private const val CAMPAIGN_LEVELS = 250
    private val areas = listOf(
        "Entrance Garden","Fountain Court","Rose Terrace","Orchard Walk","Greenhouse","Hidden Courtyard","Riverside","Moon Garden","Old Estate","Lakeside",
        "Butterfly Garden","Herb Court","Woodland Path","Sun Terrace","Kitchen Garden","Water Garden","Hill Garden","Secret Grove","Winter Garden","Grand Promenade",
        "Magnolia Court","Library Garden","Cedar Walk","Festival Lawn","Grand Vista"
    )
    private val signatureFeatures = listOf(
        "iron entrance gate","mosaic fountain","climbing rose arch","fruit-tree avenue","glasshouse roof","ivy-covered pergola","river lookout","moonlit reflecting pool","estate staircase","lakeside pavilion",
        "butterfly meadow","herb planters","woodland footbridge","sun terrace canopy","kitchen-garden beds","water-lily pond","hilltop gazebo","secret stone doorway","winter conservatory","promenade balustrade",
        "magnolia circle","library reading arbor","cedar boardwalk","festival bandstand","grand vista terrace"
    )
    private val supportingFeatures = listOf(
        "stone path","flower borders","rest bench","water feature","accent planting","garden lanterns","pond edge","quiet seating nook"
    )

    /** 25 original areas × 10 restoration beats. Each chapter has its own signature restoration. */
    val steps: List<GardenStoryStep> = (1..CAMPAIGN_LEVELS).map { level ->
        val areaIndex = (level - 1) / 10
        val beat = (level - 1) % 10
        val area = areas[areaIndex]
        val chapter = areaIndex + 1
        val signature = signatureFeatures[areaIndex]
        val feature = when (beat) {
            0 -> "access to $area"
            1 -> supportingFeatures[0]
            2 -> supportingFeatures[1]
            3 -> supportingFeatures[2]
            4 -> signature
            5 -> supportingFeatures[4]
            6 -> supportingFeatures[5]
            7 -> supportingFeatures[6]
            8 -> supportingFeatures[7]
            else -> "$area finishing display"
        }
        val speaker = if (level % 2 == 0) "Mia" else "Alex"
        val before = when (beat) {
            0 -> "$area has been closed for years. First we need to make it safe and welcoming again."
            1 -> "The old route through $area is damaged and almost lost under the growth."
            2 -> "$area needs layers of color and planting before the space feels alive."
            3 -> "Visitors need a comfortable place to stop and enjoy this part of the estate."
            4 -> "We uncovered the original plans. The $signature can become the landmark of $area."
            5 -> "The main structure is back; now the planting needs to connect it naturally to the garden."
            6 -> "$area disappears into darkness after sunset. Warm lighting will change the atmosphere."
            7 -> "This neglected edge can become a calm transition between water, stone and planting."
            8 -> "One quiet corner is still missing. It should feel different from the busy central path."
            else -> "Everything is in place. One final restoration will complete Chapter $chapter."
        }
        val action = when (beat) {
            0 -> "Reopen"
            1 -> "Rebuild"
            2 -> "Plant"
            3 -> "Restore"
            4 -> "Reconstruct"
            5 -> "Landscape"
            6 -> "Install"
            7 -> "Revive"
            8 -> "Create"
            else -> "Complete"
        }
        val task = "$action the $feature"
        val after = when (beat) {
            0 -> "$area is open again. The restoration crew can finally move in."
            1 -> "The route is clear and the whole area feels connected again."
            2 -> "Fresh planting has brought depth and color back to $area."
            3 -> "There is finally a proper place to stop and enjoy the view."
            4 -> "The $signature gives $area a memorable identity of its own."
            5 -> "The new planting softens the restored structures and ties the scene together."
            6 -> "Warm light now carries through $area after sunset."
            7 -> "The restored edge feels peaceful, layered and alive again."
            8 -> "The new retreat gives visitors a quiet place away from the main route."
            else -> "Chapter $chapter is complete. $area now feels like a finished part of the estate."
        }
        // A successful level always funds its mandatory restoration beat; this is game progression,
        // never a real-money gate. Finale beats cost slightly more to make chapter completion meaningful.
        val cost = 35 + (level - 1) * 4 + if (beat == 9) 20 else 0
        GardenStoryStep(level, speaker, before, task, after, cost, area, chapter, beat + 1, (beat + 1) * 10)
    }

    fun get(level: Int): GardenStoryStep = steps[(level - 1).coerceIn(0, steps.lastIndex)]
    fun chapterProgress(level: Int): Int = get(level).chapterProgressPercent
    fun isChapterFinale(level: Int): Boolean = get(level).beat == 10
}
