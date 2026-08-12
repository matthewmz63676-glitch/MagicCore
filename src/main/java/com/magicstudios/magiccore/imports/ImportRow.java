package com.magicstudios.magiccore.imports;

import java.util.Map;

public record ImportRow(String key, Map<String,String> values) { public ImportRow { values=Map.copyOf(values); } }
