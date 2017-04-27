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

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.FluxSink;

import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ListenerFactoryTest {

    private static final String EVENT = "event";

    @Mock
    private FluxSink<String> fluxSink;

    private ListenerFactory listenerFactory = new ListenerFactory();

    @Test
    public void generateListenerClass_whenGivenStringEventType_generatesListenerWithAnnotatedMethod()
            throws Exception {
        Class<? extends ReactorCacheListener> listenerClass = listenerFactory.generateListenerClass(String.class);

        Method listenGenerated = listenerClass.getDeclaredMethod("listenGenerated", String.class);
        Subscribe subscribeAnnotation = listenGenerated.getAnnotation(Subscribe.class);
        AllowConcurrentEvents allowConcurrentEventsAnnotation = listenGenerated.getAnnotation(AllowConcurrentEvents.class);

        assertNotNull(subscribeAnnotation);
        assertNotNull(allowConcurrentEventsAnnotation);
    }

    @Test
    public void instantiateListener_generatesMethodThatInvokesListen()
            throws Exception {
        Class<? extends ReactorCacheListener<String>> listenerClass = listenerFactory.generateListenerClass(String.class);
        ReactorCacheListener<String> reactorCacheListener = listenerFactory.instantiatedListener(fluxSink, listenerClass);

        listenerClass.getDeclaredMethod("listenGenerated", String.class).invoke(reactorCacheListener, EVENT);

        verify(fluxSink).next(EVENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void instantiateListener_whenItCanNotInstantiateAListenerClass_throwsIllegalArgumentException()
            throws Exception {
        listenerFactory.instantiatedListener(fluxSink, TestListener.class);
    }

}