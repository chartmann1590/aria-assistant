package com.aria.assistant.skill

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnitConversionSkill @Inject constructor() {

    fun convert(value: Double, fromUnit: String, toUnit: String): SkillResult<String> {
        val from = fromUnit.lowercase().trim()
        val to = toUnit.lowercase().trim()

        return if ((from.contains("celsius") && to.contains("fahrenheit")) ||
            (from.contains("fahrenheit") && to.contains("celsius"))) {
            convertTemperature(value, from, to)
        } else {
            convertWithFactor(value, from, to)
        }
    }

    private fun convertWithFactor(value: Double, fromUnit: String, toUnit: String): SkillResult<String> {
        val key = normalizeKey(fromUnit) + "->" + normalizeKey(toUnit)
        val factor = conversionTable[key] ?: conversionTable[normalizeKey(toUnit) + "->" + normalizeKey(fromUnit)]?.let { 1.0 / it } ?:
            return SkillResult.Failure("I don't know how to convert that")

        val result = value * factor
        val formatted = if (result == result.toLong().toDouble()) result.toLong().toString() else String.format("%.2f", result)
        return SkillResult.Success("$value $fromUnit = $formatted $toUnit")
    }

    private fun convertTemperature(value: Double, from: String, to: String): SkillResult<String> {
        val celsius = if (from.contains("celsius")) value else (value - 32.0) * 5.0 / 9.0
        val result = if (to.contains("celsius")) celsius else celsius * 9.0 / 5.0 + 32.0
        val formatted = if (result == result.toLong().toDouble()) result.toLong().toString() else String.format("%.1f", result)
        val toLabel = if (to.contains("fahrenheit")) "°F" else "°C"
        return SkillResult.Success("$value ${from.take(4)} = $formatted $toLabel")
    }

    private fun normalizeKey(unit: String): String = when {
        unit.startsWith("meter") -> "meters"
        unit.startsWith("feet") || unit.startsWith("foot") -> "feet"
        unit.startsWith("mile") -> "miles"
        unit.startsWith("kilometer") || unit == "km" -> "kilometers"
        unit.startsWith("centimeter") || unit == "cm" -> "centimeters"
        unit.startsWith("inch") -> "inches"
        unit.startsWith("kilogram") || unit == "kg" -> "kilograms"
        unit.startsWith("pound") || unit == "lb" -> "pounds"
        unit.startsWith("gram") -> "grams"
        unit.startsWith("ounce") -> "ounces"
        unit.startsWith("liter") -> "liters"
        unit.startsWith("gallon") -> "gallons"
        unit.startsWith("milliliter") || unit == "ml" -> "milliliters"
        unit.startsWith("fluid_ounce") || unit == "fl oz" -> "fluid_ounces"
        unit.startsWith("cup") -> "cups"
        unit.startsWith("kmh") || unit == "km/h" -> "kmh"
        unit.startsWith("mph") -> "mph"
        unit.startsWith("meter_per_second") || unit == "m/s" -> "meters_per_second"
        else -> unit
    }

    private val conversionTable = mapOf(
        "meters->feet" to 3.28084,
        "meters->miles" to 0.000621371,
        "meters->kilometers" to 0.001,
        "kilometers->miles" to 0.621371,
        "centimeters->inches" to 0.393701,
        "feet->inches" to 12.0,
        "kilograms->pounds" to 2.20462,
        "grams->ounces" to 0.035274,
        "liters->gallons" to 0.264172,
        "milliliters->fluid_ounces" to 0.033814,
        "cups->liters" to 0.236588,
        "kmh->mph" to 0.621371,
        "meters_per_second->kmh" to 3.6,
    )
}
