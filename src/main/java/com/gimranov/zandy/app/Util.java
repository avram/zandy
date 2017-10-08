package com.gimranov.zandy.app;

class Util {
    private static final String TAG = Util.class.getCanonicalName();

    private static final String DOI_PREFIX = "http://dx.doi.org/";

    static String doiToUri(String doi) {
       if (isDoi(doi)) {
           return DOI_PREFIX + doi.replaceAll("^doi:", "");
       }
        return doi;
    }

    private static boolean isDoi(String doi) {
        return (doi.startsWith("doi:") || doi.startsWith("10."));
    }
}
