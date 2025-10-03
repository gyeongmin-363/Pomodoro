package com.malrang.pomodoro.dataclass.animalInfo

object AnimalsTable {
    // 모든 동물을 포함하는 목록입니다.
    private val allAnimals = Animal.entries

    fun byId(id: String): Animal? = allAnimals.find { it.id == id }
}
