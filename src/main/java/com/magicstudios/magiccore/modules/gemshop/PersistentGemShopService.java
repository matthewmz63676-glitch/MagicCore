package com.magicstudios.magiccore.modules.gemshop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.delivery.DeliveryTransactionSupport;
import com.magicstudios.magiccore.delivery.MailboxDelivery;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport;
import com.magicstudios.magiccore.modules.shop.InternalShopService;
import com.magicstudios.magiccore.modules.statistics.PlayerStatsService;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class PersistentGemShopService implements GemShopService {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private final TransactionalDataStore store; private final CurrencyDefinition currency; private final CapabilityService capabilities;
    private final PlayerStatsService stats; private final Clock clock; private final Duration confirmationTtl; private final Map<String,GemProduct> products;
    private final RecordRepository<GemShopQuote> quotes = new RecordRepository<>("gemshop.quote", GemShopQuote.class);
    private final RecordRepository<GemShopReceipt> receipts = new RecordRepository<>("gemshop.receipt", GemShopReceipt.class);

    public PersistentGemShopService(TransactionalDataStore store, CurrencyDefinition currency, CapabilityService capabilities,
                                    PlayerStatsService stats, Clock clock, Duration confirmationTtl, Collection<GemProduct> products) {
        this.store=store;this.currency=currency;this.capabilities=capabilities;this.stats=stats;this.clock=clock;this.confirmationTtl=confirmationTtl;
        LinkedHashMap<String,GemProduct> indexed=new LinkedHashMap<>();for(GemProduct product:products)if(indexed.putIfAbsent(product.id(),product)!=null)throw new IllegalArgumentException("Duplicate GemShop product "+product.id());this.products=Map.copyOf(indexed);
    }
    @Override public String currency(){return currency.id();}
    @Override public List<GemProduct>products(){return products.values().stream().sorted(Comparator.comparing(GemProduct::category).thenComparing(GemProduct::id)).toList();}
    @Override public List<GemProduct>products(String category){return products().stream().filter(value->value.category().equalsIgnoreCase(category)).toList();}
    @Override public CompletionStage<GemShopQuote>quote(UUID playerId,String productId,String operationKey){GemProduct product=require(productId);var statFuture=stats.stats(playerId).toCompletableFuture();CompletableFuture<Boolean>capabilityFuture=product.requiredCapability()==null||product.requiredCapability().isBlank()?CompletableFuture.completedFuture(true):capabilities.has(playerId,product.requiredCapability()).toCompletableFuture();return CompletableFuture.allOf(statFuture,capabilityFuture).thenCompose(ignored->{var value=statFuture.join();if(!capabilityFuture.join()||value.playtimeSeconds()<product.minimumPlaytimeSeconds()||value.kills()<product.minimumKills())return CompletableFuture.failedFuture(new IllegalStateException("GEMSHOP_PREREQUISITE_NOT_MET"));return store.transact("gemshop-quote:"+operationKey,tx->{if(!IdempotencyKeys.reserve(tx,"gemshop-quote",operationKey))throw new IllegalStateException("DUPLICATE_GEMSHOP_QUOTE");var now=clock.instant();GemShopQuote quote=new GemShopQuote(UUID.randomUUID(),playerId,product,GemShopQuote.Status.QUOTED,now,now.plus(confirmationTtl));quotes.put(tx,quote.id().toString(),quote,0);return quote;});});}
    @Override public CompletionStage<GemShopReceipt>confirm(UUID playerId,UUID quoteId,String operationKey){return store.transact("gemshop-confirm:"+operationKey,tx->{var existingReceipt=receipts.get(tx,quoteId.toString());if(existingReceipt.isPresent())return existingReceipt.get().value();if(!IdempotencyKeys.reserve(tx,"gemshop-confirm",operationKey))throw new IllegalStateException("DUPLICATE_GEMSHOP_CONFIRMATION");var current=quotes.get(tx,quoteId.toString()).orElseThrow(()->new IllegalArgumentException("GEMSHOP_QUOTE_NOT_FOUND"));GemShopQuote quote=current.value();if(!quote.playerId().equals(playerId))throw new SecurityException("GEMSHOP_QUOTE_OWNER_MISMATCH");if(quote.status()!=GemShopQuote.Status.QUOTED||!quote.expiresAt().isAfter(clock.instant()))throw new IllegalStateException("GEMSHOP_QUOTE_EXPIRED");var applied=EconomyTransactionSupport.credit(tx,playerId,currency,-quote.product().priceMinor(),operationKey,playerId.toString(),"gemshop:"+quote.product().id(),clock.instant());UUID deliveryId=UUID.randomUUID();var payload=new InternalShopService.PurchasePayload(quote.product().id(),quote.product().material(),quote.product().amount(),quote.product().itemDataBase64());DeliveryTransactionSupport.enqueue(tx,MailboxDelivery.pending(deliveryId,playerId,operationKey,"magiccore/shop-purchase-v1",JSON.writeValueAsBytes(payload),clock.instant()));GemShopReceipt receipt=new GemShopReceipt(UUID.randomUUID(),quote.id(),playerId,quote.product().id(),quote.product().priceMinor(),applied.afterMinor(),applied.transaction().id(),deliveryId,clock.instant());receipts.put(tx,quote.id().toString(),receipt,0);quotes.put(tx,quote.id().toString(),new GemShopQuote(quote.id(),quote.playerId(),quote.product(),GemShopQuote.Status.CONFIRMED,quote.createdAt(),quote.expiresAt()),current.revision());return receipt;});}
    @Override public CompletionStage<Optional<GemShopReceipt>>receipt(UUID receiptId){return store.read(reader->{String after=null;while(true){var page=receipts.scanPage(reader,after,1000);var found=page.stream().map(RecordRepository.KeyedVersionedValue::value).filter(value->value.id().equals(receiptId)).findFirst();if(found.isPresent()||page.size()<1000)return found;after=page.getLast().key();}});}
    private GemProduct require(String id){GemProduct product=products.get(id.toUpperCase(Locale.ROOT));if(product==null)throw new IllegalArgumentException("Unknown GemShop product "+id);return product;}
}
