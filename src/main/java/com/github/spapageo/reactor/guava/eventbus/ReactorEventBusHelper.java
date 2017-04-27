/*
 * The MIT License (MIT)
 * Copyright (c) 2017 Spyridon Papageorgiou
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.spapageo.reactor.guava.eventbus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink.OverflowStrategy;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Enables creating {@link Flux} from the stream of events that are propagated by the {@link EventBus}
 */
public final class ReactorEventBusHelper {
    private final EventBus eventBus;
    private final ListenerFactory listenerFactory;

    public ReactorEventBusHelper(EventBus eventBus) {
        this(eventBus, new ListenerFactory());
    }

    @VisibleForTesting
    ReactorEventBusHelper(EventBus eventBus, ListenerFactory listenerFactory) {
        this.eventBus = checkNotNull(eventBus);
        this.listenerFactory = checkNotNull(listenerFactory);
    }


    /**
     * @see #listenFor(Class, OverflowStrategy)
     * @param eventClass the class of the events that will be propagated though the returned flux
     * @param <T>        the type of the event
     * @return the flux of events
     */
    public <T> Flux<T> listenFor(Class<T> eventClass) {
        return listenFor(eventClass, OverflowStrategy.BUFFER);
    }

    /**
     * <p>
     * Registers a new listener with the {@link EventBus} of type {@code eventClass}. A copy of all events of this
     * types are propagated through the flux.
     * </p>
     * <p>
     *
     * </p>
     *
     * @param eventClass       the class of the events that will be propagated though the returned flux
     * @param overflowStrategy the overflow strategy for the created flux
     * @param <T>              the type of the event
     * @return the flux of events
     */
    public <T> Flux<T> listenFor(Class<T> eventClass, OverflowStrategy overflowStrategy) {
        checkNotNull(eventClass);
        checkNotNull(overflowStrategy);
        checkArgument(!eventClass.isPrimitive(), "Primitive event types are not supported by guava EventBus");

        Class<? extends ReactorCacheListener<T>> generatedListenerClass = listenerFactory.generateListenerClass(eventClass);

        return Flux.create(fluxSink -> {
            ReactorCacheListener listener = listenerFactory.instantiatedListener(fluxSink, generatedListenerClass);
            eventBus.register(listener);

            fluxSink.onDispose(() -> eventBus.unregister(listener));
        }, overflowStrategy);
    }

}