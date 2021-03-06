package com.pungo.repairgame.gamescreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.pungo.repairgame.*
import com.pungo.repairgame.gamescreen.devices.BigMonitorStatus
import com.pungo.repairgame.gamescreen.devices.DevicesData
import com.pungo.repairgame.gamescreen.devices.SimpleDevice
import com.pungo.repairgame.ui.ButtonStatus

class GameScreen: Screen() {
    private lateinit var mainSprite: Sprite

    private lateinit var phText: TextIsland
    private lateinit var bigMonitor: BigMonitor
    private lateinit var redButton: RedButton
    private lateinit var cargoBay: CargoBay
    private var devices = listOf<SimpleDevice>()
    private var tools = listOf<SimpleTool>()

    private var texts = mutableListOf<TextIslandTexts>()
    private val travelTimer = Timer(20000)                  // travel timer
    private val timer = Timer(1000)                         // breakdown timer
    private val countdownTimer = Timer(100)                // countdown timer
    private var sfxBeep = Gdx.audio.newSound(Gdx.files.internal("sound/Beep.mp3"))
    private var sfxTake = Gdx.audio.newSound(Gdx.files.internal("sound/Take.mp3"))
    private var sfxFail = Gdx.audio.newSound(Gdx.files.internal("sound/Fail.mp3"))
    private var sfxChoose = Gdx.audio.newSound(Gdx.files.internal("sound/Choose.mp3"))
    private var sfxRed = Gdx.audio.newSound(Gdx.files.internal("sound/Red.mp3"))
    private var breakingList = listOf(0)
    private var chosenOption = -1
    private var patrolFlag = false
    private var patrolPlanet = false
    private var countdownIndex = -1
    private var countdownIndexLimit = 3



    fun reset(){
        chosenOption = -1
        patrolFlag = false
        patrolPlanet = false
        countdownIndex = -1
        cargoBay.reset()
        devices.forEach { it.status = DeviceStatus.NORMAL }
        tools = ToolsData.getTools()
        bigMonitor.status = BigMonitorStatus.P0
        //bigMonitor.changeMonitor("graphics/planets/p0.png")
        breakingList = listOf(0)
        phText.getPlanetPassage(1)
        updateIslandText()
    }

    fun pauseTimers(){
        sfxBeep.stop()
        sfxTake.stop()
        sfxFail.stop()
        travelTimer.pause()
        countdownTimer.pause()
        timer.pause()
        devices.forEach { it.breakTimer.pause() }

    }

    fun resumeTimers(){
        travelTimer.resume()
        countdownTimer.resume()
        timer.resume()
        devices.forEach { it.breakTimer.resume() }
    }

    override fun draw(batch: SpriteBatch) {
        mainSprite.draw(batch)
        bigMonitor.draw(batch)
        devices.forEach {
            it.draw(batch)
        }
        tools.forEach {
            it.draw(batch)
        }
        redButton.draw(batch)
        cargoBay.draw(batch)
        texts[0].draw(batch, true)
        if(chosenOption!=-1 && !texts[0].revealed && ! patrolPlanet){
            texts[4].draw(batch)
        }
        if (texts[0].revealed && phText.sceneNotOver()) {
            for (k in 1..3) {texts[k].draw(batch)}
        }
    }

    override fun firstPress() {
        tools.forEach {
            if (SharedVariables.contains(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), it.getSprite())) {
                it.flying = true
                if(!SharedVariables.sfxMuted){
                    sfxTake.play(SharedVariables.sfxVolume)
                }
            }
        }
        if(SharedVariables.contains(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), redButton.activeSprite)) {
            redButton.status = ButtonStatus.DOWN
        }
        for (k in 1..3) {
            if(texts[k].hovered) {
                texts[k].pressing = true
            }
        }
    }

    override fun pressing() {
        tools.forEach {
            if(it.flying){
                val flyingX = Gdx.input.x.toFloat()/Gdx.graphics.width*SharedVariables.mainWidth
                val flyingY = Gdx.input.y.toFloat()/Gdx.graphics.height*SharedVariables.mainHeight
                it.flyingCentre(flyingX,flyingY)
            }
        }
        if(redButton.status == ButtonStatus.DOWN && !SharedVariables.contains(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), redButton.activeSprite)) redButton.status = ButtonStatus.UP
    }

    override fun released() {
        tools.forEachIndexed { _, it ->
            if (it.flying) {
                it.flying = false
                if (!phText.sceneNotOver()) {
                    devices.forEach { it2 ->
                        if (SharedVariables.contains(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), it2.getSprite())) {
                            val fixed = it2.status==it.fixing
                            if (fixed){
                                if(!SharedVariables.sfxMuted) {
                                    it.sfx.play(SharedVariables.sfxVolume)
                                }
                                it2.breakTimer.running = false
                                it2.status = DeviceStatus.NORMAL
                            }
                            else{
                                if(!SharedVariables.sfxMuted){
                                    sfxFail.play(SharedVariables.sfxVolume)
                                }
                            }
                        }
                    }
                    it.flyingCentre(-500f, -500f)
                }
            }
        }

        for (k in 1..3) {
            if (texts[k].hovered && (texts[k].pressing && phText.sceneNotOver()) && texts[0].revealed){
                if(devices[0].status!=DeviceStatus.NORMAL){
                    chosenOption = (1..3).random()
                }
                else{
                    chosenOption = k
                }
                texts[4].setStuff(texts[chosenOption].text, texts[chosenOption].left, texts[chosenOption].top, texts[chosenOption].width,texts[chosenOption].height)
                if(!SharedVariables.sfxMuted){
                    sfxChoose.play(SharedVariables.sfxVolume)
                }
                phText.nextPassage(k)
                updateIslandText()
            }
            texts[k].pressing = false
        }

        if(!phText.sceneNotOver() && !countdownTimer.running && !travelTimer.running){
            redButton.glowing = true
        }

        if (!travelTimer.running && !phText.sceneNotOver()) {
            when( SharedVariables.planetIndex){
                1-> {
                    tools.forEach {
                        it.status = ToolStatus.IDLE
                    }
                    breakingList = listOf(0, 1, 2, 3)
                }
                2->if (phText.getTag()=="Good"){ cargoBay.addToItems("stacey") }
                3->if (phText.getTag()=="Good"){ cargoBay.addToItems("dessert") }
                4->if (phText.getTag()=="Good"){ cargoBay.addToItems("flower") }
            }
            if(redButton.status == ButtonStatus.DOWN && !countdownTimer.running && redButton.glowing) {
                if(SharedVariables.planetIndex==5){
                    when(phText.getTag()){
                        "Good" -> SharedVariables.endingScreen.goodEnder()
                        "Bad" -> SharedVariables.endingScreen.badEnder()
                    }
                    SharedVariables.activeScreen = SharedVariables.endingScreen
                }
                if(!SharedVariables.sfxMuted){
                    sfxRed.play(SharedVariables.sfxVolume)
                }
                redButton()
            }
        }
        redButton.status = ButtonStatus.UP
    }

    private fun updateIslandText() {
        texts[0].setStuff(phText.getCurrentLine())
        texts[0].letterRevealReset()
        try {
            phText.getCurrentChoices().let {
                texts[1].setStuff(it[0])
                texts[2].setStuff(it[1])
                texts[3].setStuff(it[2])
            }
        } catch (ex: Exception) {
            texts[1].setStuff("")
            texts[2].setStuff("")
            texts[3].setStuff("")
        }
    }

    private fun redButton() {
        redButton.glowing = false
        if(patrolFlag){
            phText.getPlanetPassage(218)
            bigMonitor.status = BigMonitorStatus.Ps
            //bigMonitor.changeMonitor("graphics/planets/ps.png")
            updateIslandText()
            for(device in devices){
                device.breakTimer.running = false
                device.status = DeviceStatus.NORMAL
            }
            chosenOption = -1
            patrolFlag = false
            return
        }
        else {
            //countdownTimer.go()
            bigMonitor.activateCountdown()
            texts[0].setStuff("")
        }
    }

    private fun diceThrowingFunctionThatThrowsBetweenZeroAndTenInsteadOfItBeingAVariable(): Boolean {
        val rng = (0..10).random()
        if (rng < SharedVariables.planetIndex*2+2) return true
        return false
    }

    private fun breakShip() {
        val device = when ((0..3).random()) {
            0 -> devices[0]
            1 -> devices[1]
            2 -> devices[2]
            else -> devices[3]
        }

        if (device.status == DeviceStatus.NORMAL) {
            when (breakingList.random()) {
                0 -> device.breakDevice(DeviceStatus.BROKEN)
                1 -> device.breakDevice(DeviceStatus.STUCK)
                2 -> device.breakDevice(DeviceStatus.SHORT)
                else -> device.breakDevice(DeviceStatus.HOT)
            }
        }
    }

    override fun loopAction() {
        /*
        if(countdownTimer.running){
            if(countdownTimer.done()){
                if(countdownIndex==countdownIndexLimit){
                    texts[0].setStuff("")

                    bigMonitor.changeMonitor(countdownList[4])
                    travelTimer.go()
                    for(device in devices){
                        device.breakTimer.resume()
                    }
                    countdownTimer.running = false
                    countdownIndex = -1
                    // bigMonitor.changeMonitor("graphics/spaceview.png")
                }
                else{
                    countdownIndex++
                    bigMonitor.changeMonitor(countdownList[countdownIndex])
                    chosenOption = -1
                    countdownTimer.go()
                }
            }
        }
         */
        when (bigMonitor.loopAction()){
            1-> {
                travelTimer.go()
                for(device in devices){
                    device.breakTimer.resume()
                }
                chosenOption = -1
                texts[0].setStuff("")
            }

        }

        if (travelTimer.running) {
            devices.forEach {
                if(it.checkTimer() && !patrolFlag){
                    patrolFlag = true
                }
            }
            if (travelTimer.done()) {
                travelTimer.running = false
                devices.forEach { it.breakTimer.pause() }
                changePlanet()
            } else if ((travelTimer.timeLeft()>1000)&&(timer.done())){
                if (diceThrowingFunctionThatThrowsBetweenZeroAndTenInsteadOfItBeingAVariable()) {
                    breakShip()
                }
                timer.go()
            }
        }

        tools.forEach {
            if(it.status!= ToolStatus.INACTIVE){
                if (SharedVariables.contains(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), it.getSprite())) {
                    it.status = ToolStatus.GLOW
                } else {
                    it.status = ToolStatus.IDLE
                }
            }
        }
        if(texts[0].revealed){
            sfxBeep.stop()
        }
        else{
            if(!SharedVariables.sfxMuted){
                sfxBeep.play(SharedVariables.sfxVolume)
            }
        }

        for (k in 1..3){
            texts[k].hovered=false
        }

        for (k in 1..3) {
            if (texts[k].contains(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())){
                texts[k].hovered = true
                break
            }
        }
    }

    private fun changePlanet() {
        when (SharedVariables.planetIndex) {
            0 -> {
                when {
                    devices[1].status != DeviceStatus.NORMAL -> { // speaker
                        phText.getPlanetPassage(25)
                    }
                    devices[3].status != DeviceStatus.NORMAL -> { //translator
                        phText.getPlanetPassage(15)
                    }
                    else -> {
                        phText.getPlanetPassage(7)
                    }
                }
                //bigMonitor.changeMonitor("graphics/planets/p1.png")
                bigMonitor.status = BigMonitorStatus.P1
                countdownIndexLimit = 2
            }
            1 -> {
                if (devices[1].status != DeviceStatus.NORMAL) {
                    phText.getPlanetPassage(112)
                } else {
                    phText.getPlanetPassage(93)
                }
                //bigMonitor.changeMonitor("graphics/planets/p2.png")
                bigMonitor.status = BigMonitorStatus.P2
            }
            2 -> {
                when {
                    devices[1].status != DeviceStatus.NORMAL -> phText.getPlanetPassage(61) //speaker
                    devices[3].status != DeviceStatus.NORMAL -> phText.getPlanetPassage(3) //translator
                    else -> phText.getPlanetPassage(36)
                }
                //bigMonitor.changeMonitor("graphics/planets/p3.png")
                bigMonitor.status = BigMonitorStatus.P3
            }
            3 -> {
                when {
                    devices[1].status != DeviceStatus.NORMAL -> phText.getPlanetPassage(174) //speaker
                    devices[3].status != DeviceStatus.NORMAL -> phText.getPlanetPassage(196) //translator
                    else -> phText.getPlanetPassage(146)
                }
                //bigMonitor.changeMonitor("graphics/planets/p4.png")
                bigMonitor.status = BigMonitorStatus.P4
            }
            4 -> {
                cargoBay.endingTree().also{
                    phText.getPlanetPassage(it)
                }
                //bigMonitor.changeMonitor("graphics/planets/p5.png")
                bigMonitor.status = BigMonitorStatus.P5
            }
        }
        SharedVariables.planetIndex++
        updateIslandText()
    }

    override fun lateInitializer() {
        bigMonitor = BigMonitor().apply{
            //changeMonitor("graphics/planets/p0.png")
            status = BigMonitorStatus.P0
        }
        mainSprite = SharedVariables.loadSprite(SharedVariables.gameBackgroundPath, SharedVariables.gameBackgroundRatio)
        mainSprite.setCenterX(SharedVariables.mainWidth.toFloat() / 2)
        mainSprite.setCenterY(SharedVariables.mainHeight.toFloat() / 2)
        redButton = RedButton(DevicesData.redPath, DevicesData.redRatio)
        redButton.relocateCentre(DevicesData.redX, DevicesData.redY)
        devices = DevicesData.getDevices()
        tools = ToolsData.getTools()
        phText = TextIsland(Gdx.files.internal("planet_0/story.json"), SharedVariables.planets[0].second)
        TextIslandTexts().also {
            it.setStuff(phText.getCurrentLine(), 532f, 433f, 835f, 140f)
            texts.add(it)
        }
        phText.getCurrentChoices().let{
            TextIslandTexts().apply {
                setStuff(it[0],250f,250f,1250f,65f)
                texts.add(this)
            }
            TextIslandTexts().apply {
                setStuff(it[1],250f,185f,1250f,65f)
                texts.add(this)
            }
            TextIslandTexts().apply {
                setStuff(it[2],250f,120f,1250f,65f)
                texts.add(this)
            }
        }
        TextIslandTexts().also {
            it.setStuff("", 250f, 0f, 1250f,65f)
            texts.add(it)
        }

        cargoBay = CargoBay()
        timer.go()
    }
}