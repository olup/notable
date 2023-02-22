package com.example.inka

import android.content.Context
import com.example.inka.db.StrokeWithPoints
import com.onyx.android.sdk.utils.BroadcastHelper.App


sealed class Operation {
    data class DeleteStroke(val strokeId: Int) : Operation()
    data class AddStroke(val stroke: StrokeWithPoints) : Operation()
}

typealias OperationBlock = List<Operation>
typealias OperationList = MutableList<OperationBlock>

var undoList: OperationList = mutableListOf()
var redoList: OperationList = mutableListOf()

fun treatOperation(context: Context, operation: Operation): Operation {
    val appRepository = AppRepository(context)
    return when (operation) {
        is Operation.AddStroke -> {
            appRepository.strokeRepository.create(operation.stroke.stroke)
            appRepository.pointRepository.create(operation.stroke.points)
            return Operation.DeleteStroke(strokeId = operation.stroke.stroke.id)
        }
        is Operation.DeleteStroke -> {
            val stroke = appRepository.strokeRepository.getStrokeWithPointsById(operation.strokeId)
            println(stroke)
            appRepository.strokeRepository.deleteAll(listOf(operation.strokeId))
            return Operation.AddStroke(stroke = stroke)
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

fun undoRedo(context: Context, type: UndoRedoType) {
    val originList =
        if (type == UndoRedoType.Undo) undoList else redoList
    val targetList =
        if (type == UndoRedoType.Undo) redoList else undoList

    if (originList.size == 0) return

    val operationBlock = originList.removeLast()
    val revertOperations = mutableListOf<Operation>()
    for (operation in operationBlock) {
        revertOperations.add(treatOperation(context, operation))
    }
    targetList.add(revertOperations)

    // reload stroke cache
    loadStrokeCache(context)
}

fun clearHistory() {
    undoList.clear()
    redoList.clear()
}

fun addOperationsToHistory(operations: OperationBlock) {
    undoList.add(operations)
    redoList.clear()
}