/*
 * Copyright 2020 Florian Spieß
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gay.solonovamax.jda.ktx

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import net.dv8tion.jda.api.requests.restaction.pagination.PaginationAction
import java.util.*
import java.util.concurrent.CancellationException

/**
 * Converts this PaginationAction to a [Flow]
 *
 * This is the same as
 * ```kotlin
 * flow {
 *   emitAll(produce())
 * }
 * ```
 *
 * @param[scope] The [CoroutineScope] to use (default: [GlobalScope])
 *
 * @return[Flow] instance
 */
@ExperimentalCoroutinesApi
fun <T, M: PaginationAction<T, M>> M.asFlow(scope: CoroutineScope = GlobalScope): Flow<T> = flow {
    this.emitAll(produce(scope))
}

/**
 * Converts this PaginationAction to a [ReceiveChannel][kotlinx.coroutines.channels.ReceiveChannel]
 *
 * @param[scope] The [CoroutineScope] to use (default: [GlobalScope])
 *
 * @return [ReceiveChannel][kotlinx.coroutines.channels.ReceiveChannel] instance
 */
@ExperimentalCoroutinesApi
fun <T, M: PaginationAction<T, M>> M.produce(scope: CoroutineScope = GlobalScope) = scope.produce<T> {
    cache(false)
    val queue = LinkedList<T>()
    try {
        while (!isClosedForSend) {
            if (queue.isEmpty())
                queue.addAll(await())
            if (queue.isEmpty()) {
                close()
                break
            }

            while (!isClosedForSend && queue.isNotEmpty()) {
                send(queue.poll())
            }
        }
    } catch (ignored: CancellationException) {}
}