package yaw.engine.shader;

import javax.management.remote.rmi.RMIConnectionImpl;

public class ShaderCode {
    public static final int INDENT_SPACES = 2;

    public StringBuilder code;
    public StringBuilder cLayout;
    public int indentLevel;

    public ShaderCode(String version, boolean coreProfile) {
        code = new StringBuilder();
        code.append("#version ");
        code.append(version);

        if(coreProfile) {
            code.append(" core");
        }
        code.append("\n");
    }

    public void mkIndent() {
        code.append(" ".repeat(indentLevel * INDENT_SPACES));
    }

    public ShaderCode indent() {
        indentLevel += 1;
        return this;
    }

    public ShaderCode dedent() {
        indentLevel -= 1;
        return this;
    }

    public ShaderCode cmt(String comment) {
        mkIndent();
        code.append("/* "); code.append(comment); code.append(" */\n");
        return this;
    }

    public ShaderCode l(String line) {
        mkIndent();
        code.append(line);
        if (!line.endsWith(";")) {
            code.append(";");
        }
        code.append("\n");
        return this;
    }

    public ShaderCode l() {
        code.append("\n");
        return this;
    }

    public ShaderCode beginBlock(){
        mkIndent();
        indent();
        code.append("{\n");
        return this;
    }

    public ShaderCode endBlock(){
        dedent();
        mkIndent();
        code.append("}\n");
        return this;
    }

    public ShaderCode beginStruct(String structName, String cmt) {
        if (cmt != null && !cmt.equals("")) {
            mkIndent();
            code.append("/* "); code.append(cmt); code.append(" */\n");
        }
        mkIndent();
        code.append("struct "); code.append(structName); code.append("\n");
        beginBlock();
        return this;
    }

    public ShaderCode beginStruct(String structName) {
        return beginStruct(structName, "");
    }

    public ShaderCode item(String itemType, String itemName, String itemCmt) {
        if (itemCmt != null && !itemCmt.equals("")) {
            mkIndent();
            code.append("// "); code.append(itemCmt); code.append("\n");
        }
        mkIndent();
        code.append(itemType); code.append(' '); code.append(itemName); code.append(";\n");
        return this;
    }

    public ShaderCode item(String itemType, String itemName) {
        return item(itemType, itemName, "");
    }

    public ShaderCode endStruct() {
        dedent();
        mkIndent();
        code.append("};\n");
        return this;
    }

    public ShaderCode function(String comment, String ret_type, String name, String[][] arguments) {
        mkIndent();
        code.append("/* "); code.append(comment); code.append(" */\n");
        code.append(ret_type); code.append(' '); code.append(name); code.append('(');
        String sep = null;
        for (String[] argument : arguments) {
            if (sep != null) {
                code.append(sep);
            } else {
                sep = ", ";
            }
            code.append(argument[0]);
            code.append(' ');
            code.append(argument[1]);
        }
        code.append(")\n");
        return beginBlock();
    }

    public ShaderCode function(String ret_type, String name, String[][] arguments) {
        return function("", ret_type, name, arguments);
    }

    public ShaderCode endFunction() {
        return endBlock();
    }

    public ShaderCode beginIf(String cond) {
        mkIndent();
        code.append("if ("); code.append(cond); code.append(")\n");
        return beginBlock();
    }

    public ShaderCode endIf() {
        return endBlock();
    }

    public ShaderCode beginElse() {
        mkIndent();
        code.append("else \n");
        return beginBlock();
    }

    public ShaderCode endElse() {
        return endBlock();
    }

    public ShaderCode beginFor(String init, String cond, String update) {
        mkIndent();
        code.append("for (");
        code.append(init); code.append("; "); code.append(cond); code.append("; "); code.append(update);
        code.append(")\n");
        return beginBlock();
    }

    public ShaderCode endFor() {
        return endBlock();
    }

    public ShaderCode beginMain() {
        mkIndent();
        code.append("void main()\n");
        mkIndent();
        code.append("{\n");
        indent();
        return this;
    }

    public ShaderCode endMain() {
        dedent();
        mkIndent();
        code.append("}\n");
        return this;
    }

    @Override
    public String toString() {
        return code.toString();
    }
}