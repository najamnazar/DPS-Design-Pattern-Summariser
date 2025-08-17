package summarise;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.collections4.MultiValuedMap;

import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.realiser.english.Realiser;
import utils.Utils;

public class Summarise {
    public String summarise(HashMap<String, HashMap> fileDetails,
            ArrayList<HashMap> designPatternDetails,
            HashMap<String, MultiValuedMap<String, String>> summary, String projectName) {

        ClassInterfaceSummariser classInterfaceSummariser = new ClassInterfaceSummariser();
        DesignPatternSummarise designPatternSummarise = new DesignPatternSummarise();
        MethodSummariser methodSummariser = new MethodSummariser();

        Lexicon lexicon = Lexicon.getDefaultLexicon();
        NLGFactory nlgFactory = new NLGFactory(lexicon);
        Realiser realiser = new Realiser(lexicon);

        String projectSummary = "";

        // if the project has a design pattern, include the multivalue map to store //
        // values
        if (!designPatternDetails.isEmpty()) {
            designPatternSummarise.summarise(fileDetails, designPatternDetails, summary);
        }
        for (String designPattern : summary.keySet()) {
            projectSummary += "\n" + designPattern + ":\n";
            String designPatternSummary = "";
            for (Map.Entry<String, HashMap> fileEntry : fileDetails.entrySet()) {

                String file = fileEntry.getKey();
                HashSet<String> fileSummarySet = new HashSet<>();
                for (String fileSummary : summary.get(designPattern).get(file))
                    fileSummarySet.add(fileSummary);

                // generate class detail description, put summary as a parameter so that
                // design pattern details shall be included.
                ArrayList classDetails = Utils.getClassOrInterfaceDetails(fileEntry.getValue());
                if (classDetails.size() == 0) {
                    continue;
                }
                HashMap classDetail = Utils.getClassOrInterfaceDetails(fileEntry.getValue()).get(0);
                String classDescription = classInterfaceSummariser.generateClassDescription(nlgFactory,
                        realiser, classDetail, fileSummarySet);
                // generate method description, as well as method usage sdescription, merge into
                // method summary
                ArrayList<HashMap> methodDetails = Utils.getMethodDetails(fileEntry.getValue());
                if (methodDetails.size() != 0) {
                    String methodDescription = methodSummariser.generateMethodsSummary(nlgFactory, realiser,
                            methodDetails, file);
                    String methodUsageDescription = methodSummariser.generateMethodDescription(nlgFactory, realiser,
                            methodDetails);
                    String methodSummary = methodDescription + " " + methodUsageDescription;
                    classDescription += " " + methodSummary;
                }
                designPatternSummary += classDescription;
            }

            try {
                // use the project name, create txt files to store projects, and write to
                // projects

                File outputDir = new File("summary");
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                // Create the directory: summary/projectName/designPattern
                Path designPatternDir = Paths.get("summary", projectName, designPattern);
                Files.createDirectories(designPatternDir);

                // // Write each class summary to its own file
                for (String className : summary.get(designPattern).keySet()) {
                    Path filePath = designPatternDir.resolve(className + ".txt");
                    try (FileWriter fileWriter = new FileWriter(filePath.toFile())) {
                        for (String line : summary.get(designPattern).get(className)) {
                            fileWriter.write(line + "\n");
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            projectSummary += designPatternSummary;
        }

        // try {
        // // use the project name, create txt files to store projects, and write to
        // // projects

        // File outputDir = new File("summary");
        // if (!outputDir.exists()) {
        // outputDir.mkdirs();
        // }

        // FileWriter summaryWriter = new FileWriter(
        // new File("summary", projectName + ".txt"));
        // summaryWriter.write(projectSummary);
        // summaryWriter.close();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }

        return projectSummary;
    }
}
