package com.digitaldiscipline.spike.data.local

import androidx.room.TypeConverter
import com.digitaldiscipline.spike.data.local.entities.RuleMode

class Converters {
    @TypeConverter
    fun fromRuleMode(mode: RuleMode?): String {
        return mode?.name ?: RuleMode.BLOCK.name
    }

    @TypeConverter
    fun toRuleMode(value: String?): RuleMode {
        return if (value == null) RuleMode.BLOCK else try {
            RuleMode.valueOf(value)
        } catch (e: Exception) {
            RuleMode.BLOCK
        }
    }
}
