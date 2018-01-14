package com.tonyodev.fetch2.util

import java.util.*

class AverageCalculator @JvmOverloads constructor(
        /**
         * This value indicates how many values the value list can hold.
         * If a newer value is being add and the data list is full, the oldest value is deleted
         * and the newer value added. Works like a simple cache. The default value is 0 indicating
         * that values are not discarded.
         * */
        val discardLimit: Int = 0) {

    /** Linked list that holds values.*/
    private val valueList = LinkedList<Double>()

    /** Add a new value to be calculated in the average. If the values list is equal to the discard limit
     * and the discard limit is greater than 0, the oldest value is discarded.
     * @param value value to be calculated in average.
     * */
    fun add(value: Double) {
        if (discardLimit > 0 && valueList.size == discardLimit) {
            valueList.removeFirst()
        }
        valueList.addLast(value)
    }

    /** Clear all values.*/
    fun clear() {
        valueList.clear()
    }

    /** Gets the number of values that are being held in the values list.
     * @return number of items held in the values list.
     * */
    fun count(): Int {
        return valueList.size
    }

    /**
     * Gets the last input value or null if the values list is empty.
     * @return last input value or null.
     * */
    fun getLastInputValue(): Double? {
        return valueList.lastOrNull()
    }

    /**
     * Gets the first input value or null if the values list is empty.
     * @return first input value or null.
     * */
    fun getFirstInputValue(): Double? {
        return valueList.firstOrNull()
    }

    /**
     * Checks if the passed in value is in the values list.
     * @param value value to check against.
     * @return true if the passed in value was found in the values list. False otherwise.
     * */
    fun hasInputValue(value: Double): Boolean {
        return valueList.contains(value)
    }

    /** Gets all values contained in the values list.
     * @return list of values contained in the values list.
     * */
    fun getValues(): List<Double> {
        return valueList.toList()
    }

    /** Get the simple average of all values in the values list.
     * @return simple value
     * */
    fun getAverage(): Double {
        val size = valueList.size
        val total = valueList.sum()
        return total / size.toDouble()
    }

    /** Gets the moving average with more weight being placed on recent values.
     * @param inclusionCount the number of recent values to be included in the moving average calculation.
     * Default is all values in the values list.
     * @return moving average with more weight placed on the recent values.
     * */
    @JvmOverloads
    fun getMovingAverageWithWeightOnRecentValues(inclusionCount: Int = valueList.size): Double {
        if (inclusionCount < 1) {
            throw IllegalArgumentException("inclusionCount cannot be less than 1.")
        }
        if (inclusionCount > valueList.size) {
            throw IllegalArgumentException("inclusionCount cannot be greater than the inserted value count.")
        }
        val values = valueList.reversed().subList(0, inclusionCount)
        var movingAverage = 0.0
        var weight = inclusionCount
        val denominator = getDenominator(inclusionCount)
        values.forEach { value ->
            movingAverage += (value * (weight / denominator))
            --weight
        }
        return movingAverage
    }

    /** Gets the moving average with more weight being placed on the oldest values.
     * @param inclusionCount the number of oldest values to be included in the moving average calculation.
     * Default is all values in the values list.
     * @return moving average with more weight placed on the the oldest values.
     * */
    @JvmOverloads
    fun getMovingAverageWithWeightOnOlderValues(inclusionCount: Int = valueList.size): Double {
        if (inclusionCount < 1) {
            throw IllegalArgumentException("inclusionCount cannot be less than 1.")
        }
        if (inclusionCount > valueList.size) {
            throw IllegalArgumentException("inclusionCount cannot be greater than the inserted value count.")
        }
        val values = valueList.subList(0, inclusionCount)
        var movingAverage = 0.0
        var weight = inclusionCount
        val denominator = getDenominator(inclusionCount)
        values.forEach { value ->
            movingAverage += (value * (weight / denominator))
            --weight
        }
        return movingAverage
    }

    private fun getDenominator(number: Int): Double {
        return (1..number).sum().toDouble()
    }

}