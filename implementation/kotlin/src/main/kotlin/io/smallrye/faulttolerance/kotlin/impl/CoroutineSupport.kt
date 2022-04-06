package io.smallrye.faulttolerance.kotlin.impl

import io.smallrye.faulttolerance.core.invocation.AsyncSupport
import io.smallrye.faulttolerance.core.invocation.Invoker
import io.smallrye.faulttolerance.core.util.CompletionStages.failedFuture
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutionException
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// the `Any?` type is used here similarly to Kotlin-transformed `suspend` functions:
// it stands for a non-denotable union type `T | COROUTINE_SUSPENDED`
class CoroutineSupport<T> : AsyncSupport<T, Any?> {
    override fun mustDescription(): String {
        return "be a Kotlin suspend function"
    }

    override fun doesDescription(): String {
        return "is a Kotlin suspend function"
    }

    override fun applies(parameterTypes: Array<Class<*>>, returnType: Class<*>): Boolean {
        return parameterTypes.isNotEmpty() && parameterTypes.last() == Continuation::class.java
    }

    override fun toCompletionStage(invoker: Invoker<Any?>): CompletionStage<T> {
        val future = CompletableFuture<T>()

        val index = invoker.parametersCount() - 1
        val previousContinuation = invoker.replaceArgument(index, Continuation::class.java) { originalContinuation ->
            object : Continuation<T> {
                override val context: CoroutineContext
                    get() = originalContinuation.context

                override fun resumeWith(result: Result<T>) {
                    result.fold(
                            onSuccess = { value -> future.complete(value) },
                            onFailure = { exception -> future.completeExceptionally(exception) }
                    )
                }
            }
        }

        try {
            val result = invoker.proceed()
            if (result !== COROUTINE_SUSPENDED) {
                return completedFuture(result as T)
            }
        } catch (e: Exception) {
            return failedFuture(e)
        } finally {
            invoker.replaceArgument(index, Continuation::class.java) { previousContinuation }
        }

        return future
    }

    override fun fromCompletionStage(invoker: Invoker<CompletionStage<T>>): Any? {
        // remember the continuation early, though there should be no harm in looking it up later
        val index = invoker.parametersCount() - 1
        val continuation = invoker.getArgument(index, Continuation::class.java) as Continuation<T>

        val completableFuture = invoker.proceed().toCompletableFuture()

        if (completableFuture.isDone) {
            try {
                return completableFuture.get()
            } catch (e: ExecutionException) {
                throw e.cause!!
            }
        }

        completableFuture.whenComplete { value, exception ->
            if (exception == null) {
                continuation.resume(value)
            } else {
                continuation.resumeWithException(exception)
            }
        }
        return COROUTINE_SUSPENDED
    }

    // ---

    override fun fallbackResultToCompletionStage(value: Any?): CompletionStage<T> {
        if (value === COROUTINE_SUSPENDED) {
            // should never happen
            throw FaultToleranceException("Unexpected $value")
        } else {
            return completedFuture(value as T)
        }
    }
}
