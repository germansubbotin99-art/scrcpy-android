package tech.devline.scropy_ui.operator

/**
 * Operator safety layer.
 * Blocks accidental touch control while keeping video stream active.
 */
class InputLockController {
    private var locked = false

    fun lock() {
        locked = true
    }

    fun unlock() {
        locked = false
    }

    fun toggle(): Boolean {
        locked = !locked
        return locked
    }

    fun isLocked(): Boolean = locked
}
