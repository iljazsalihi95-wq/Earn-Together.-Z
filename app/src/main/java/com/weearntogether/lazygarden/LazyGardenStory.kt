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
        GardenStoryStep(1,"Alex","This place has been forgotten for years. Let's start with the entrance.","Clear the entrance and repair the garden gate","Much better. Now the garden finally feels open again.",120,"Garden gate"),
        GardenStoryStep(2,"Mia","The old path is cracked and almost hidden under the weeds.","Restore the stone path to the house","The path is safe again. We can finally reach the house properly.",150,"Stone path"),
        GardenStoryStep(3,"Alex","This empty patch used to be full of color.","Plant the first flower bed","Look at that color! The garden is starting to come alive.",180,"Flower bed"),
        GardenStoryStep(4,"Mia","That bench has seen better days.","Repair the wooden garden bench","A quiet place to sit and enjoy what we've restored.",220,"Garden bench"),
        GardenStoryStep(5,"Alex","There's a fountain under all that dirt.","Clean and restore the fountain","The water is flowing again. This changes the whole courtyard.",260,"Fountain"),
        GardenStoryStep(6,"Mia","The fountain needs something softer around it.","Plant roses around the fountain","Perfect. The roses make the fountain feel like the heart of the garden.",300,"Rose garden"),
        GardenStoryStep(7,"Alex","It gets too dark here in the evening.","Install new garden lanterns","Now the paths will stay warm and welcoming after sunset.",350,"Garden lanterns"),
        GardenStoryStep(8,"Mia","The little pond is still muddy.","Restore the pond and add water plants","Clear water and fresh plants make this corner peaceful again.",400,"Garden pond"),
        GardenStoryStep(9,"Alex","We need one place made just for relaxing.","Build a relaxing garden corner","This is exactly where I'd spend a quiet afternoon.",450,"Relaxing corner"),
        GardenStoryStep(10,"Mia","Let's bring everything together in the center.","Complete the central garden makeover","The first garden area is restored.",500,"Central garden"),
        GardenStoryStep(11,"Alex","The side garden is still hidden behind old hedges.","Trim the hedges and open the side garden","A whole new part of the estate is visible now.",540,"Side garden"),
        GardenStoryStep(12,"Mia","This arch could become a beautiful entrance.","Restore the flower arch","The arch gives this path a real sense of arrival.",580,"Flower arch"),
        GardenStoryStep(13,"Alex","The old tea terrace needs a fresh start.","Restore the tea terrace","Now we have a bright place for morning tea.",620,"Tea terrace"),
        GardenStoryStep(14,"Mia","These planters are empty and cracked.","Renew the terrace planters","The terrace finally has color again.",660,"Terrace planters"),
        GardenStoryStep(15,"Alex","The pergola frame is still strong underneath.","Repair and decorate the pergola","The pergola makes this corner feel complete.",700,"Pergola"),
        GardenStoryStep(16,"Mia","There's an old orchard beyond the pergola.","Clear and revive the orchard","The orchard is breathing again.",750,"Orchard"),
        GardenStoryStep(17,"Alex","The orchard path disappears into weeds.","Build the orchard walkway","Now every tree is easy to reach.",800,"Orchard walkway"),
        GardenStoryStep(18,"Mia","A small bridge used to cross this stream.","Rebuild the wooden bridge","The bridge reconnects both sides of the garden.",850,"Wooden bridge"),
        GardenStoryStep(19,"Alex","This quiet corner would be perfect for birds.","Create a bird garden","The garden sounds alive now.",900,"Bird garden"),
        GardenStoryStep(20,"Mia","The old greenhouse is our biggest job yet.","Restore the greenhouse exterior","The greenhouse is standing proud again.",960,"Greenhouse"),
        GardenStoryStep(21,"Alex","The greenhouse is empty inside.","Prepare the greenhouse beds","Everything is ready for new plants.",1020,"Greenhouse beds"),
        GardenStoryStep(22,"Mia","Let's turn it into a real collection.","Plant the greenhouse collection","This feels like a tiny botanical garden.",1080,"Plant collection"),
        GardenStoryStep(23,"Alex","The upper lawn still feels unfinished.","Build the upper lawn seating area","We finally have a place to enjoy the whole view.",1140,"Upper lawn"),
        GardenStoryStep(24,"Mia","One dark corner remains near the greenhouse.","Add decorative lights and flowers","The whole garden now glows after sunset.",1200,"Evening garden"),
        GardenStoryStep(25,"Alex","We've restored every area we opened so far.","Complete the grand garden vista","What a transformation. The next chapter can begin beyond the estate wall.",1300,"Grand vista"),
        GardenStoryStep(26,"Mia","Beyond the wall is an old courtyard no one has used in years.","Open and clean the hidden courtyard","The hidden courtyard finally belongs to the garden again.",1360,"Hidden courtyard"),
        GardenStoryStep(27,"Alex","These stone stairs are too damaged to use safely.","Rebuild the courtyard stairs","Now the upper terrace is connected again.",1420,"Courtyard stairs"),
        GardenStoryStep(28,"Mia","The terrace needs shade before anyone can enjoy it.","Restore the canvas pavilion","This is going to be perfect on warm afternoons.",1480,"Garden pavilion"),
        GardenStoryStep(29,"Alex","An empty pavilion needs a reason to stay awhile.","Create the pavilion lounge","Now this feels like a real outdoor room.",1540,"Pavilion lounge"),
        GardenStoryStep(30,"Mia","I found an old plan showing a herb garden here.","Plant the heritage herb garden","The scents make this corner feel alive.",1620,"Herb garden"),
        GardenStoryStep(31,"Alex","The service path behind the herbs is almost gone.","Restore the brick service path","We can move through the whole estate again.",1700,"Brick path"),
        GardenStoryStep(32,"Mia","This wall was once covered in climbing flowers.","Build a flowering trellis wall","The bare wall has become a feature instead of an eyesore.",1780,"Trellis wall"),
        GardenStoryStep(33,"Alex","There's room here for a small place to gather.","Create the garden dining corner","Dinner outside just became an option.",1860,"Dining corner"),
        GardenStoryStep(34,"Mia","The old sundial is still here, just buried under ivy.","Restore the stone sundial","The sundial gives the courtyard its character back.",1940,"Stone sundial"),
        GardenStoryStep(35,"Alex","This long border could tie the whole courtyard together.","Plant the grand flower border","Color now runs from one end of the courtyard to the other.",2040,"Flower border"),
        GardenStoryStep(36,"Mia","We can hear water behind those overgrown shrubs.","Clear the stream overlook","The stream view was worth uncovering.",2140,"Stream overlook"),
        GardenStoryStep(37,"Alex","The overlook needs a safe edge and somewhere to stop.","Build the overlook railing and bench","Now everyone can enjoy the stream safely.",2240,"Overlook bench"),
        GardenStoryStep(38,"Mia","The old steps down to the water have disappeared.","Restore the riverside steps","The lower garden is open again.",2340,"Riverside steps"),
        GardenStoryStep(39,"Alex","One final dark stretch separates the courtyard from the stream.","Install the riverside lantern walk","The route now shines all the way to the water.",2440,"Lantern walk"),
        GardenStoryStep(40,"Mia","Look how far we've come. Let's finish this chapter with a real landmark.","Complete the courtyard and stream panorama","The estate has a new heart. Another chapter is ready to begin.",2580,"Courtyard panorama")
    )

    fun get(level: Int): GardenStoryStep = steps[(level - 1).coerceIn(0, steps.lastIndex)]
}
