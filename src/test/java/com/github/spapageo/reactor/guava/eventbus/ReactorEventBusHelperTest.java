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

import com.google.common.eventbus.EventBus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.FluxSink;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReactorEventBusHelperTest {

    @Mock
    private EventBus eventBus;

    @Mock
    private ListenerFactory listenerFactory;

    @InjectMocks
    private ReactorEventBusHelper reactorEventBusHelper;

    @Test(expected = IllegalArgumentException.class)
    public void listenFor_whenGivenPrimitiveClass_throwsIllegalArgumentException() throws Exception {
        reactorEventBusHelper.listenFor(int.class);
    }

    @Test(expected = NullPointerException.class)
    public void listenFor_whenGivenNullClass_throwsIllegalArgumentException() throws Exception {
        reactorEventBusHelper.listenFor(null);
    }

    @Test(expected = NullPointerException.class)
    public void listenFor_whenGivenNullBackPressureStrategy_throwsIllegalArgumentException() throws Exception {
        reactorEventBusHelper.listenFor(String.class, null);
    }

    @Test
    public void listenFor_whenFluxIsNotSubscribed_createsListenerClassButDoesNotRegisterListener() throws Exception {
        Mockito.<Class<? extends ReactorCacheListener<String>>>when(listenerFactory.generateListenerClass(String.class))
                .thenReturn(TestListener.class);

        reactorEventBusHelper.listenFor(String.class);

        verify(listenerFactory).generateListenerClass(String.class);
        verify(eventBus, times(0)).register(any(Object.class));
    }

    @Test
    public void listenFor_whenFluxIsSubscribed_createsListenerClassAndRegisterListener() throws Exception {
        Mockito.<Class<? extends ReactorCacheListener<String>>>when(listenerFactory.generateListenerClass(String.class))
                .thenReturn(TestListener.class);

        ReactorCacheListener<String> listener = Mockito.mock(TestListener.class);
        when(listenerFactory.instantiatedListener(any(), eq(TestListener.class))).thenReturn(listener);

        StepVerifier.create(reactorEventBusHelper.listenFor(String.class)).expectSubscription().thenCancel().verify();

        verify(listenerFactory).generateListenerClass(String.class);
        verify(eventBus).register(listener);
    }

}