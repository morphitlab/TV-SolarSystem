package com.solarsystemlive.data

import android.content.Context
import org.json.JSONObject

class FactsRepository(context: Context) {
    private val factsMap: Map<String, BodyFacts>

    init {
        val raw = context.assets.open("facts.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        val map = HashMap<String, BodyFacts>()
        for (key in root.keys()) {
            val obj = root.getJSONObject(key)
            val arr = obj.getJSONArray("facts")
            val list = List(arr.length()) { arr.getString(it) }
            map[key.uppercase()] = BodyFacts(
                id = key.uppercase(),
                diameter = obj.getString("diameter"),
                dayLength = obj.getString("dayLength"),
                yearLength = obj.getString("yearLength"),
                moons = obj.getString("moons"),
                facts = list
            )
        }
        factsMap = map
    }

    fun getFacts(body: CelestialBody): BodyFacts {
        return factsMap[body.name] ?: BodyFacts(
            id = body.name,
            diameter = "—",
            dayLength = "—",
            yearLength = "—",
            moons = "—",
            facts = listOf("No facts available.")
        )
    }
}

/** Pure Kotlin helper for cycling facts cleanly (unit-testable). */
class FactCycler(private val totalFacts: Int) {
    var currentIndex: Int = 0
        private set

    fun next(): Int {
        if (totalFacts <= 0) return 0
        currentIndex = (currentIndex + 1) % totalFacts
        return currentIndex
    }

    fun reset() {
        currentIndex = 0
    }
}
