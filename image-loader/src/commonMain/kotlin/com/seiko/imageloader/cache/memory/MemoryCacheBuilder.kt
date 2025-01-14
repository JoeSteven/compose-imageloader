package com.seiko.imageloader.cache.memory

expect class MemoryCacheBuilder : CommonMemoryCacheBuilder<MemoryCacheBuilder>

abstract class CommonMemoryCacheBuilder<B : CommonMemoryCacheBuilder<B>> {

    protected abstract var maxSizePercent: Double
    private var maxSizeBytes = 0
    private var strongReferencesEnabled = true
    private var weakReferencesEnabled = true

    abstract fun calculateMemoryCacheSize(percent: Double): Int

    /**
     * Set the maximum size of the memory cache as a percentage of this application's
     * available memory.
     */
    fun maxSizePercent(percent: Double) = apply {
        require(percent in 0.0..1.0) { "size must be in the range [0.0, 1.0]." }
        this.maxSizeBytes = 0
        this.maxSizePercent = percent
    }

    /**
     * Set the maximum size of the memory cache in bytes.
     */
    fun maxSizeBytes(size: Int) = apply {
        require(size >= 0) { "size must be >= 0." }
        this.maxSizePercent = 0.0
        this.maxSizeBytes = size
    }

    /**
     * Enables/disables strong reference tracking of values added to this memory cache.
     */
    fun strongReferencesEnabled(enable: Boolean) = apply {
        this.strongReferencesEnabled = enable
    }

    /**
     * Enables/disables weak reference tracking of values added to this memory cache.
     * Weak references do not contribute to the current size of the memory cache.
     * This ensures that if a [Image] hasn't been garbage collected yet it will be
     * returned from the memory cache.
     */
    fun weakReferencesEnabled(enable: Boolean) = apply {
        this.weakReferencesEnabled = enable
    }

    /**
     * Create a new [MemoryCache] instance.
     */
    fun build(): MemoryCache {
        val weakMemoryCache = if (weakReferencesEnabled) {
            RealWeakMemoryCache()
        } else {
            EmptyWeakMemoryCache
        }
        val strongMemoryCache = if (strongReferencesEnabled) {
            val maxSize = if (maxSizePercent > 0) {
                calculateMemoryCacheSize(maxSizePercent)
            } else {
                maxSizeBytes
            }
            if (maxSize > 0) {
                RealStrongMemoryCache(maxSize, weakMemoryCache)
            } else {
                EmptyStrongMemoryCache(weakMemoryCache)
            }
        } else {
            EmptyStrongMemoryCache(weakMemoryCache)
        }
        return RealMemoryCache(strongMemoryCache, weakMemoryCache)
    }
}

internal const val STANDARD_MEMORY_MULTIPLIER = 0.2
internal const val LOW_MEMORY_MULTIPLIER = 0.15
internal const val DEFAULT_MEMORY_CLASS_MEGABYTES = 256
