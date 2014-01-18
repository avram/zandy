package com.gimranov.zandy.app;

public class Util {
    private static final String TAG = Util.class.getCanonicalName();

    public static final String DOI_PREFIX = "http://dx.doi.org/";

    public static String doiToUri(String doi) {
       if (isDoi(doi)) {
           return DOI_PREFIX + doi.replaceAll("^doi:", "");
       }
        return doi;
    }

    public static boolean isDoi(String doi) {
        return (doi.startsWith("doi:") || doi.startsWith("10."));
    }
}
