package com.malrang.pomodoro.dataclass.animalInfo

import com.malrang.pomodoro.dataclass.animalInfo.Rarity

object AnimalsTable {
    // 희귀도별로 Animal enum class의 멤버들을 필터링하여 목록을 생성합니다.
    private val common = Animal.entries.filter { it.rarity == Rarity.COMMON }
    private val rare = Animal.entries.filter { it.rarity == Rarity.RARE }
    private val epic = Animal.entries.filter { it.rarity == Rarity.EPIC }
    private val legendary = Animal.entries.filter { it.rarity == Rarity.LEGENDARY }

    // 모든 동물을 포함하는 목록입니다.
    private val allAnimals = Animal.entries

    fun byId(id: String): Animal? = allAnimals.find { it.id == id }

    fun randomByRarity(r: Rarity): Animal = when (r) {
        Rarity.COMMON -> common.random()
        Rarity.RARE -> rare.random()
        Rarity.EPIC -> epic.random()
        Rarity.LEGENDARY -> legendary.random()
    }
}
