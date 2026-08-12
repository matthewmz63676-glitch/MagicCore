package com.magicstudios.magiccore.modules.orders;

import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.delivery.DeliveryTransactionSupport;
import com.magicstudios.magiccore.delivery.MailboxDelivery;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport;
import com.magicstudios.magiccore.modules.shop.InventoryRemovalPort;
import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class PersistentOrderService implements OrderService {
    private final TransactionalDataStore store; private final CapabilityService capabilities;
    private final InventoryRemovalPort inventory; private final CurrencyDefinition currency; private final Clock clock;
    private final Duration minimumDuration, maximumDuration; private final long minimumUnitPrice, maximumUnitPrice;
    private final Set<String> categories;
    private final RecordRepository<BuyOrder> orders = new RecordRepository<>("orders.order", BuyOrder.class);
    private final RecordRepository<OrderFulfillment> fulfillments = new RecordRepository<>("orders.fulfillment", OrderFulfillment.class);

    public PersistentOrderService(TransactionalDataStore store, CapabilityService capabilities,
                                  InventoryRemovalPort inventory, CurrencyDefinition currency, Clock clock,
                                  Duration minimumDuration, Duration maximumDuration,
                                  long minimumUnitPrice, long maximumUnitPrice, Set<String> categories) {
        this.store=store;this.capabilities=capabilities;this.inventory=inventory;this.currency=currency;this.clock=clock;
        this.minimumDuration=minimumDuration;this.maximumDuration=maximumDuration;
        this.minimumUnitPrice=minimumUnitPrice;this.maximumUnitPrice=maximumUnitPrice;
        this.categories=categories.stream().map(PersistentOrderService::category).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override public CompletionStage<OrderMutation> create(UUID buyerId, String category, ItemFingerprint fingerprint,
                                                            int quantity, long unitPriceMinor, Duration duration, String operationKey) {
        String categoryId = category(category);
        if (!categories.contains(categoryId)) throw new IllegalArgumentException("UNKNOWN_CATEGORY");
        if (quantity < 1 || quantity > 100_000) throw new IllegalArgumentException("INVALID_QUANTITY");
        if (unitPriceMinor < minimumUnitPrice || unitPriceMinor > maximumUnitPrice) throw new IllegalArgumentException("INVALID_PRICE");
        if (duration.compareTo(minimumDuration)<0 || duration.compareTo(maximumDuration)>0) throw new IllegalArgumentException("INVALID_DURATION");
        long escrow = Math.multiplyExact(unitPriceMinor, quantity);
        return capabilities.limit(buyerId, "ORDER_SLOTS").thenCompose(limit -> store.transact("order-create:"+operationKey, tx -> {
            if (!IdempotencyKeys.reserve(tx,"order-create",operationKey)) return new OrderMutation(false,"REPLAY",null,null);
            long active=orders.scan(tx,null,1000).stream().map(RecordRepository.VersionedValue::value)
                    .filter(order->order.buyerId().equals(buyerId)&&order.status()==BuyOrder.Status.OPEN).count();
            if(active>=limit) throw new IllegalStateException("ORDER_SLOT_LIMIT_REACHED");
            EconomyTransactionSupport.credit(tx,buyerId,currency,-escrow,operationKey+":escrow",buyerId.toString(),"order-escrow",clock.instant());
            var now=clock.instant(); BuyOrder order=new BuyOrder(UUID.randomUUID(),buyerId,categoryId,fingerprint,quantity,0,0,
                    currency.id(),unitPriceMinor,escrow,BuyOrder.Status.OPEN,now,now.plus(duration),null);
            orders.put(tx,order.id().toString(),order,0); return new OrderMutation(true,"OPEN",order,null);
        }));
    }

    @Override public CompletionStage<OrderMutation> fulfill(UUID sellerId, UUID orderId, int quantity, String operationKey) {
        if(quantity<1) throw new IllegalArgumentException("INVALID_QUANTITY");
        return reserve(sellerId,orderId,quantity,operationKey).thenCompose(reserved->{
            if(!reserved.applied()) return CompletableFuture.completedFuture(reserved);
            return inventory.removeExact(sellerId,reserved.order().fingerprint(),quantity,operationKey+":remove")
                    .handle((receipt,failure)->new Outcome(receipt,failure)).thenCompose(outcome->{
                        if(outcome.failure()!=null) return reject(reserved.fulfillment(),"REMOVAL_FAILURE")
                                .thenCompose(ignored->CompletableFuture.failedFuture(outcome.failure()));
                        return outcome.receipt().removed()?settle(reserved.fulfillment(),outcome.receipt().recoveryPayloadBase64())
                                :reject(reserved.fulfillment(),outcome.receipt().code());
                    });
        });
    }

    private CompletionStage<OrderMutation> reserve(UUID sellerId,UUID orderId,int quantity,String operationKey){
        return store.transact("order-reserve:"+operationKey,tx->{
            var current=orders.get(tx,orderId.toString()).orElseThrow(()->new IllegalStateException("ORDER_NOT_FOUND"));
            if(!IdempotencyKeys.reserve(tx,"order-fill",operationKey)) return new OrderMutation(false,"REPLAY",current.value(),null);
            BuyOrder order=current.value(); if(order.buyerId().equals(sellerId)) throw new IllegalArgumentException("CANNOT_FILL_OWN_ORDER");
            if(!order.openAt(clock.instant())||quantity>order.availableQuantity()) throw new IllegalStateException("ORDER_UNAVAILABLE_QUANTITY");
            long payout=Math.multiplyExact(order.unitPriceMinor(),quantity); UUID fulfillmentId=UUID.randomUUID();
            BuyOrder reserved=copy(order,order.filledQuantity(),order.reservedQuantity()+quantity,order.escrowRemainingMinor(),BuyOrder.Status.OPEN,null);
            orders.put(tx,order.id().toString(),reserved,current.revision());
            OrderFulfillment fulfillment=new OrderFulfillment(fulfillmentId,order.id(),sellerId,quantity,payout,
                    OrderFulfillment.Status.PREPARING,"",operationKey,clock.instant(),null);
            fulfillments.put(tx,fulfillmentId.toString(),fulfillment,0);
            return new OrderMutation(true,"RESERVED",reserved,fulfillment);
        });
    }

    private CompletionStage<OrderMutation> settle(OrderFulfillment fulfillment,String payload){
        return store.transact("order-settle:"+fulfillment.operationKey(),tx->{
            var fill=fulfillments.get(tx,fulfillment.id().toString()).orElseThrow();
            if(fill.value().status()!=OrderFulfillment.Status.PREPARING) return new OrderMutation(false,"REPLAY",null,fill.value());
            var current=orders.get(tx,fulfillment.orderId().toString()).orElseThrow(); BuyOrder order=current.value();
            int filled=Math.addExact(order.filledQuantity(),fulfillment.quantity()); int reserved=order.reservedQuantity()-fulfillment.quantity();
            long escrow=order.escrowRemainingMinor()-fulfillment.payoutMinor();
            EconomyTransactionSupport.credit(tx,fulfillment.sellerId(),currency,fulfillment.payoutMinor(),
                    fulfillment.operationKey()+":payout","order-escrow","order-fulfillment",clock.instant());
            BuyOrder.Status status=filled==order.requestedQuantity()?BuyOrder.Status.FILLED:BuyOrder.Status.OPEN;
            BuyOrder updated=copy(order,filled,reserved,escrow,status,status==BuyOrder.Status.FILLED?clock.instant():null);
            orders.put(tx,order.id().toString(),updated,current.revision());
            OrderFulfillment settled=new OrderFulfillment(fulfillment.id(),fulfillment.orderId(),fulfillment.sellerId(),
                    fulfillment.quantity(),fulfillment.payoutMinor(),OrderFulfillment.Status.SETTLED,payload,
                    fulfillment.operationKey(),fulfillment.createdAt(),clock.instant());
            fulfillments.put(tx,settled.id().toString(),settled,fill.revision());
            DeliveryTransactionSupport.enqueue(tx,MailboxDelivery.pending(UUID.randomUUID(),order.buyerId(),
                    fulfillment.operationKey(),"magiccore/order-item-v1",java.util.Base64.getDecoder().decode(payload),clock.instant()));
            return new OrderMutation(true,status==BuyOrder.Status.FILLED?"FILLED":"PARTIAL",updated,settled);
        });
    }

    private CompletionStage<OrderMutation> reject(OrderFulfillment fulfillment,String reason){
        return store.transact("order-reject:"+fulfillment.operationKey(),tx->{
            var fill=fulfillments.get(tx,fulfillment.id().toString()).orElseThrow(); var current=orders.get(tx,fulfillment.orderId().toString()).orElseThrow();
            BuyOrder order=current.value(); BuyOrder released=copy(order,order.filledQuantity(),order.reservedQuantity()-fulfillment.quantity(),
                    order.escrowRemainingMinor(),order.status(),order.closedAt()); orders.put(tx,order.id().toString(),released,current.revision());
            OrderFulfillment rejected=new OrderFulfillment(fulfillment.id(),fulfillment.orderId(),fulfillment.sellerId(),
                    fulfillment.quantity(),fulfillment.payoutMinor(),OrderFulfillment.Status.REJECTED,"",fulfillment.operationKey(),fulfillment.createdAt(),clock.instant());
            fulfillments.put(tx,rejected.id().toString(),rejected,fill.revision()); return new OrderMutation(false,reason,released,rejected);
        });
    }

    @Override public CompletionStage<OrderMutation> cancel(UUID buyerId,UUID orderId,String operationKey){
        return close(buyerId,orderId,operationKey,BuyOrder.Status.CANCELLED);
    }
    @Override public CompletionStage<Integer> expire(String operationKey,int limit){
        return store.transact("order-expire:"+operationKey,tx->{
            if(!IdempotencyKeys.reserve(tx,"order-expire",operationKey)) return 0;int changed=0;
            for(var record:orders.scan(tx,null,1000)){BuyOrder order=record.value();if(changed>=limit)break;
                if(order.status()!=BuyOrder.Status.OPEN||order.expiresAt().isAfter(clock.instant())||order.reservedQuantity()!=0)continue;
                refund(tx,order,operationKey+":"+order.id());BuyOrder closed=copy(order,order.filledQuantity(),0,0,BuyOrder.Status.EXPIRED,clock.instant());
                orders.put(tx,order.id().toString(),closed,record.revision());changed++;}return changed;});
    }
    private CompletionStage<OrderMutation> close(UUID buyerId,UUID orderId,String operationKey,BuyOrder.Status status){
        return store.transact("order-close:"+operationKey,tx->{var current=orders.get(tx,orderId.toString()).orElseThrow(()->new IllegalStateException("ORDER_NOT_FOUND"));
            if(!IdempotencyKeys.reserve(tx,"order-close",operationKey))return new OrderMutation(false,"REPLAY",current.value(),null);
            BuyOrder order=current.value();if(!order.buyerId().equals(buyerId))throw new SecurityException("NOT_BUYER");
            if(order.status()!=BuyOrder.Status.OPEN||order.reservedQuantity()!=0)throw new IllegalStateException("ORDER_CANNOT_CLOSE");
            refund(tx,order,operationKey);BuyOrder closed=copy(order,order.filledQuantity(),0,0,status,clock.instant());orders.put(tx,order.id().toString(),closed,current.revision());
            return new OrderMutation(true,status.name(),closed,null);});}
    private void refund(com.magicstudios.magiccore.storage.DataTransaction tx,BuyOrder order,String operationKey)throws Exception{
        if(order.escrowRemainingMinor()>0)EconomyTransactionSupport.credit(tx,order.buyerId(),currency,order.escrowRemainingMinor(),operationKey+":refund","order-escrow","order-refund",clock.instant());}
    @Override public CompletionStage<List<BuyOrder>> open(String category,int limit){String id=category==null?"":category(category);return store.read(r->orders.scan(r,null,1000).stream().map(RecordRepository.VersionedValue::value)
            .filter(o->o.openAt(clock.instant())).filter(o->id.isEmpty()||o.category().equals(id)).sorted(Comparator.comparing(BuyOrder::unitPriceMinor).reversed()).limit(limit).toList());}
    @Override public CompletionStage<List<BuyOrder>> history(UUID playerId,int limit){return store.read(r->orders.scan(r,null,1000).stream().map(RecordRepository.VersionedValue::value)
            .filter(o->o.buyerId().equals(playerId)).sorted(Comparator.comparing(BuyOrder::createdAt).reversed()).limit(limit).toList());}
    private static BuyOrder copy(BuyOrder o,int filled,int reserved,long escrow,BuyOrder.Status status,java.time.Instant closed){return new BuyOrder(o.id(),o.buyerId(),o.category(),o.fingerprint(),o.requestedQuantity(),filled,reserved,o.currency(),o.unitPriceMinor(),escrow,status,o.createdAt(),o.expiresAt(),closed);}
    private static String category(String value){if(value==null||!value.matches("[A-Za-z0-9_-]{1,24}"))throw new IllegalArgumentException("INVALID_CATEGORY");return value.toLowerCase(Locale.ROOT);}
    private record Outcome(InventoryRemovalPort.RemovalReceipt receipt,Throwable failure){}
}
