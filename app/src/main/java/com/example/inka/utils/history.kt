package com.example.inka

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import com.example.inka.db.Stroke
import com.onyx.android.sdk.utils.BroadcastHelper.App


sealed class Operation {
    data class DeleteStroke(val strokeId: String) : Operation()
    data class AddStroke(val stroke: Stroke) : Operation()
}

typealias OperationBlock = List<Operation>
typealias OperationList = MutableList<OperationBlock>

var undoList: OperationList = mutableListOf()
var redoList: OperationList = mutableListOf()

fun treatOperation(context: Context, operation: Operation): Pair<Operation, RectF> {
    val appRepository = AppRepository(context)
    return when (operation) {
        is Operation.AddStroke -> {
            appRepository.strokeRepository.create(operation.stroke)
            return Operation.DeleteStroke(strokeId = operation.stroke.id) to RectF(
                operation.stroke.left,
                operation.stroke.top,
                operation.stroke.right,
                operation.stroke.bottom
            )
        }
        is Operation.DeleteStroke -> {
            val stroke = appRepository.strokeRepository.getStrokeWithPointsById(operation.strokeId)
            appRepository.strokeRepository.deleteAll(listOf(operation.strokeId))
            return Operation.AddStroke(stroke = stroke) to RectF(
                stroke.left,
                stroke.top,
                stroke.right,
                stroke.bottom
            )
        }
        else -> {
            throw (java.lang.Error("Unhandled history operation"))
        }
    }
}

enum class UndoRedoType {
    Undo,
    Redo
}

fun undoRedo(context: Context, type: UndoRedoType): RectF {
    val originList =
        if (type == UndoRedoType.Undo) undoList else redoList
    val targetList =
        if (type == UndoRedoType.Undo) redoList else undoList

    if (originList.size == 0) return RectF()

    val operationBlock = originList.removeLast()
    val revertOperations = mutableListOf<Operation>()
    val zoneAffected = RectF()
    for (operation in operationBlock) {
        val (cancelOperation, thisZoneAffected) = treatOperation(context, operation)
        revertOperations.add(cancelOperation)
        zoneAffected.union(thisZoneAffected)
    }
    targetList.add(revertOperations)

    // reload stroke cache
    loadStrokeCache(context)

    // return the affected zone
    return zoneAffected
}

fun clearHistory() {
    undoList.clear()
    redoList.clear()
}

fun addOperationsToHistory(operations: OperationBlock) {
    undoList.add(operations)
    redoList.clear()
}