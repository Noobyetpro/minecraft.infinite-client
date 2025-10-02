package org.theinfinitys.settings

class Property<T>(
    initialValue: T,
) {
    var value: T = initialValue
        set(newValue) {
            if (field != newValue) {
                val oldValue = field
                field = newValue
                listeners.forEach { it(oldValue, newValue) }
            }
        }

    private val listeners = mutableListOf<(oldValue: T, newValue: T) -> Unit>()

    fun addListener(listener: (oldValue: T, newValue: T) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (oldValue: T, newValue: T) -> Unit) {
        listeners.remove(listener)
    }
}
