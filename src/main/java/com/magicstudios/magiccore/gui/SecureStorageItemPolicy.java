package com.magicstudios.magiccore.gui;

public record SecureStorageItemPolicy(int maximumItemBytes,int maximumContainerBytes,String nestedPolicy,String customPolicy){
    public boolean accepts(int payloadBytes,boolean container,boolean nestedNonEmpty,boolean custom,long currentBytes,int insertionMultiplier){if(payloadBytes<0||payloadBytes>maximumItemBytes||currentBytes<0||insertionMultiplier<1)return false;if(container&&(nestedPolicy.equals("DENY_ALL")||(nestedPolicy.equals("DENY_NON_EMPTY")&&nestedNonEmpty)))return false;if(custom&&customPolicy.equals("DENY"))return false;try{return Math.addExact(currentBytes,Math.multiplyExact((long)payloadBytes,insertionMultiplier))<=maximumContainerBytes;}catch(ArithmeticException overflow){return false;}}
}
