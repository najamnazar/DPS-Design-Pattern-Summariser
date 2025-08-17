package parseProject;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;

import designPatterns.CheckPattern;
import summarise.Summarise;
import utils.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.collections4.MultiValuedMap;

public class ParseProject {

    // reference: java callgraph
    // 需要跳过的pattern列表
    private List<Pattern> skipPatterns = new ArrayList<Pattern>();

    public HashMap<String, Object> parseProject(File directory, LanguageLevel languageLevel)
            throws FileNotFoundException {

        ArrayList<File> fileArrayList = new ArrayList<>();

        // referenced from Java Callgraph
        ArrayList<String> srcPathList = new ArrayList<>();
        ArrayList<String> libPathList = new ArrayList<>();

        // names of all files in directory added to fileArrayList (list not tree)
        // srcPathList and libPathList consist of abs paths of src and lib folders
        fetchFiles(directory, fileArrayList, srcPathList, libPathList);

        // referenced from Java Callgraph
        JavaSymbolSolver symbolSolver = SymbolSolverFactory.getJavaSymbolSolver(srcPathList, libPathList);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
        StaticJavaParser.getParserConfiguration().setLanguageLevel(languageLevel);

        // referenced from Java callgraph
        // 获取src目录中的全部java文件，并进行解析
        HashMap<String, ArrayList<String>> callerCallees = new HashMap<>();

        HashMap<String, HashMap> parsedFile = new HashMap<>();
        CheckPattern checkPattern = new CheckPattern();
        Summarise summarise = new Summarise();

        ArrayList designPatternArrayList = new ArrayList<>();

        HashMap<String, MultiValuedMap<String, String>> summaries = new HashMap<>();
        HashMap<String, HashMap<String, HashSet<String>>> summaryMap = new HashMap<String, HashMap<String, HashSet<String>>>();
        String finalSummary = "";

        // go through all files under the project
        for (File file : fileArrayList) {
            try {
                HashMap<String, ArrayList> fileDetails = new HashMap<>();
                CompilationUnit compilationUnit = new CompilationUnit();

                try {
                    compilationUnit = StaticJavaParser.parse(file);
                } catch (Exception e) {
                    System.out.println("Skipping file due to parse error: " + file.getName() + ", " + e.getMessage());
                    continue;
                } catch (Error e) {
                    System.out.println("Skipping file due to parse error: " + file.getName() + ", " + e.getMessage());
                    continue;
                }

                MethodsExtr methodsExtr = new MethodsExtr();

                FieldExtr fieldExtr = new FieldExtr();
                ConstructorExtr constructorExtr = new ConstructorExtr();
                VariableExtr variableExtr = new VariableExtr();
                ClassOrInterfaceExtr classOrInterfaceExtr = new ClassOrInterfaceExtr();

                fileDetails.put("FIELDDETAIL", fieldExtr.getFieldInfo(compilationUnit));
                fileDetails.put("CONSTRUCTORDETAIL", constructorExtr.getConstructorInfo(compilationUnit));
                fileDetails.put("VARIABLEDETAIL", variableExtr.getVariableInfo(compilationUnit));
                fileDetails.put("METHODDETAIL", methodsExtr.getMethodInfo(compilationUnit));
                fileDetails.put("CLASSORINTERFACEDETAIL", classOrInterfaceExtr.getClassInterfaceInfo(compilationUnit));
                extract(compilationUnit, callerCallees, skipPatterns);

                // zip the extracted file details with the file name
                parsedFile.put(Utils.getBaseName(file.getName()), fileDetails);
            } catch (Exception e) {
                System.out.println("Error processing file: " + file.getName() + ", " + e.getMessage());
                e.printStackTrace();
                continue;
            } catch (Error e) {
                System.out.println("Error processing file: " + file.getName() + ", " + e.getMessage());
                e.printStackTrace();
                continue;
            }
        }

        // merge the features with the callgraph
        HashMap<String, Object> parsedProject = new HashMap<>();
        HashMap extractedCallGraph = extractCallgraphResults(parsedFile, callerCallees);

        if (extractedCallGraph.isEmpty())
            return new HashMap<>();
        checkPattern.extractDesignPattern(extractedCallGraph, designPatternArrayList);

        if (!designPatternArrayList.isEmpty()) {
            finalSummary = summarise.summarise(extractedCallGraph, designPatternArrayList,
                    summaries, directory.getName());
            for (String designPattern : summaries.keySet()) {
                summaryMap.put(designPattern, new HashMap<>());
                for (String classString : summaries.get(designPattern).keySet()) {
                    HashSet<String> summarySet = new HashSet<String>();
                    for (String summary : summaries.get(designPattern).get(classString)) {
                        summarySet.add(summary);
                    }
                    summaryMap.get(designPattern).put(classString, summarySet);
                }
            }
        }
        parsedProject.put(directory.getName(), extractedCallGraph);
        parsedProject.put("design_pattern", designPatternArrayList);
        parsedProject.put("summary_NLG", summaryMap);
        parsedProject.put("final_summary", finalSummary);

        // return the result, which contains all files of the project, stored in the
        // hashmap, the key is file name, the value is the details.
        return parsedProject;
    }

    private HashMap<String, HashMap> extractCallgraphResults(HashMap<String, HashMap> parsedFile,
            HashMap<String, ArrayList<String>> callerCallees) {
        Set<String> classNames = parsedFile.keySet();

        for (HashMap.Entry mapElement : callerCallees.entrySet()) {
            String caller = (String) mapElement.getKey();
            ArrayList<String> callees = callerCallees.get(caller);

            String callerClass = extractCallgraphClass(caller);
            String callerMethodName = extractCallgraphMethodName(caller);

            if (classNames.contains(callerClass)) {
                HashMap<String, ArrayList> parsedCallerClass = parsedFile.get(callerClass);
                ArrayList<HashMap> parsedCalledMethods = Utils.getMethodDetails(parsedCallerClass);
                for (HashMap parsedCalledMethod : parsedCalledMethods) {

                    // need parameter comparison also
                    if (Utils.getMethodName(parsedCalledMethod).equals(callerMethodName)) {
                        for (String callee : callees) {

                            String calleeClass = extractCallgraphClass(callee);
                            String calleeMethodName = extractCallgraphMethodName(callee);

                            HashMap<String, String> newOutgoing = new HashMap<>();
                            newOutgoing.put("CALLEECLASS", calleeClass);
                            newOutgoing.put("CALLEEMETHODNAME", calleeMethodName);

                            Utils.getOutgoingMethod(parsedCalledMethod).add(newOutgoing);

                            // add incoming method for the method in caller class
                            HashMap<String, ArrayList> parsedCalleeClass = parsedFile.get(calleeClass);
                            if (parsedCalleeClass == null) {
                                continue;
                            }
                            ArrayList<HashMap> parsedCallingMethods = Utils.getMethodDetails(parsedCalleeClass);

                            for (HashMap parsedCallingMethod : parsedCallingMethods) {

                                if (Utils.getMethodName(parsedCallingMethod).equals(calleeMethodName)) {

                                    HashMap<String, String> newIncoming = new HashMap<>();
                                    newIncoming.put("CALLEDCLASS", callerClass);
                                    newIncoming.put("CALLEDMETHODNAME", callerMethodName);

                                    Utils.getIncomingMethod(parsedCallingMethod).add(newIncoming);

                                    // Update number of incoming calls
                                    parsedCallingMethod.put("NUMBEROFINCOMINGMETHODS",
                                            Utils.getIncomingMethod(parsedCallingMethod).size());
                                }
                            }
                        }
                    }
                }
            }
        }
        return parsedFile;
    }

    private String extractCallgraphClass(String caller) {
        String filteredCaller = caller.replaceAll("\\(.*\\)", "");
        return Utils.splitByDot(filteredCaller, 2);
    }

    private String extractCallgraphMethodName(String caller) {
        String filteredCaller = caller.replaceAll("\\(.*\\)", "");
        return Utils.splitByDot(filteredCaller, 1);
    }

    private void fetchFiles(File dir, ArrayList<File> fileList, ArrayList<String> srcPathList,
            ArrayList<String> libPathList) {
        if (dir.getName().equals("src")) {
            srcPathList.add(dir.getAbsolutePath());
        }

        if (dir.getName().equals("lib")) {
            libPathList.add(dir.getAbsolutePath());
        }

        if (dir.isDirectory()) {
            for (File file1 : dir.listFiles()) {
                fetchFiles(file1, fileList, srcPathList, libPathList);
            }
        } else if (Utils.getExtension(dir).equals("java")) {
            fileList.add(dir);
        }

    }

    // referenced from Java Callgraph
    private void extract(CompilationUnit compilationUnit, HashMap<String, ArrayList<String>> callerCallees,
            List<Pattern> skipPatterns) {

        // 获取到方法声明，并进行遍历
        List<MethodDeclaration> all = compilationUnit.findAll(MethodDeclaration.class);
        for (MethodDeclaration methodDeclaration : all) {
            ArrayList<String> curCallees = new ArrayList<>();

            // 对每个方法声明内容进行遍历，查找内部调用的其他方法
            methodDeclaration.accept(new MethodCallVisitor(skipPatterns), curCallees);
            String caller;
            try {
                caller = methodDeclaration.resolve().getQualifiedSignature();
            } catch (Exception e) {
                caller = methodDeclaration.getSignature().asString();
                System.out.println("Use " + caller + " instead of  qualified signature, cause: " + e.getMessage());
            }
            assert caller != null;

            // // 如果map中还没有key，则添加key
            if (!callerCallees.containsKey(caller) && !Utils.shouldSkip(caller, skipPatterns)) {
                callerCallees.put(caller, new ArrayList<>());
            }

            if (!Utils.shouldSkip(caller, skipPatterns)) {
                callerCallees.get(caller).addAll(curCallees);
            }

        }
    }

    // 遍历源码文件时，只关注方法调用的Visitor， 然后提取存放到第二个参数collector中
    private static class MethodCallVisitor extends VoidVisitorAdapter<List<String>> {

        private List<Pattern> skipPatterns = new ArrayList<Pattern>();

        public MethodCallVisitor(List<Pattern> skipPatterns) {
            if (skipPatterns != null) {
                this.skipPatterns = skipPatterns;

            }
        }

        @Override
        public void visit(MethodCallExpr n, List<String> collector) {
            // 提取方法调用
            ResolvedMethodDeclaration resolvedMethodDeclaration = null;
            try {
                resolvedMethodDeclaration = n.resolve();
                // 仅关注提供src目录的工程代码
                // resolvedMethodDeclaration.get
                String signature = n.resolve().getQualifiedSignature();
                if (!Utils.shouldSkip(signature, skipPatterns)) {
                    if (resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) {
                        collector.add(signature);
                    }
                }
            } catch (Exception e) {
                System.out.print("Line ");
                System.out.print(n.getRange().get().begin.line);
                System.out.print(", ");
                System.out.print(
                        n.getNameAsString() + n.getArguments()
                                .toString().replace("[", "(").replace("]", ")"));
                System.out.print(" cannot resolve some symbol, because ");
                System.out.println(e.getMessage());
            }
            // Don't forget to call super, it may find more method calls inside the
            // arguments of this method call, for example.
            super.visit(n, collector);
        }
    }

}
