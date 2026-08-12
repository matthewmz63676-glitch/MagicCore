package com.magicstudios.magiccore.modules.bounties;

import java.util.List;

public record BountyPage(List<Bounty> bounties,int page,int pageSize,boolean hasMore){public BountyPage{bounties=List.copyOf(bounties);}}
