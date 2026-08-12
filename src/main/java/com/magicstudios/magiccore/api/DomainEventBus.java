package com.magicstudios.magiccore.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class DomainEventBus {
    private final Map<Class<?>, List<Subscription<?>>> subscriptions = new ConcurrentHashMap<>();

    public <T extends DomainEvent> void subscribe(String owner, Class<T> eventType, Consumer<T> consumer) {
        subscriptions.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>())
                .add(new Subscription<>(Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(consumer, "consumer")));
    }

    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event");
        subscriptions.getOrDefault(event.getClass(), List.of()).forEach(subscription -> subscription.accept(event));
    }

    public void unsubscribeOwner(String owner) {
        subscriptions.values().forEach(list -> list.removeIf(subscription -> subscription.owner().equals(owner)));
    }

    public int subscriptionCount() {
        return subscriptions.values().stream().mapToInt(List::size).sum();
    }

    private record Subscription<T extends DomainEvent>(String owner, Consumer<T> consumer) {
        @SuppressWarnings("unchecked")
        private void accept(DomainEvent event) {
            consumer.accept((T) event);
        }
    }
}
