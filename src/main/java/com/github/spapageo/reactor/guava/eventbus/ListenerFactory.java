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

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.This;
import reactor.core.publisher.FluxSink;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uses bytecode generation to dynamically create sub-classes of {@link ReactorCacheListener} with a method properly
 * annotated with {@link Subscribe} and {@link AllowConcurrentEvents}
 */
class ListenerFactory {
    private static final ImmutableList<Annotation> LISTENER_ANNOTATIONS
            = ImmutableList.of(new SubscribeImpl(), new AllowConcurrentEventsImpl());

    private final Map<Class<?>, Class<? extends ReactorCacheListener>> eventClassToListener =
            new ConcurrentHashMap<>();

    /**
     * Instantiates a {@link ReactorCacheListener} giving {@link FluxSink} as its single argument
     * @param fluxSink the only argument the invoked constructor
     * @param listenerClass the class that will be instantiated
     * @return the {@link ReactorCacheListener} instance
     */
    <T> ReactorCacheListener<T> instantiatedListener(FluxSink<T> fluxSink,
                                              Class<? extends ReactorCacheListener<T>> listenerClass) {
        try {
            return listenerClass.getConstructor(FluxSink.class).newInstance(fluxSink);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Unable to instantiate a listener for the given argument type", e);
        }
    }

    /**
     * Dynamically generates a sub-class of {@link ReactorCacheListener} with a method named 'listenGenerated' that has
     * a single parameter of type {@code eventClass}.
     * @param eventClass the type of event that should be handled by the generated {@link ReactorCacheListener}
     * @return the generated {@link ReactorCacheListener}
     */
    @SuppressWarnings("unchecked")
    <T> Class<? extends ReactorCacheListener<T>> generateListenerClass(Class<T> eventClass) {
        return (Class<? extends ReactorCacheListener<T>>) eventClassToListener.computeIfAbsent(eventClass, eventClazz ->
                new ByteBuddy().subclass(ReactorCacheListener.class)
                        .defineMethod("listenGenerated", void.class).withParameter(eventClazz)
                        .intercept(MethodDelegation.to(ListenerFactory.class))
                        .annotateMethod(LISTENER_ANNOTATIONS)
                        .make()
                        .load(ClassLoader.getSystemClassLoader())
                        .getLoaded());

    }

    @SuppressWarnings("unused")
    static <T> void listenGenerated(@This ReactorCacheListener<T> reactorCacheListener, @Argument(0) T event) {
        reactorCacheListener.listen(event);
    }

    @SuppressWarnings("ClassExplicitlyAnnotation")
    private static class AllowConcurrentEventsImpl implements AllowConcurrentEvents {
        @Override
        public Class<? extends Annotation> annotationType() {
            return AllowConcurrentEvents.class;
        }
    }

    @SuppressWarnings("ClassExplicitlyAnnotation")
    private static class SubscribeImpl implements Subscribe {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Subscribe.class;
        }
    }
}
