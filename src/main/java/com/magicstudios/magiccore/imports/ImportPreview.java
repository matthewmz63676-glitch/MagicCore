package com.magicstudios.magiccore.imports;

import java.util.List;
import java.util.Map;

public record ImportPreview(String importId,String sourceId,String targetId,String fingerprint,Map<String,String>mapping,
                            long totalRows,long validRows,List<ImportIssue>issues) { public ImportPreview{mapping=Map.copyOf(mapping);issues=List.copyOf(issues);} }
