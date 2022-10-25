package digital.capsa.it.aggregate

import digital.capsa.core.logger
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

@DslMarker
annotation class AggregateMarker

data class Key(var value: String)

@AggregateMarker
abstract class Aggregate(private val aggregateName: String) {

    enum class Pass {
        First, Second
    }

    var key: Key? = null

    var id: UUID? = null

    var parent: Aggregate? = null

    val children = arrayListOf<Aggregate>()

    var randomSeed: Long = 0

    abstract fun onConstruct()

    abstract fun onCreate(pass: Pass)

    protected fun <T : Aggregate> addAggregate(aggregate: T, init: T.() -> Unit): T {
        aggregate.parent = this
        children.add(aggregate)
        aggregate.randomSeed = Random(aggregate.getPath().hashCode().toLong()).nextLong()
        aggregate.onConstruct()
        aggregate.init()
        return aggregate
    }

    open fun create(pass: Pass = Pass.First) {
        if (pass == Pass.First && parent == null) {
            logger.info("Aggregate Tree:\n $this")
        }
        onCreate(pass)
        for (c in children) {
            c.create(pass)
        }
    }

    private fun toString(builder: StringBuilder, nesting: Int) {
        builder.append("".padStart(nesting * 4) + "$aggregateName${key?.let { " key=${it.value}" } ?: ""} ${getAttributes()}\n")
        for (c in children) {
            c.toString(builder, nesting + 1)
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        toString(builder, 0)
        return builder.toString()
    }

    open fun getPath(): String {
        return (parent?.getPath() ?: "") + "/${aggregateName}" + (parent?.let { "(${it.getChildIndex(this)})" } ?: "")
    }

    open fun getAttributes(): String {
        return this::class.declaredMemberProperties.filter { it.visibility == KVisibility.PUBLIC }
            .joinToString(prefix = "[", postfix = "]") { "${it.name}=${it.getter.call(this)}" }
    }

    inline fun <reified T> getChild(i: Int): T {
        return children.filter { it is T }[i] as T
    }

    inline fun <reified T> getChild(key: Key): T {
        return children.filter { it is T && key == it.key }[0] as T
    }

    inline fun <reified T> getAllChildren(): Set<T> {
        return children.filter { it is T }.toSet() as Set<T>
    }

    open fun getChildIndex(aggregate: Aggregate): Int {
        return children.subList(0, children.indexOf(aggregate)).count { aggregate::class.isInstance(it) }
    }

    open fun getChildCount(kClass: KClass<out Aggregate>): Int {
        return children.count { kClass.isInstance(it) }
    }

    open fun getDescendantCount(kClass: KClass<out Aggregate>): Int {
        fun count(aggregate: Aggregate, kClass: KClass<out Aggregate>): Int {
            return getChildCount(kClass) + aggregate.children.sumOf { it.getDescendantCount(kClass) }
        }
        return count(this, kClass)
    }

    inline fun <reified T : Aggregate> getDescendant(key: Key): T {
        return getRecursiveDescendant(aggregate = this, kClass = T::class, key = key)
            ?: throw Error("Descendant with key = ${key.value} not found")
    }

    fun <T : Aggregate> getRecursiveDescendant(aggregate: Aggregate, kClass: KClass<out T>, key: Key): T? {
        val children = aggregate.children.filter { it::class == kClass && key == it.key }
        return if (children.isNotEmpty()) {
            children[0] as T
        } else {
            var child: T? = null
            for (item in aggregate.children) {
                child = getRecursiveDescendant(aggregate = item, kClass = kClass, key = key)
                if (child != null) {
                    break
                }
            }
            child
        }
    }

    inline fun <reified T : Aggregate> getAncestor(): T {
        return getRecursiveAncestor(this, T::class)
    }

    fun <T : Aggregate> getRecursiveAncestor(aggregate: Aggregate, kClass: KClass<out T>): T {
        return if (aggregate.parent == null) {
            throw Error("Ancestor with class = $kClass not found")
        } else if (aggregate.parent!!::class == kClass) {
            aggregate.parent!! as T
        } else {
            getRecursiveAncestor(aggregate.parent!!, kClass)
        }
    }

    operator fun Key.unaryPlus() {
        key = this
    }

    fun nextRandomInt(seed: Int, bound: Int): Int {
        return Random(31 * randomSeed + seed).nextInt(bound)
    }
}
