package designPatterns;

import java.util.*;

public class CheckPattern {
    private ArrayList<DesignPatterns> patternList = new ArrayList<DesignPatterns>();

    public CheckPattern() {
        patternList.add(new SingletonPattern());
        patternList.add(new FactoryPattern());
        patternList.add(new AbstractFactoryPattern());
        patternList.add(new AdapterPattern());
        patternList.add(new DecoratorPattern());
        patternList.add(new VisitorPattern());
        patternList.add(new FacadePattern());
        patternList.add(new ObserverPattern());
        patternList.add(new MementoPattern());
    }

    public void extractDesignPattern(HashMap fileDetails, ArrayList designPatternArrayList) {
        for (DesignPatterns pattern : patternList) {
            HashMap output = pattern.checkPattern(fileDetails);
            if (!output.isEmpty())
                designPatternArrayList.add(output);
        }
    }
}
