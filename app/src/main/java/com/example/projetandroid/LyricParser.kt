package com.example.projetandroid


class LyricParser(fileContent: String) {
    var lyrics:List<List<Lyric>> = listOf()
    val information: MutableMap<String, String> = mutableMapOf()
    init {
        val lyricList = fileContent.lines()
        if(lyricList[0] == "SingWithMe"){
            try {
                val lyricsStart = lyricList.indexOfFirst { it.startsWith("# lyrics") } + 1
                if(lyricsStart == 0) throw IllegalArgumentException()
                for (i in 1..<lyricsStart - 1 step 2) {
                    if (lyricList[i].startsWith("#")) information[lyricList[i].drop(2)] =
                        lyricList[i + 1]
                }
                val r = Regex("""(?=((\{ ?\d+:\d+:?\d+ ?\})([^\n{]+)\n?(\{ ?\d+:?\d+ ?\}\n?)))""")
                val lyricsList: MutableList<Lyric> = mutableListOf()
                r.findAll(fileContent).forEachIndexed { _, v ->
                    val groups = v.groups
                    lyricsList.add(
                        Lyric(
                            groups[2]?.value ?: "",
                            groups[3]?.value ?: "",
                            groups[4]?.value ?: "",
                            toreunite = groups[1]?.value?.contains("\n") == false
                        )
                    )
                }
                val reunifiedLyrics: MutableList<MutableList<Lyric>> = mutableListOf()
                var flag = false
                lyricsList.forEach {
                    if (!flag) {
                        reunifiedLyrics.add(mutableListOf(it))
                    } else {
                        reunifiedLyrics.last().add(it)
                    }
                    flag = it.toreunite
                }
                this.lyrics = reunifiedLyrics
            }
            catch (e: IllegalArgumentException){
                throw e
            }
        }
        else{
            throw IllegalArgumentException()
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