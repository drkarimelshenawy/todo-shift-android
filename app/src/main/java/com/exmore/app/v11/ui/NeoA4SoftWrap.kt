package com.exmore.app.v11.ui

/** Read-only pagination boundaries. Never writes page breaks into note source. */
internal object NeoA4SoftWrap {
    data class Range(val start:Int,val end:Int)
    data class Slice(val start:Int,val end:Int,val topPx:Int,val bottomPx:Int) {
        init {require(start<=end && topPx<=bottomPx)}
        val heightPx:Int get()=bottomPx-topPx
    }

    /** Slice locations in the ONE shaped editor layout, retaining source offsets. */
    fun slices(length:Int,lineStarts:IntArray,lineTops:IntArray,
        lineBottoms:FloatArray,contentHeightPx:Int,maxHeightPx:Int,
        firstHeightPx:Int=maxHeightPx):List<Slice> {
        require(maxHeightPx>0 && contentHeightPx>=0)
        require(lineStarts.size==lineTops.size && lineTops.size==lineBottoms.size)
        require(lineTops.all{it in 0..contentHeightPx})
        // A corrupt layout must never generate a negative or clipped page slice.
        require(lineBottoms.indices.all { i ->
            lineBottoms[i].isFinite() && lineBottoms[i]>=lineTops[i].toFloat() &&
                lineBottoms[i]<=contentHeightPx.toFloat()
        })
        for(i in 1 until lineTops.size)require(lineTops[i]>=lineTops[i-1])
        require(firstHeightPx>0)
        val split=ranges(length,lineStarts,lineBottoms,maxHeightPx.toFloat(),firstHeightPx.toFloat())
        return split.mapIndexed { index,range ->
            val first=if(index==0)0 else
                lineStarts.indexOf(range.start).also{require(it>=0)}
            val next=if(index==split.lastIndex)contentHeightPx else {
                val nextLine=lineStarts.indexOf(split[index+1].start)
                require(nextLine>=0)
                lineTops[nextLine]
            }
            Slice(range.start,range.end,lineTops.getOrElse(first){0},next)
        }
    }

    /**
     * Splits at real text-layout line starts (UTF-16 offsets), not guessed
     * character counts, preserving shaped Arabic and combining sequences.
     */
    fun ranges(length:Int,lineStarts:IntArray,lineBottoms:FloatArray,
        maxHeightPx:Float,firstHeightPx:Float=maxHeightPx):List<Range> {
        require(length>=0 && lineStarts.size==lineBottoms.size)
        require(maxHeightPx>0f && maxHeightPx.isFinite())
        require(firstHeightPx>0f && firstHeightPx.isFinite())
        if(length==0 || lineStarts.isEmpty())return listOf(Range(0,length))
        require(lineStarts[0]==0 && lineStarts.all{it in 0..length})
        for(i in 1 until lineStarts.size)require(lineStarts[i]>=lineStarts[i-1])
        require(lineBottoms.all{it.isFinite() && it>=0f})
        // Layout bottoms must be ordered: otherwise pagination may silently
        // omit or overlap visual lines while source offsets still look valid.
        for(i in 1 until lineBottoms.size)
            require(lineBottoms[i]>=lineBottoms[i-1])
        val result=mutableListOf<Range>()
        var start=0
        var firstLine=0
        var capacity=firstHeightPx
        for(i in 1 until lineStarts.size) {
            val preceding=if(firstLine==0)0f else lineBottoms[firstLine-1]
            if(lineBottoms[i]-preceding>capacity) {
                val cut=lineStarts[i]
                if(cut>start) {
                    result+=Range(start,cut)
                    start=cut
                    firstLine=i
                    capacity=maxHeightPx
                }
            }
        }
        result+=Range(start,length)
        return result
    }
}
