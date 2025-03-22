package utils;

import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// referenced from Java Callgraph
/**
 * 符号解析工厂类
 * 
 * @author allen
 */
public class SymbolSolverFactory {
    // referenced from Java Callgraph
    /**
     * 获取符号推理器，以便获取某个类的具体来源
     * 
     * @param srcPaths
     * @param libPaths
     * @return
     */
    public static JavaSymbolSolver getJavaSymbolSolver(List<String> srcPaths, List<String> libPaths) {
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver(); // jdk推理
        List<JavaParserTypeSolver> javaParserTypeSolvers = makeJavaParserTypeSolvers(srcPaths); // 工程内代码推理
        List<JarTypeSolver> jarTypeSolvers = makeJarTypeSolvers(libPaths);// jar包推理
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);
        javaParserTypeSolvers.stream().forEach(t -> combinedTypeSolver.add(t));
        jarTypeSolvers.stream().forEach(t -> combinedTypeSolver.add(t));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        return symbolSolver;
    }

    // referenced from Java Callgraph
    /**
     * 获取jar包的符号推理器
     * 
     * @param libPaths
     * @return
     */
    private static List<JarTypeSolver> makeJarTypeSolvers(List<String> libPaths) {
        List<String> jarPaths = Utils.getFilesBySuffixInPaths("jar", libPaths);
        List<JarTypeSolver> jarTypeSolvers = new ArrayList<>(jarPaths.size());
        try {
            for (String jarPath : jarPaths) {
                jarTypeSolvers.add(new JarTypeSolver(jarPath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jarTypeSolvers;
    }

    // referenced from Java Callgraph
    /**
     * 获取工程源代码src的符号推理器
     * 
     * @param srcPaths
     * @return
     */
    private static List<JavaParserTypeSolver> makeJavaParserTypeSolvers(List<String> srcPaths) {
        List<JavaParserTypeSolver> javaParserTypeSolvers = srcPaths.stream()
                .map(t -> new JavaParserTypeSolver(new File(t))).collect(Collectors.toList());
        return javaParserTypeSolvers;
    }

    // referenced from Java Callgraph
    /**
     * 获取符号推理器
     * 
     * @param srcPath
     * @param libPath
     * @return
     */
    public JavaSymbolSolver getJavaSymbolSolver(String srcPath, String libPath) {
        return getJavaSymbolSolver(Utils.makeListFromOneElement(srcPath), Utils.makeListFromOneElement(libPath));
    }
}
