package com.magicstudios.magiccore.imports;

import java.util.List;

public record ImportRun(ImportJob job,List<ImportIssue>issues){public ImportRun{issues=List.copyOf(issues);}}
