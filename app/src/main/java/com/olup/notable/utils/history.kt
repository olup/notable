package com.olup.notable

import android.graphics.Rect
import com.olup.notable.db.Stroke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch


sealed class Operation {
    data class DeleteStroke(val strokeIds: List<String>) : Operation()
    data class AddStroke(val strokes: List<Stroke>) : Operation()
}

typealias OperationBlock = List<Operation>
typealias OperationList = MutableList<OperationBlock>

enum class UndoRedoType {
    Undo,
    Redo
}

sealed class HistoryBusActions {
    data class RegisterHistoryOperationBlock(val operationBlock: OperationBlock) : HistoryBusActions()
    data class MoveHistory(val type: UndoRedoType) : HistoryBusActions()
}

class History(coroutineScope: CoroutineScope, pageView: PageView) {

    private var undoList: OperationList = mutableListOf()
    private var redoList: OperationList = mutableListOf()
    val pageModel = pageView

    // TODO maybe not in a companion object ?
    companion object {
        val historyBus = MutableSharedFlow<HistoryBusActions>()
        suspend fun registerHistoryOperationBlock(operationBlock : OperationBlock){
            historyBus.emit(HistoryBusActions.RegisterHistoryOperationBlock(operationBlock))
        }
        suspend fun moveHistory(type: UndoRedoType){
            historyBus.emit(HistoryBusActions.MoveHistory(type))
        }
    }


    init {
        coroutineScope.launch {
            historyBus.collect {
                when (it) {
                    is HistoryBusActions.MoveHistory -> {
                        val zoneAffected = undoRedo(type = it.type)
                        if(zoneAffected != null) {
                            pageView.drawArea(pageAreaToCanvasArea(zoneAffected, pageView.scroll))
                        }
                    }
                    is HistoryBusActions.RegisterHistoryOperationBlock -> { addOperationsToHistory(it.operationBlock)}
                    else -> {}
                }
            }
        }
    }

    private fun treatOperation(operation: Operation): Pair<Operation, Rect> {
        return when (operation) {
            is Operation.AddStroke -> {
                pageModel.addStrokes(operation.strokes)
                return Operation.DeleteStroke(strokeIds = operation.strokes.map{it.id}) to strokeBounds(operation.strokes)
            }
            is Operation.DeleteStroke -> {
                val strokes = pageModel.getStrokes(operation.strokeIds).filterNotNull()
                pageModel.removeStrokes(operation.strokeIds)
                return Operation.AddStroke(strokes = strokes) to strokeBounds(strokes)
            }
            else -> {
                throw (java.lang.Error("Unhandled history operation"))
            }
        }
    }

    private fun undoRedo(type: UndoRedoType): Rect? {
        val originList =
            if (type == UndoRedoType.Undo) undoList else redoList
        val targetList =
            if (type == UndoRedoType.Undo) redoList else undoList

        if (originList.size == 0) return null

        val operationBlock = originList.removeLast()
        val revertOperations = mutableListOf<Operation>()
        val zoneAffected = Rect()
        for (operation in operationBlock) {
            val (cancelOperation, thisZoneAffected) = treatOperation(operation = operation)
            revertOperations.add(cancelOperation)
            zoneAffected.union(thisZoneAffected)
        }
        targetList.add(revertOperations.reversed())

        // update the affected zone
        return zoneAffected
    }

    fun addOperationsToHistory(operations: OperationBlock) {
        undoList.add(operations)
        redoList.clear()
    }
}