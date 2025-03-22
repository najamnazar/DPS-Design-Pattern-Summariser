package designPatterns;

import java.util.*;
import org.apache.commons.collections4.MultiValuedMap;

import summarise.SentenceGenerator;
import utils.Utils;

public abstract class DesignPatterns {
    protected String patternName;
    protected String patternNameAsText;

    protected SentenceGenerator sentenceGenerator = new SentenceGenerator();

    public DesignPatterns(String patternName) {
        this.patternName = patternName;
        this.patternNameAsText = Utils.convertToPlainText(patternName);
    }

    public String getPatternName() {
        return patternName;
    }

    public abstract HashMap checkPattern(HashMap<String, HashMap> fileDetails);

    public abstract void summarise(HashMap<String, HashMap> fileDetails,
            HashMap designPatternDetails, MultiValuedMap<String, String> summary);
}
