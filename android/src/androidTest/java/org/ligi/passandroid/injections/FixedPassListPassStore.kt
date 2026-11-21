package org.ligi.passandroid.injections

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.ligi.passandroid.model.PassArchive
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.PassStoreUpdateEvent
import org.ligi.passandroid.model.pass.Pass
import java.io.File

class FixedPassListPassStore(private var passes: List<Pass>) : PassStore {

    override lateinit var archive: PassArchive

    init {
        archive = PassArchive(HashMap(), this)
    }

    fun setList(newPasses: List<Pass>, newCurrentPass: Pass? = newPasses.firstOrNull()) {
        currentPass = newCurrentPass
        passes = newPasses
        passMap.clear()
        passMap.putAll(createHashMap())

        archive = PassArchive(HashMap(), this)
    }

    override var currentPass: Pass? = null

    private val passMap: HashMap<String, Pass> by lazy {
        return@lazy createHashMap()
    }

    private fun createHashMap(): HashMap<String, Pass> {
        val hashMap = HashMap<String, Pass>()

        passes.forEach { hashMap[it.id] = it }
        return hashMap
    }

    override fun getPassbookForId(id: String): Pass? {
        return passMap[id]
    }


    override fun reload(id: String): Pass? = passMap[id]

    override fun reloadAll() = Unit

    override fun deletePassWithId(id: String): Boolean {
        return false
    }

    override fun getPathForID(id: String): File {
        return File("")
    }

    override val updateChannel: SharedFlow<PassStoreUpdateEvent> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun save(pass: Pass) {
        // no effect in this impl
    }

    override fun syncArchiveWithStorage() {
        // no effect in this impl
    }

}
