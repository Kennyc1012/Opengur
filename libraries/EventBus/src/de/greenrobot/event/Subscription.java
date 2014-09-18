/*
 * Copyright (C) 2012 Markus Junginger, greenrobot (http://greenrobot.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.greenrobot.event;

import java.lang.ref.WeakReference;

final class Subscription {
    private final WeakReference<Object> subscriber;

    final SubscriberMethod subscriberMethod;

    final int priority;

    /**
     * Becomes false as soon as {@link EventBus#unregister(Object)} is called, which is checked by queued event delivery
     * {@link EventBus#invokeSubscriber(PendingPost)} to prevent race conditions.
     */
    volatile boolean active;

    Subscription(Object subscriber, SubscriberMethod subscriberMethod, int priority) {
        this.subscriber = new WeakReference<Object>(subscriber);
        this.subscriberMethod = subscriberMethod;
        this.priority = priority;
        active = true;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Subscription))
            return false;

        Subscription otherSubscription = (Subscription) other;
        Object mySubscriber = subscriber.get();
        Object otherSubscriber = otherSubscription.subscriber.get();

        if (mySubscriber == null || otherSubscriber == null)
            return false;

        return mySubscriber == otherSubscriber
                && subscriberMethod.equals(otherSubscription.subscriberMethod);
    }


    @Override
    public int hashCode() {
        int hashCode = subscriberMethod.methodString.hashCode();
        Object mySubscriber = subscriber.get();

        if (mySubscriber != null)
            hashCode += mySubscriber.hashCode();

        return hashCode;
    }

    public Object getSubscriber() {
        return subscriber.get();
    }
}