package org.ligi.passandroid.injections

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.ligi.passandroid.model.PassArchive
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.PassStoreChange
import org.ligi.passandroid.model.pass.Pass
import java.io.File

class FixedPassListPassStore(private var passes: List<Pass>) : PassStore {

    override lateinit var archive: PassArchive

    init {
        archive = PassArchive(HashMap(), this)
    }

    fun setList(newPasses: List<Pass>) {
        passes = newPasses
        passMap.clear()
        passMap.putAll(createHashMap())

        archive = PassArchive(HashMap(), this)
    }

    private val passMap: HashMap<String, Pass> by lazy {
        return@lazy createHashMap()
    }

    private fun createHashMap(): HashMap<String, Pass> {
        val hashMap = HashMap<String, Pass>()

        passes.forEach { hashMap[it.id] = it }
        return hashMap
    }

    override fun getPass(id: String): Pass? {
        return passMap[id]
    }


    override fun reload(id: String): Pass? = passMap[id]

    override fun reloadAll() = Unit

    override fun delete(id: String): Boolean {
        return false
    }

    override val directory: File = File("")

    override fun directoryOf(id: String): File {
        return File("")
    }

    override val changes: SharedFlow<PassStoreChange> = MutableSharedFlow(
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
