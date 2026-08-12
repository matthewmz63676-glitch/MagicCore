package com.magicstudios.magiccore.modules.events;

import java.util.Set;
import java.util.UUID;

public record KothContender(String groupId,String displayName,Set<UUID>rewardRecipients){public KothContender{rewardRecipients=Set.copyOf(rewardRecipients);if(groupId.isBlank()||rewardRecipients.isEmpty())throw new IllegalArgumentException("A KOTH contender needs an identity and recipient");}}
