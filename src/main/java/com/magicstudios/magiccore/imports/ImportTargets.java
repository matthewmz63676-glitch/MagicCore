package com.magicstudios.magiccore.imports;

import com.magicstudios.magiccore.modules.crates.CrateService;
import com.magicstudios.magiccore.modules.economy.EconomyService;
import com.magicstudios.magiccore.modules.economy.Money;
import com.magicstudios.magiccore.modules.profiles.PlayerProfileService;
import com.magicstudios.magiccore.ranks.RankService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ImportTargets {
 private ImportTargets(){}
 public static ImportTarget profiles(PlayerProfileService profiles){return new ImportTarget(){public String targetId(){return "profiles";}public List<String>requiredFields(){return List.of("uuid","name","locale","seen_at");}
  public List<String>validate(ImportRow row){try{UUID.fromString(row.values().get("uuid"));Instant.parse(row.values().get("seen_at"));return row.values().get("name").isBlank()?List.of("name is blank"):List.of();}catch(Exception failure){return List.of("invalid uuid or seen_at");}}
  public java.util.concurrent.CompletionStage<Void>apply(ImportRow row,String operation){return profiles.recordSeen(UUID.fromString(row.values().get("uuid")),row.values().get("name"),row.values().get("locale"),Instant.parse(row.values().get("seen_at"))).thenApply(ignored->null);}
  public java.util.concurrent.CompletionStage<Boolean>verify(ImportRow row){return profiles.find(UUID.fromString(row.values().get("uuid"))).thenApply(found->found.map(value->value.currentName().equals(row.values().get("name"))&&value.locale().equals(row.values().get("locale"))).orElse(false));}};}
 public static ImportTarget balances(EconomyService economy){return new ImportTarget(){public String targetId(){return "balances";}public List<String>requiredFields(){return List.of("uuid","currency","balance_minor");}
  public List<String>validate(ImportRow row){try{UUID.fromString(row.values().get("uuid"));long value=Long.parseLong(row.values().get("balance_minor"));return value<0||!economy.currencies().containsKey(row.values().get("currency"))?List.of("invalid balance or currency"):List.of();}catch(Exception failure){return List.of("invalid uuid or balance");}}
  public java.util.concurrent.CompletionStage<Void>apply(ImportRow row,String operation){UUID id=UUID.fromString(row.values().get("uuid"));String currency=row.values().get("currency");long desired=Long.parseLong(row.values().get("balance_minor"));return economy.balance(id,currency).thenCompose(current->economy.adjust(id,new Money(currency,desired-current.minorUnits()),"IMPORT","balance-import",operation)).thenApply(ignored->null);}
  public java.util.concurrent.CompletionStage<Boolean>verify(ImportRow row){return economy.balance(UUID.fromString(row.values().get("uuid")),row.values().get("currency")).thenApply(value->value.minorUnits()==Long.parseLong(row.values().get("balance_minor")));}};}
 public static ImportTarget ranks(RankService ranks){return new ImportTarget(){public String targetId(){return "ranks";}public List<String>requiredFields(){return List.of("uuid","rank_id");}
  public List<String>validate(ImportRow row){try{UUID.fromString(row.values().get("uuid"));ranks.catalog().require(row.values().get("rank_id"));return List.of();}catch(Exception failure){return List.of("invalid uuid or rank");}}
  public java.util.concurrent.CompletionStage<Void>apply(ImportRow row,String operation){return ranks.setRank(UUID.fromString(row.values().get("uuid")),row.values().get("rank_id"),"IMPORT",operation).thenApply(ignored->null);}
  public java.util.concurrent.CompletionStage<Boolean>verify(ImportRow row){return ranks.rankOf(UUID.fromString(row.values().get("uuid"))).thenApply(value->value.equals(row.values().get("rank_id")));}};}
 public static ImportTarget crateKeys(CrateService crates){return new ImportTarget(){public String targetId(){return "crate-keys";}public List<String>requiredFields(){return List.of("uuid","key_id","amount");}
  public List<String>validate(ImportRow row){try{UUID.fromString(row.values().get("uuid"));return Long.parseLong(row.values().get("amount"))<0?List.of("negative key amount"):List.of();}catch(Exception failure){return List.of("invalid uuid or key amount");}}
  public java.util.concurrent.CompletionStage<Void>apply(ImportRow row,String operation){UUID id=UUID.fromString(row.values().get("uuid"));String key=row.values().get("key_id");long desired=Long.parseLong(row.values().get("amount"));return crates.keyBalance(id,key).thenCompose(current->{if(current.amount()>desired)return CompletableFuture.failedFuture(new IllegalStateException("IMPORT_WOULD_DECREASE_KEYS"));if(current.amount()==desired)return CompletableFuture.completedFuture(current);return crates.grantKeys(id,key,desired-current.amount(),operation);}).thenApply(ignored->null);}
  public java.util.concurrent.CompletionStage<Boolean>verify(ImportRow row){return crates.keyBalance(UUID.fromString(row.values().get("uuid")),row.values().get("key_id")).thenApply(value->value.amount()==Long.parseLong(row.values().get("amount")));}};}
}
