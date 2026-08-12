package com.magicstudios.magiccore.integrations.luckperms;

import com.magicstudios.magiccore.ranks.RankCatalog;
import com.magicstudios.magiccore.ranks.RankChange;
import com.magicstudios.magiccore.ranks.RankService;
import com.magicstudios.magiccore.ranks.RankSyncPreview;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class LuckPermsRankService implements RankService {
    private final LuckPerms luckPerms;
    private final RankCatalog catalog;

    public LuckPermsRankService(LuckPerms luckPerms, RankCatalog catalog) {
        this.luckPerms = luckPerms;
        this.catalog = catalog;
    }

    @Override
    public RankCatalog catalog() {
        return catalog;
    }

    @Override
    public CompletionStage<String> rankOf(UUID playerId) {
        return luckPerms.getUserManager().loadUser(playerId).thenApply(this::mappedPrimaryGroup);
    }

    @Override
    public CompletionStage<RankChange> setRank(UUID playerId, String rankId, String actor, String operationKey) {
        catalog.require(rankId);
        return luckPerms.getUserManager().loadUser(playerId).thenCompose(user -> {
            String previous = mappedPrimaryGroup(user);
            Set<String> managedGroups = catalog.definitions().keySet().stream()
                    .map(id -> id.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
            user.getNodes(NodeType.INHERITANCE).stream()
                    .filter(node -> managedGroups.contains(node.getGroupName().toLowerCase(Locale.ROOT)))
                    .forEach(node -> user.data().remove(node));
            user.data().add(InheritanceNode.builder(rankId.toLowerCase(Locale.ROOT)).build());
            return luckPerms.getUserManager().saveUser(user)
                    .thenApply(ignored -> new RankChange(!previous.equals(rankId), previous, rankId));
        });
    }

    @Override
    public CompletionStage<RankSyncPreview> previewSync(UUID playerId, String rankId) {
        catalog.require(rankId);
        return luckPerms.getUserManager().loadUser(playerId).thenApply(user -> {
            String previous = mappedPrimaryGroup(user);
            Set<String> managed = catalog.definitions().keySet().stream()
                    .map(id -> "group." + id.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
            Set<String> currentNodes = user.getNodes().stream().map(Node::getKey)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            Set<String> preserved = new LinkedHashSet<>(currentNodes);
            preserved.removeAll(managed);
            return new RankSyncPreview(playerId.toString(), previous, rankId,
                    Set.of("group." + rankId.toLowerCase(Locale.ROOT)),
                    previous.equals(rankId) ? Set.of() : Set.of("group." + previous.toLowerCase(Locale.ROOT)),
                    preserved);
        });
    }

    private String mappedPrimaryGroup(User user) {
        String group = user.getPrimaryGroup().toUpperCase(Locale.ROOT);
        return catalog.definitions().containsKey(group) ? group : catalog.defaultRank();
    }
}
