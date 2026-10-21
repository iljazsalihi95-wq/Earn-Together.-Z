package com.weearntogether.lazygarden

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.weearntogether.play.LevelPlayManager
import com.weearntogether.play.R

class LazyGardenActivity : Activity() {
    private val collected = mutableMapOf<GardenTile, Int>()
    private lateinit var board: LazyGardenBoardView
    private lateinit var scene: LazyGardenSceneView
    private lateinit var wallet: LazyGardenWallet
    private lateinit var progressStore: LazyGardenProgressStore
    private var currentLevel = 1
    private var unlockedLevel = 1
    private var levelFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_lazy_garden)
        wallet = LazyGardenWallet(this); progressStore = LazyGardenProgressStore(this)
        val saved = progressStore.load(); currentLevel = saved.level; unlockedLevel = saved.unlockedLevel
        scene = LazyGardenSceneView(this); findViewById<FrameLayout>(R.id.lgSceneHost).addView(scene, FrameLayout.LayoutParams(-1,-1))
        board = LazyGardenBoardView(this); findViewById<FrameLayout>(R.id.lgBoardHost).addView(board, FrameLayout.LayoutParams(-1,-1))
        startLevel(currentLevel, saved.collected)
        board.onMove = { result -> result.tiles.forEach { (tile,count) -> collected[tile]=(collected[tile]?:0)+count }; saveProgress(); if(goalsComplete()) finishLevel(true) else if(board.engine.movesLeft<=0) finishLevel(false) else updateHud() }
        findViewById<Button>(R.id.lgAction).setOnClickListener { handleAction() }; updateHud()
    }

    private fun startLevel(number:Int, restored:Map<GardenTile,Int> = emptyMap()){ currentLevel=number.coerceIn(1,LazyGardenLevels.count); collected.clear(); GardenTile.entries.forEach{collected[it]=restored[it]?:0}; board.loadLevel(LazyGardenLevels.get(currentLevel)); levelFinished=false; updateHud() }
    private fun goalsComplete()=LazyGardenLevels.get(currentLevel).goals.all{(collected[it.tile]?:0)>=it.target}
    private fun finishLevel(won:Boolean){ levelFinished=true; board.setInputEnabled(false); if(won){val l=LazyGardenLevels.get(currentLevel);if(progressStore.markRewardClaimed(currentLevel))wallet.credit(l.coinReward);unlockedLevel=maxOf(unlockedLevel,(currentLevel+1).coerceAtMost(LazyGardenLevels.count));saveProgress();LevelPlayManager.showLevelCompleteVideo()};updateHud() }
    private fun handleAction(){if(!levelFinished)return;if(!goalsComplete()){startLevel(currentLevel);saveProgress();return};val s=LazyGardenStory.get(currentLevel);if(!progressStore.isBuildTaskCompleted(currentLevel)){if(wallet.spend(s.cost)){progressStore.markBuildTaskCompleted(currentLevel);updateHud()};return};if(currentLevel<LazyGardenLevels.count){startLevel(currentLevel+1);saveProgress()}}
    private fun saveProgress()=progressStore.save(LazyGardenProgress(currentLevel,unlockedLevel,board.engine.movesLeft,collected.toMap()))
    private fun tileName(t:GardenTile)=mapOf(GardenTile.LEAF to "Gjethe",GardenTile.FLOWER to "Lule",GardenTile.ROSE to "Trëndafil",GardenTile.BERRY to "Manaferra",GardenTile.PEAR to "Dardha",GardenTile.WATER to "Ujë")[t]!!

    private val sqBefore=listOf("Ky kopsht është lënë pas dore prej vitesh. Ta fillojmë nga hyrja.","Shtegu i vjetër është çarë dhe është mbuluar nga bari.","Kjo pjesë dikur ishte plot me ngjyra. Le t'i kthejmë lulet.","Stoli është dëmtuar. Mund ta bëjmë përsëri një vend të bukur pushimi.","Nën gjithë këtë papastërti fshihet një shatërvan. Mund ta shpëtojmë.","Shatërvanit i duhen lule përreth. Trëndafilat do të duken bukur.","Në mbrëmje këtu është shumë errët. Duhet ndriçim.","Pellgu është mbushur me baltë. Ta pastrojmë dhe ta gjallërojmë.","Na duhet një kënd i qetë vetëm për pushim.","Edhe një punë e fundit. Ta bashkojmë gjithë kopshtin në një pamje të vetme.")
    private val sqAfter=listOf("Shumë më mirë! Hyrja e kopshtit është hapur përsëri.","Tani shtegu është i sigurt dhe i bukur.","Shiko sa ngjyra! Kopshti po merr jetë.","Tani kemi ku të ulemi dhe ta shijojmë kopshtin.","Uji po rrjedh përsëri. Oborri ndryshoi krejt.","Trëndafilat e bënë shatërvanin zemrën e kopshtit.","Tani kopshti do të jetë i ngrohtë dhe i bukur edhe natën.","Ujë i pastër dhe bimë të reja. Ky kënd është bërë shumë i qetë.","Ky është vendi ideal për një pasdite të qetë.","E përfunduam kapitullin e parë të kopshtit. Pas portës na presin zona të tjera.")
    private val sqTasks=listOf("Rregullo portën e kopshtit","Rindërto shtegun prej guri","Mbill shtratin e parë me lule","Rregullo stolin prej druri","Pastro dhe restauro shatërvanin","Mbill trëndafila rreth shatërvanit","Vendos ndriçimin e kopshtit","Restauro pellgun dhe bimët ujore","Ndërto këndin e pushimit","Përfundo qendrën e kopshtit")

    private fun updateHud(){val level=LazyGardenLevels.get(currentLevel);val story=LazyGardenStory.get(currentLevel);val done=progressStore.isBuildTaskCompleted(currentLevel);scene.setCompletedTasks((1..LazyGardenLevels.count).filter{progressStore.isBuildTaskCompleted(it)}.toSet());findViewById<TextView>(R.id.lgLevel).text="Niveli $currentLevel";findViewById<TextView>(R.id.lgMoves).text="Lëvizje: ${board.engine.movesLeft}";findViewById<TextView>(R.id.lgCoins).text="Monedha: ${wallet.coins}";findViewById<TextView>(R.id.lgSpeaker).text=story.speaker;findViewById<ImageView>(R.id.lgCharacter).setImageResource(if(story.speaker=="Alex")R.drawable.lg_alex else R.drawable.lg_mia);findViewById<TextView>(R.id.lgDialogue).text=if(done)sqAfter[currentLevel-1] else sqBefore[currentLevel-1];findViewById<TextView>(R.id.lgGoals).text=level.goals.joinToString(prefix="Detyra • ",separator=" • "){"${tileName(it.tile)} ${collected[it.tile]?:0}/${it.target}"};findViewById<TextView>(R.id.lgBuildTask).text=if(done)"✓ U restaurua: ${sqTasks[currentLevel-1]}" else "Ndërtim: ${sqTasks[currentLevel-1]} • ${story.cost} monedha";val a=findViewById<Button>(R.id.lgAction);when{!levelFinished->{a.isEnabled=false;a.text="Përfundo nivelin për të hapur ndërtimin"};!goalsComplete()->{a.isEnabled=true;a.text="Provo përsëri nivelin $currentLevel"};!done->{a.isEnabled=wallet.coins>=story.cost;a.text="Ndërto • ${story.cost} monedha"};currentLevel<LazyGardenLevels.count->{a.isEnabled=true;a.text="Vazhdo historinë • Niveli ${currentLevel+1}"};else->{a.isEnabled=false;a.text="Kapitulli i parë u përfundua"}}}
}
