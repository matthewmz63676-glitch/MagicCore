package com.magicstudios.magiccore.imports;

import com.magicstudios.magiccore.platform.SchedulerFacade;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class CsvImportSource implements ImportSource {
 private final String id,fingerprint;private final List<ImportRow>rows;
 private CsvImportSource(String id,String fingerprint,List<ImportRow>rows){this.id=id;this.fingerprint=fingerprint;this.rows=List.copyOf(rows);}
 public static CompletionStage<CsvImportSource>load(Path importsRoot,Path file,SchedulerFacade scheduler){return scheduler.supplyAsync(()->load(importsRoot,file));}
 public static CsvImportSource load(Path importsRoot,Path file)throws Exception{Path root=importsRoot.toAbsolutePath().normalize(),source=file.toAbsolutePath().normalize();if(!source.startsWith(root))throw new SecurityException("Import source must remain inside "+root);
  byte[]bytes=Files.readAllBytes(source);List<String>lines=Files.readAllLines(source,StandardCharsets.UTF_8);if(lines.isEmpty())throw new IllegalArgumentException("CSV is empty");List<String>header=parse(lines.getFirst());if(header.isEmpty()||header.stream().anyMatch(String::isBlank))throw new IllegalArgumentException("CSV header is invalid");
  List<ImportRow>rows=new ArrayList<>();for(int i=1;i<lines.size();i++){if(lines.get(i).isBlank())continue;List<String>values=parse(lines.get(i));if(values.size()!=header.size())throw new IllegalArgumentException("CSV column mismatch at line "+(i+1));LinkedHashMap<String,String>mapped=new LinkedHashMap<>();for(int c=0;c<header.size();c++)mapped.put(header.get(c),values.get(c));rows.add(new ImportRow(String.format("%09d",i),mapped));}
  return new CsvImportSource(source.getFileName().toString(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),rows);}
 @Override public String sourceId(){return id;}@Override public String fingerprint(){return fingerprint;}
 @Override public CompletionStage<ImportPage>read(String after,int limit){if(limit<1||limit>1000)throw new IllegalArgumentException("limit must be 1..1000");int start=0;if(after!=null)while(start<rows.size()&&rows.get(start).key().compareTo(after)<=0)start++;int end=Math.min(rows.size(),start+limit);List<ImportRow>page=rows.subList(start,end);String next=page.isEmpty()?after:page.getLast().key();return CompletableFuture.completedFuture(new ImportPage(page,next,end>=rows.size()));}
 private static List<String>parse(String line){ArrayList<String>values=new ArrayList<>();StringBuilder current=new StringBuilder();boolean quoted=false;for(int i=0;i<line.length();i++){char ch=line.charAt(i);if(ch=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){current.append('"');i++;}else quoted=!quoted;}else if(ch==','&&!quoted){values.add(current.toString());current.setLength(0);}else current.append(ch);}if(quoted)throw new IllegalArgumentException("Unclosed CSV quote");values.add(current.toString());return values;}
}
