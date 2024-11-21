package io.smallrye.faulttolerance.kotlin.impl

import io.smallrye.faulttolerance.core.Completer
import io.smallrye.faulttolerance.core.Future
import io.smallrye.faulttolerance.core.invocation.AsyncSupport
import io.smallrye.faulttolerance.core.invocation.ConstantInvoker
import io.smallrye.faulttolerance.core.invocation.Invoker
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

    override fun createComplete(value: T): Any? {
        return value
    }

    override fun toFuture(invoker: Invoker<Any?>): Future<T> {
        if (invoker is ConstantInvoker) {
            // special case because `ConstantInvoker` throws on all other methods
            return Future.of(invoker.proceed() as T)
        }

        val completer = Completer.create<T>()

        val index = invoker.parametersCount() - 1
        val previousContinuation = invoker.replaceArgument(index, Continuation::class.java) { originalContinuation ->
            object : Continuation<T> {
                override val context: CoroutineContext
                    get() = originalContinuation.context

                override fun resumeWith(result: Result<T>) {
                    result.fold(
                            onSuccess = { value -> completer.complete(value) },
                            onFailure = { exception -> completer.completeWithError(exception) }
                    )
                }
            }
        }

        try {
            val result = invoker.proceed()
            if (result !== COROUTINE_SUSPENDED) {
                completer.complete(result as T)
            }
        } catch (e: Exception) {
            completer.completeWithError(e)
        } finally {
            invoker.replaceArgument(index, Continuation::class.java) { previousContinuation }
        }

        return completer.future()
    }

    override fun fromFuture(invoker: Invoker<Future<T>>): Any? {
        // remember the continuation early, though there should be no harm in looking it up later
        val index = invoker.parametersCount() - 1
        val continuation = invoker.getArgument(index, Continuation::class.java) as Continuation<T>

        val future = invoker.proceed()

        if (future.isComplete || future.isCancelled) {
            return future.awaitBlocking()
        }

        future.then { value, error ->
            if (error == null) {
                continuation.resume(value)
            } else {
                continuation.resumeWithException(error)
            }
        }
        return COROUTINE_SUSPENDED
    }
}
