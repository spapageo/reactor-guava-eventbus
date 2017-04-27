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

import com.github.spapageo.reactor.guava.eventbus.ReactorEventBusHelper;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.test.StepVerifier;
import reactor.test.StepVerifierOptions;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;

public class IntegrationTest {

    private EventBus eventBus = new EventBus();

    private ReactorEventBusHelper reactorEventBusHelper = new ReactorEventBusHelper(eventBus);

    @Test
    public void listenFor_correctlyPropagates2PostedEvents() throws Exception {
        Flux<Integer> stringFlux = reactorEventBusHelper.listenFor(Integer.class);

        StepVerifier.create(stringFlux).then(() -> {
            eventBus.post(0);
            eventBus.post(1);
        }).expectNext(0, 1).thenCancel().verify();
    }

    @Test
    public void listenFor_whenGivenEventSubType_correctlyPropagates2PostedEvents() throws Exception {
        Flux<Object> stringFlux = reactorEventBusHelper.listenFor(Object.class);

        StepVerifier.create(stringFlux).then(() -> {
            eventBus.post(0);
            eventBus.post(1);
        }).expectNext(0, 1).thenCancel().verify();
    }

    @Test
    public void listenFor_whenGivenDeadEventType_correctlyPropagates2PostedEvents() throws Exception {
        Flux<DeadEvent> stringFlux = reactorEventBusHelper.listenFor(DeadEvent.class);

        StepVerifier.create(stringFlux)
                .then(() -> {
                    eventBus.post(0);
                    eventBus.post(1);
                })
                .expectNextMatches(deadEvent -> deadEvent.getEvent().equals(0))
                .expectNextMatches(deadEvent -> deadEvent.getEvent().equals(1))
                .thenCancel()
                .verify();
    }

    @Test
    public void listenFor_whenOverflowStrategyLatest_correctlyPropagatesLatestPostedEvents() throws Exception {
        Flux<Integer> stringFlux = reactorEventBusHelper.listenFor(Integer.class, FluxSink.OverflowStrategy.LATEST);

        StepVerifierOptions options = StepVerifierOptions.create().initialRequest(0).virtualTimeSchedulerSupplier(
                VirtualTimeScheduler::getOrSet
        );

        StepVerifier.create(stringFlux, options)
                .expectSubscription()
                .then(() -> {
                    eventBus.post(0);
                    eventBus.post(1);
                })
                .thenAwait(Duration.ofMillis(10000))
                .thenRequest(1)
                .expectNext(1)
                .thenCancel()
                .verify();
    }
}
