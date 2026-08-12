package com.magicstudios.magiccore.placeholders;

import com.magicstudios.magiccore.modules.worth.ItemValuation;
import com.magicstudios.magiccore.modules.worth.ItemValuationService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WorthPlaceholderView {
    private final ItemValuationService valuation;private final Map<UUID,String>held=new ConcurrentHashMap<>();
    public WorthPlaceholderView(ItemValuationService valuation){this.valuation=valuation;}
    public void register(String owner,PlaceholderRegistry registry){registry.register(owner,"worth_held",context->context.subjectId()==null?"":held.getOrDefault(context.subjectId(),""));}
    public void update(UUID playerId,ItemValuation value){held.put(playerId,value.sellable()?Long.toString(value.totalWorthMinor()):"0");}
    public void unavailable(UUID playerId){held.put(playerId,"0");}
    public String render(ItemValuation value){return valuation.render(value);}
    public java.util.List<String> loreLines(ItemValuation value){return java.util.List.of(valuation.render(value));}
    public void remove(UUID playerId){held.remove(playerId);}
}
