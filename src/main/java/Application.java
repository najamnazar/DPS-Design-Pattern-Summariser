import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import parseProject.ParseProject;

public class Application {

    public static void main(String[] args) throws IOException {

        ParseProject parseProject = new ParseProject();

        // Create output and reference Directory if non-existent
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File referenceDir = new File("reference");
        if (!referenceDir.exists()) {
            referenceDir.mkdirs();
        }

        // Gets list of directories (folders) from specified input folder
        File[] projects = new File("input").listFiles(File::isDirectory);
        for (File project : projects) {
            System.out.println("\n" + project.getName());
            HashMap<String, Object> parsedProject = new HashMap<>();

            try{
                // Each directory in input folder is parsed
                parsedProject = parseProject.parseProject(project);
            } catch (Exception e){
                System.out.println("\tError during project " + project.getName());
                e.printStackTrace();
                continue;
            }

            ObjectWriter writer = new ObjectMapper()
                    .writer(new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("\t", "\n")));

            if (parsedProject.isEmpty()) {
                System.out.println("\tEmpty");
                continue;
            }
            writer.writeValue(new File("output/" + project.getName() + ".json"),
                    parsedProject);
        }
    }
}
