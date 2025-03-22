package summarise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import designPatterns.*;

public class DesignPatternSummarise {
    private HashMap<String, DesignPatterns> patternList = new HashMap<String, DesignPatterns>();

    public DesignPatternSummarise() {
        ArrayList<DesignPatterns> patterns = new ArrayList<>();
        patterns.add(new SingletonPattern());
        patterns.add(new FactoryPattern());
        patterns.add(new AbstractFactoryPattern());
        patterns.add(new AdapterPattern());
        patterns.add(new DecoratorPattern());
        patterns.add(new VisitorPattern());
        patterns.add(new FacadePattern());
        patterns.add(new ObserverPattern());
        patterns.add(new MementoPattern());

        for (DesignPatterns pattern : patterns)
            patternList.put(pattern.getPatternName(), pattern);
    }

    public void summarise(HashMap<String, HashMap> fileDetails,
            ArrayList<HashMap> designPatternDetails, HashMap<String, MultiValuedMap<String, String>> summary) {
        for (HashMap<String, Object> identifiedDesignPattern : designPatternDetails)
            for (Map.Entry<String, Object> designPatternEntry : identifiedDesignPattern.entrySet()) {
                summary.put(designPatternEntry.getKey(), new HashSetValuedHashMap<String, String>());
                patternList.get(designPatternEntry.getKey()).summarise(fileDetails,
                        identifiedDesignPattern, summary.get(designPatternEntry.getKey()));
            }
    }
}
