package parseProject;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class FieldExtr extends VoidVisitorAdapter<Void> {
    private ArrayList<HashMap<String, Object>> fieldInfoHashArray;

    public FieldExtr() {
        this.fieldInfoHashArray = new ArrayList<>();
    }

    public ArrayList<HashMap<String, Object>> getFieldInfo(CompilationUnit compilationUnit) {
        compilationUnit.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(FieldDeclaration fd, Void arg) {
                HashMap<String, Object> fieldInfo = new HashMap<>();

                fieldInfo.put("FIELDDECLARATION", Utils.nodeListToArrayList(fd.getVariables()));

                // FDT
                fieldInfo.put("FIELDDATATYPE", fd.getElementType().toString());
                fieldInfo.put("FIELDCOMMONTYPE", fd.getCommonType().toString());
                fieldInfo.put("FIELDACCESSSPECIFIER", fd.getAccessSpecifier().toString());

                // FMT
                fieldInfo.put("FIELDMODIFIERTYPE", Utils.nodeListToArrayList(fd.getModifiers()));

                fieldInfoHashArray.add(fieldInfo);
            }
        }, null);

        return fieldInfoHashArray;
    }

}
