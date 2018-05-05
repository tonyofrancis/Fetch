package com.tonyodev.fetch2.util

class AverageCalculator @JvmOverloads constructor(
        /**
         * This value indicates how many values the value list can hold.
         * If a newer value is being add and the data list is full, the oldest value is deleted
         * and the newer value added. Works like a simple cache. The default value is 0 indicating
         * that values are not discarded.
         * */
        val discardLimit: Int = 0) {

    private val defaultValueListSize = 16
    private val defaultIndexPosition = -1

    /** Linked list that holds values.*/
    private var valueList = DoubleArray(defaultValueListSize)
    private var startIndex = defaultIndexPosition
    private var endIndex = defaultIndexPosition

    /** Add a new value to be calculated in the average. If the values list is equal to the discard limit
     * and the discard limit is greater than 0, the oldest value is discarded.
     * @param value value to be calculated in average.
     * */
    fun add(value: Double) {
        if (discardLimit > 0 && count() == discardLimit) {
            startIndex += 1
        }
        if (endIndex == valueList.size - 1) {
            expandValueList()
        }
        endIndex += 1
        if (endIndex == 0) {
            startIndex = endIndex
        }
        valueList[endIndex] = value
    }

    /** Doubles the value list size.*/
    private fun expandValueList() {
        val newList = DoubleArray(valueList.size * 2)
        val length = count()
        System.arraycopy(valueList, startIndex, newList,0, length)
        valueList = newList
        startIndex = 0
        endIndex = length - 1
    }

    /** Clear all values.*/
    fun clear() {
        valueList = DoubleArray(defaultValueListSize)
        startIndex = defaultIndexPosition
        endIndex = defaultIndexPosition
    }

    /** Gets the number of values that are being held in the values list.
     * @return number of items held in the values list.
     * */
    fun count(): Int {
        return (endIndex - startIndex) + 1
    }

    /**
     * Gets the last input value or null if the values list is empty.
     * @return last input value or null.
     * */
    fun getLastInputValue(): Double {
        if (count() < 1) {
            throw ArrayIndexOutOfBoundsException("value array is empty")
        }
        return valueList[endIndex]
    }

    /**
     * Gets the first input value or null if the values list is empty.
     * @return first input value or null.
     * */
    fun getFirstInputValue(): Double {
        if (count() < 1) {
            throw ArrayIndexOutOfBoundsException("value array is empty")
        }
        return valueList[startIndex]
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
        var total = 0.0
        for (index in startIndex..endIndex) {
            total += valueList[index]
        }
        return total / count().toDouble()
    }

    /** Gets the moving average with more weight being placed on recent values.
     * @param inclusionCount the number of recent values to be included in the moving average calculation.
     * Default is all values in the values list.
     * @return moving average with more weight placed on the recent values.
     * */
    @JvmOverloads
    fun getMovingAverageWithWeightOnRecentValues(inclusionCount: Int = count()): Double {
        if (inclusionCount < 1) {
            throw IllegalArgumentException("inclusionCount cannot be less than 1.")
        }
        if (inclusionCount > count()) {
            throw IllegalArgumentException("inclusionCount cannot be greater than the inserted value count.")
        }
        var movingAverage = 0.0
        var weight = inclusionCount
        val denominator = getDenominator(inclusionCount)
        for (index in endIndex downTo (endIndex - (inclusionCount - 1))) {
            movingAverage += (valueList[index] * (weight / denominator))
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
    fun getMovingAverageWithWeightOnOlderValues(inclusionCount: Int = count()): Double {
        if (inclusionCount < 1) {
            throw IllegalArgumentException("inclusionCount cannot be less than 1.")
        }
        if (inclusionCount > count()) {
            throw IllegalArgumentException("inclusionCount cannot be greater than the inserted value count.")
        }
        var movingAverage = 0.0
        var weight = inclusionCount
        val denominator = getDenominator(inclusionCount)
        for (index in startIndex..(startIndex + (inclusionCount - 1))) {
            movingAverage += (valueList[index] * (weight / denominator))
            --weight
        }
        return movingAverage
    }

    private fun getDenominator(number: Int): Double {
        var total = 0.0
        for (n in 1..number) {
            total += n
        }
        return total
    }

}