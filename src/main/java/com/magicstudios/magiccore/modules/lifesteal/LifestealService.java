package com.magicstudios.magiccore.modules.lifesteal;
import com.magicstudios.magiccore.modules.combat.VerifiedPlayerKill;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
public interface LifestealService{
 CompletionStage<HeartAccount> account(UUID playerId);
 CompletionStage<HeartMutation> transfer(VerifiedPlayerKill kill,String operationKey);
 CompletionStage<HeartMutation> nonPlayerDeath(UUID playerId,String operationKey);
 CompletionStage<HeartMutation> withdraw(UUID playerId,String operationKey);
 CompletionStage<HeartMutation> consume(UUID playerId,String operationKey);
 CompletionStage<HeartMutation> revive(UUID playerId,String operationKey);
 CompletionStage<List<HeartAccount>> leaderboard(int limit);
}
