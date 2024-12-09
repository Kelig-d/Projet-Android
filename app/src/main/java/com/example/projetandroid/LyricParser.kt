package com.example.projetandroid

import androidx.compose.animation.core.StartOffset


class LyricParser(fileContent: String) {
    lateinit var lyrics:List<List<Lyric>>
    init {
        val lyricList = fileContent.lines()
        if(lyricList[0] == "SingWithMe"){
            val lyricsStart = lyricList.indexOfFirst { it.startsWith("{") }
            val lyricContentInfo: MutableMap<String, String> = mutableMapOf()
            for(i in 1..<lyricsStart-1 step 2){
                if(lyricList[i].startsWith("#")) lyricContentInfo[lyricList[i].drop(2)] = lyricList[i+1]
            }
            val r = Regex("""(?=((\{ ?\d+:\d+:?\d+ ?\})([^\n{]+)\n?(\{ ?\d+:?\d+ ?\}\n?)))""")
            val lyricsList:MutableList<Lyric> = mutableListOf()
            r.findAll(fileContent).forEachIndexed { i,v ->
                val groups = v.groups
                lyricsList.add(Lyric(groups[2]?.value ?: "", groups[3]?.value ?: "", groups[4]?.value?: "", toreunite = groups[1]?.value?.contains("\n") == false))
            }
            val reunifiedLyrics:MutableList<MutableList<Lyric>> = mutableListOf()
            var flag:Boolean = false
            lyricsList.forEach{
                if(!flag) {
                    reunifiedLyrics.add(mutableListOf(it))
                }
                else{
                    reunifiedLyrics.last().add(it)
                }
                flag = it.toreunite
            }
            this.lyrics = reunifiedLyrics
        }
    }
}

class Lyric(startOffset:String, val sentence: String, endOffset: String, val toreunite:Boolean = false){
    private val timeRegex = Regex("""(\d+):?(\d+)\.?(\d+)?""")
    private val filteredStartOffset = timeRegex.find(startOffset)!!.groups
    val startOffset:Float = filteredStartOffset[1]!!.value.toFloat()*60000 + filteredStartOffset[2]!!.value.toFloat() * 1000 + (filteredStartOffset[3]?.value?.toFloat()
        ?.times(100) ?: 0.0f)
    private val filteredEndOffset = timeRegex.find(endOffset)!!.groups
    val endOffset:Float = filteredEndOffset[1]!!.value.toFloat() * 60000 + filteredEndOffset[2]!!.value.toFloat() * 1000 + (filteredEndOffset[3]?.value?.toFloat()
        ?.times(100) ?: 0.0f)
    override fun toString(): String{
        return "{$startOffset ms} $sentence {$endOffset ms}"
    }
}