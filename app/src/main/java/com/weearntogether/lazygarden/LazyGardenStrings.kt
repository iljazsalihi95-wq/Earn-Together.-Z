package com.weearntogether.lazygarden

import android.content.Context
import java.util.Locale

/** Runtime copy for the game HUD. Android's current locale is the source of truth. */
class LazyGardenStrings(context: Context) {
    private val language = context.resources.configuration.locales[0]?.language ?: Locale.getDefault().language
    private val de = language == "de"
    private val it = language == "it"

    fun level(n: Int, total: Int) = when { de -> "Level $n/$total"; it -> "Livello $n/$total"; else -> "Level $n/$total" }
    fun moves(n: Int) = when { de -> "Züge: $n"; it -> "Mosse: $n"; else -> "Moves: $n" }
    fun coins(n: Int) = when { de -> "Münzen: $n"; it -> "Monete: $n"; else -> "Coins: $n" }
    fun goals(parts: List<String>) = when { de -> parts.joinToString(prefix="Ziele • ", separator=" • "); it -> parts.joinToString(prefix="Obiettivi • ", separator=" • "); else -> parts.joinToString(prefix="Goals • ", separator=" • ") }
    fun shuffle(count: Int) = "Shuffle ×$count"
    fun booster(count: Int) = when { count > 0 && de -> "Booster • verbraucht keinen Zug"; count > 0 && it -> "Booster • non consuma mosse"; count > 0 -> "Booster • no move consumed"; de -> "Alle 5 Level erhältst du 1 Shuffle"; it -> "Ottieni 1 Shuffle ogni 5 livelli"; else -> "Earn 1 Shuffle every 5 levels" }
    fun retry(n: Int) = when { de -> "Level $n erneut spielen"; it -> "Riprova livello $n"; else -> "Retry level $n" }
    fun finishToBuild() = when { de -> "Schließe das Level ab, um zu bauen"; it -> "Completa il livello per costruire"; else -> "Finish the level to unlock building" }
    fun building() = when { de -> "Wird restauriert…"; it -> "Restauro in corso…"; else -> "Restoring…" }
    fun build(cost: Int) = when { de -> "Bauen • $cost Münzen"; it -> "Costruisci • $cost monete"; else -> "Build • $cost coins" }
    fun continueStory(n: Int) = when { de -> "Weiter • Level $n"; it -> "Continua • Livello $n"; else -> "Continue • Level $n" }
    fun chapterDone() = when { de -> "Kapitel abgeschlossen"; it -> "Capitolo completato"; else -> "Chapter complete" }
    fun buildTask(task: String, cost: Int) = when { de -> "Bauauftrag: $task • $cost Münzen"; it -> "Restauro: $task • $cost monete"; else -> "Build task: $task • $cost coins" }
    fun restored(task: String) = when { de -> "✓ Restauriert: $task"; it -> "✓ Restaurato: $task"; else -> "✓ Restored: $task" }
    fun restoring(task: String) = when { de -> "Restaurierung: $task"; it -> "Restauro: $task"; else -> "Restoring: $task" }
    fun workingDialogue() = when { de -> "Wir arbeiten daran… gleich ist es fertig."; it -> "Ci stiamo lavorando… manca poco."; else -> "We're working on it… almost ready." }

    fun tile(t: GardenTile) = when(t) {
        GardenTile.LEAF -> when { de -> "Blätter"; it -> "Foglie"; else -> "Leaves" }
        GardenTile.FLOWER -> when { de -> "Blumen"; it -> "Fiori"; else -> "Flowers" }
        GardenTile.ROSE -> when { de -> "Rosen"; it -> "Rose"; else -> "Roses" }
        GardenTile.BERRY -> when { de -> "Beeren"; it -> "Bacche"; else -> "Berries" }
        GardenTile.PEAR -> when { de -> "Birnen"; it -> "Pere"; else -> "Pears" }
        GardenTile.WATER -> when { de -> "Wasser"; it -> "Acqua"; else -> "Water" }
    }
    fun obstacle(o: GardenObstacle) = when(o) {
        GardenObstacle.CRATE -> when { de -> "Kisten"; it -> "Casse"; else -> "Crates" }
        GardenObstacle.IVY -> when { de -> "Efeu"; it -> "Edera"; else -> "Ivy" }
        GardenObstacle.STONE -> when { de -> "Steine"; it -> "Pietre"; else -> "Stones" }
    }
}
