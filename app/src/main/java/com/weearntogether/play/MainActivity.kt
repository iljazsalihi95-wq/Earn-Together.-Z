package com.weearntogether.play

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.random.Random

class MainActivity : Activity() {
    private val navy = Color.rgb(10, 16, 39)
    private val purple = Color.rgb(101, 71, 224)
    private val gold = Color.rgb(255, 197, 77)
    private lateinit var root: LinearLayout
    private var round = 0
    private var correct = 0
    private val questions = listOf(
        Triple("Sa ditë ka një javë?", listOf("5", "7", "9", "10"), 1),
        Triple("Sa është 6 × 7?", listOf("36", "42", "48", "56"), 1),
        Triple("Cili planet quhet Planeti i Kuq?", listOf("Venusi", "Marsi", "Jupiteri", "Saturni"), 1),
        Triple("Cili është oqeani më i madh?", listOf("Atlantiku", "Indian", "Paqësori", "Arktiku"), 2),
        Triple("Sa brinjë ka trekëndëshi?", listOf("3", "4", "5", "6"), 0),
        Triple("Cila është kryeqyteti i Francës?", listOf("Roma", "Parisi", "Berlini", "Vjena"), 1),
        Triple("Sa minuta ka një orë?", listOf("30", "45", "60", "90"), 2),
        Triple("Cili është simboli kimik i ujit?", listOf("CO2", "O2", "H2O", "NaCl"), 2),
        Triple("Sa është 12 + 15?", listOf("25", "26", "27", "28"), 2),
        Triple("Cila gjuhë flitet kryesisht në Brazil?", listOf("Spanjisht", "Portugalisht", "Italisht", "Frëngjisht"), 1)
    )
    private val profile by lazy { getSharedPreferences("wet_guest_profile", MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (profile.contains("guest_id")) home() else welcome()
    }
    private fun welcome() {
        layout("Mirë se vjen!")
        text("Lojëra, sfida dhe progres në një vend.", 21, gold)
        text("Fillo pa regjistrim. Profili provizor ruhet vetëm në këtë pajisje dhe nuk është llogari online.")
        text("Emri në lojë (opsional)", 16)
        val nickname = EditText(this).apply {
            hint = "P.sh. Player82"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            filters = arrayOf(android.text.InputFilter.LengthFilter(24))
        }
        root.addView(nickname)
        button("Vazhdo si mysafir") {
            val displayName = nickname.text.toString().trim().ifBlank { "Lojtar" }
            profile.edit().putString("display_name", displayName).apply()
            profile.edit().putString("guest_id", java.util.UUID.randomUUID().toString())
                .putLong("created_at", System.currentTimeMillis()).apply()
            home()
        }
        text("Regjistrimi me email dhe rikthimi i llogarisë në pajisje tjetër do të aktivizohen vetëm pasi të lidhet shërbimi i sigurt i identifikimit.", 15)
    }
    private fun account() {
        layout("Profili im")
        val id = profile.getString("guest_id", "") ?: ""
        text("Llogari provizore në këtë telefon", 19, gold)
        text("Mirë se erdhe, " + profile.getString("display_name", "Lojtar"), 22, gold)
        text("ID lokale: " + id.take(8).uppercase())
        val editName = EditText(this).apply {
            setText(profile.getString("display_name", "Lojtar"))
            setTextColor(Color.WHITE)
            setSingleLine(true)
            filters = arrayOf(android.text.InputFilter.LengthFilter(24))
        }
        root.addView(editName)
        button("Ruaj emrin") {
            val updated = editName.text.toString().trim()
            if (updated.isNotEmpty()) {
                profile.edit().putString("display_name", updated).apply()
                account()
            } else Toast.makeText(this, "Shkruaj emrin", Toast.LENGTH_SHORT).show()
        }
        text("Progresi i lojërave ruhet në këtë pajisje. Nuk ka ende sinkronizim online, bilanc apo pagesa.")
        button("Kthehu te lojërat") { home() }
        button("Dil nga profili provizor") {
            android.app.AlertDialog.Builder(this)
                .setTitle("Dil nga profili?")
                .setMessage("Do të hiqet identifikuesi lokal. Progresi i lojërave nuk fshihet.")
                .setNegativeButton("Anulo", null)
                .setPositiveButton("Dil") { _, _ -> profile.edit().clear().apply(); welcome() }
                .show()
        }
    }
    private fun layout(title: String) {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 50, 32, 24); setBackgroundColor(navy) }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(root) }
        setContentView(scroll)
        text("✦ WE EARN TOGETHER", 16, gold)
        text(title, 29, Color.WHITE)
    }
    private fun text(s: String, size: Int = 17, color: Int = Color.WHITE) {
        root.addView(TextView(this).apply { text = s; textSize = size.toFloat(); setTextColor(color); setPadding(4, 16, 4, 16); if(size >= 25) setTypeface(null, Typeface.BOLD) })
    }
    private fun button(label: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = label; textSize = 17f; isAllCaps = false; setTextColor(Color.WHITE)
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(purple, Color.rgb(39, 117, 225))).apply { cornerRadius = 32f }
            setOnClickListener { action() }
        }
        root.addView(b, LinearLayout.LayoutParams(-1, 124).apply { bottomMargin = 18 })
    }
    private fun home() {
        layout("Play. Learn. Earn Together.")
        text("🎮 Lojëra • Sfida • Kuiz", 21)
        button("Profili • " + profile.getString("display_name", "Lojtar")) { account() }
        text("Version privat testues. Nuk ka reklama ose fitime reale.")
        button("▶ Luaj kuizin – 10 pyetje") { round = 0; correct = 0; question() }
        button("🫧 Bubble Shooter – qëllo flluskat") { bubbleShooter() }
        button("🧩 Block Puzzle – vendos blloqet") { blockPuzzle() }
        button("🔢 2048 – bashko numrat") { merge2048() }
        button("🧠 Memory – gjej çiftet") { memory() }
        button("🔢 Numrat – sfida matematikore") { mathGame() }
        button("📺 Unity Ads – vetëm test") { adsTest() }


    }
    private fun question() {
        if (round >= questions.size) { result(); return }
        layout("Kuiz • Pyetja ${round + 1}/10")
        text("Pikë: $correct", 17, gold)
        val q = questions[round]
        text(q.first, 23)
        q.second.forEachIndexed { index, answer ->
            button(answer) {
                if (index == q.third) correct++
                round++
                question()
            }
        }
    }
    private fun result() {
        layout("Raundi përfundoi!")
        text("Përgjigje të sakta: $correct / 10", 23, gold)
        text("Këtu mund të vendoset një reklamë vetëm pas miratimit të modelit monetar nga partneri. Aktualisht nuk shfaqet reklamë.")
        button("Luaj përsëri") { round = 0; correct = 0; question() }
        button("Ballina") { home() }
    }

    private fun bubbleShooter() {
        layout("Bubble Shooter")
        text("Prek ekranin për të drejtuar dhe qëlluar.", 16, gold)
        root.addView(BubbleShooterView(this), LinearLayout.LayoutParams(-1, 1250))
        button("Rifillo lojën") { bubbleShooter() }
        button("Ballina") { home() }
    }
    private fun merge2048() {
        layout("2048 • Bashko numrat")
        text("Bashko dy numra të njëjtë duke rrëshqitur tabelën.",16,gold)
        val game = Merge2048View(this)
        root.addView(game, LinearLayout.LayoutParams(-1, 1050))
        button("Rifillo 2048") { game.restart() }
        button("Ballina") { home() }
    }
    private fun blockPuzzle() {
        layout("Block Puzzle")
        text("Prek një formë, pastaj katrorin ku dëshiron ta vendosësh.", 16, gold)
        val game = BlockPuzzleView(this)
        root.addView(game, LinearLayout.LayoutParams(-1, 1250))
        button("Rifillo lojën") { game.restart() }
        button("Ballina") { home() }
    }
    private var memoryCards = mutableListOf<String>()
    private var memoryOpened = mutableListOf<Int>()
    private var memoryMatched = mutableSetOf<Int>()
    private var memoryMoves = 0
    private fun memory() {
        memoryCards = (listOf("★","♥","◆","●","☀","♣") + listOf("★","♥","◆","●","☀","♣")).shuffled().toMutableList()
        memoryOpened.clear(); memoryMatched.clear(); memoryMoves = 0
        renderMemory()
    }
    private fun renderMemory() {
        layout("Memory • Gjej çiftet")
        text("Lëvizje: $memoryMoves • Çifte: ${memoryMatched.size / 2}/6", 18, gold)
        if (memoryMatched.size == memoryCards.size) {
            text("Urime! I gjete të gjitha çiftet.", 24, gold)
            button("Luaj përsëri") { memory() }
            button("Ballina") { home() }
            return
        }
        for (rowIndex in 0 until 3) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            root.addView(row)
            for (col in 0 until 4) {
                val index = rowIndex * 4 + col
                val revealed = index in memoryOpened || index in memoryMatched
                val card = Button(this).apply {
                    text = if (revealed) memoryCards[index] else "?"
                    textSize = 26f
                    setTextColor(Color.WHITE)
                    isAllCaps = false
                    setBackgroundColor(if (revealed) purple else Color.rgb(28, 49, 95))
                    isEnabled = index !in memoryMatched
                    setOnClickListener {
                        if (index in memoryOpened || memoryOpened.size >= 2) return@setOnClickListener
                        memoryOpened.add(index)
                        if (memoryOpened.size == 2) {
                            memoryMoves++
                            val a = memoryOpened[0]; val b = memoryOpened[1]
                            if (memoryCards[a] == memoryCards[b]) {
                                memoryMatched.add(a); memoryMatched.add(b)
                                memoryOpened.clear()
                                renderMemory()
                            } else {
                                renderMemory()
                                android.os.Handler(mainLooper).postDelayed({
                                    memoryOpened.clear()
                                    renderMemory()
                                }, 850)
                            }
                        } else renderMemory()
                    }
                }
                row.addView(card, LinearLayout.LayoutParams(0, 125, 1f).apply { setMargins(3, 3, 3, 3) })
            }
        }
        button("Ballina") { home() }
    }
    private var mathRound = 0
    private var mathScore = 0
    private fun mathGame() {
        mathRound = 0; mathScore = 0
        nextMath()
    }
    private fun nextMath() {
        if (mathRound == 10) {
            layout("Sfida përfundoi!")
            text("Rezultati: $mathScore / 10", 26, gold)
            button("Luaj përsëri") { mathGame() }
            button("Ballina") { home() }
            return
        }
        val a = Random.nextInt(2, 21)
        val b = Random.nextInt(2, 13)
        val answer = a * b
        val choices = (listOf(answer) + generateSequence { answer + Random.nextInt(-20, 21) }
            .filter { it > 0 && it != answer }.distinct().take(3).toList()).shuffled()
        layout("Numrat • ${mathRound + 1}/10")
        text("Sa është $a × $b ?", 27, gold)
        text("Pikë: $mathScore")
        choices.forEach { value ->
            button(value.toString()) {
                if (value == answer) mathScore++
                mathRound++
                nextMath()
            }
        }
    }
    private fun adsTest() {
        layout("Unity Ads • Test privat")
        text("Reklamat janë vetëm për testim. Nuk krijojnë fitime apo pagesa reale.", 17, gold)
        text("SDK mund të komunikojë me Unity. Përpara aktivizimit kërkohet pëlqimi yt dhe një zgjidhje e plotë privatësie për publikim.")
        val status = TextView(this).apply { text = "SDK joaktiv"; textSize = 16f; setTextColor(gold) }
        root.addView(status)
        val manager = UnityAdsTestManager(this) { status.text = it }
        button("Pranoj testimin dhe aktivizo Unity Ads") { manager.initializeAfterConsent() }
        button("Ngarko Rewarded TEST") { manager.loadRewarded() }
        button("Shfaq Rewarded TEST") { manager.showRewarded() }
        button("Ngarko Interstitial TEST") { manager.loadInterstitial() }
        button("Shfaq Interstitial TEST") { manager.showInterstitial() }
        button("Ballina") { home() }
    }
    private fun demo() {
        layout("Simulim 50/50")
        text("Shembull matematikor, JO fitime reale.", 19, gold)
        text("Nëse një reklamë e lejuar gjeneron 0,10 € të ardhura të konfirmuara:")
        text("Lojtari: 0,05 €", 25, gold)
        text("Platforma: 0,05 €", 25)
        text("Bilanci real: 0,00 €. Nuk ka lidhje PayPal ose reklama.")
        button("Kthehu") { home() }
    }
}
