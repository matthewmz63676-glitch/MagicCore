package com.magicstudios.magiccore.imports;

import java.util.List;

public record ImportPage(List<ImportRow> rows,String nextCursor,boolean done) { public ImportPage { rows=List.copyOf(rows); } }
