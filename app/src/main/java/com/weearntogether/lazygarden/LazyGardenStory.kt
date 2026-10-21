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
    val steps = listOf(
        GardenStoryStep(1, "Alex", "This place has been forgotten for years. Let's start with the entrance.", "Clear the entrance and repair the garden gate", "Much better. Now the garden finally feels open again.", 120, "Garden gate"),
        GardenStoryStep(2, "Mia", "The old path is cracked and almost hidden under the weeds.", "Restore the stone path to the house", "The path is safe again. We can finally reach the house properly.", 150, "Stone path"),
        GardenStoryStep(3, "Alex", "This empty patch used to be full of color.", "Plant the first flower bed", "Look at that color! The garden is starting to come alive.", 180, "Flower bed"),
        GardenStoryStep(4, "Mia", "That bench has seen better days. It could become a perfect resting spot.", "Repair the wooden garden bench", "A quiet place to sit and enjoy what we've restored.", 220, "Garden bench"),
        GardenStoryStep(5, "Alex", "There's a fountain under all that dirt. I think we can save it.", "Clean and restore the fountain", "The water is flowing again. This changes the whole courtyard.", 260, "Fountain"),
        GardenStoryStep(6, "Mia", "The fountain needs something softer around it.", "Plant roses around the fountain", "Perfect. The roses make the fountain feel like the heart of the garden.", 300, "Rose garden"),
        GardenStoryStep(7, "Alex", "It gets too dark here in the evening.", "Install new garden lanterns", "Now the paths will stay warm and welcoming after sunset.", 350, "Garden lanterns"),
        GardenStoryStep(8, "Mia", "The little pond is still muddy, but I can already imagine it restored.", "Restore the pond and add water plants", "Clear water, fresh plants—this corner feels peaceful again.", 400, "Garden pond"),
        GardenStoryStep(9, "Alex", "We need one place made just for relaxing.", "Build a relaxing garden corner", "This is exactly where I'd spend a quiet afternoon.", 450, "Relaxing corner"),
        GardenStoryStep(10, "Mia", "One last job. Let's bring everything together in the center.", "Complete the central garden makeover", "We did it. The first garden chapter is restored—and there's much more beyond the gate.", 500, "Central garden")
    )

    fun get(level: Int): GardenStoryStep = steps[(level - 1).coerceIn(0, steps.lastIndex)]
}
