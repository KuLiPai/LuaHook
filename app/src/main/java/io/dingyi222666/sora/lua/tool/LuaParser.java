package io.dingyi222666.sora.lua.tool;

import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;


import org.luaj.LocVars;
import org.luaj.LuaString;
import org.luaj.LuaValue;
import org.luaj.Prototype;
import org.luaj.Upvaldesc;
import org.luaj.compiler.LexState;
import org.luaj.compiler.LuaC;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import io.kulipai.sora.luaj.CompletionName;
import io.github.rosemoe.sora.lang.completion.CompletionItemKind;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;

/**
 * Created by nirenr on 2019/11/23.
 */

public class LuaParser {
    private static HashMap<String, ArrayList<Pair>> localMap = new HashMap<>();
    public static HashMap<String, ArrayList<Method>> javaMethodMap = new HashMap<>();
    public static HashMap<String, ArrayList<Field>> javaFieldMap = new HashMap<>();
    public static HashMap<String, ArrayList<JavaVar>> javaVar = new HashMap<>();
    private static ArrayList<Var> varList = new ArrayList<>();
    private static ArrayList<LuaString> globalist = new ArrayList<>();
    private static HashMap<String, ArrayList<Pair>> valueMap = new HashMap<>();

    public final static int UNKNOWN = -1;
    public final static int NORMAL = 0;
    public final static int KEYWORD = 1;
    public final static int OPERATOR = 2;
    public final static int NAME = 3;
    public final static int LITERAL = 4;
    public final static int GLOBAL = 5;
    public final static int UPVAL = 6;
    public final static int LOCAL = 7;
    public final static int STRING = 8;
    public final static int COMMENT = 9;

    public static ArrayList<CompletionName> filterJava(String pkg, String keyword, int i) {
        //Log.i("luaj", "filterJava: " + pkg + ";" + keyword + ";" + i);
        ArrayList<CompletionName> ms = new ArrayList<>();
        ArrayList<JavaVar> js = javaVar.get(pkg);
        if (js == null)
            return ms;
        for (int i1 = js.size() - 1; i1 >= 0; i1--) {
            JavaVar j = js.get(i1);
            //Log.i("luaj", "filterJava: " + pkg + ";" + keyword + ";" + j.name);
            if (j.startidx <= i && j.endidx >= i) {
                if (j.name.toLowerCase().endsWith("." + pkg)) {
                    ArrayList<Field> mm = javaFieldMap.get(j.name);
                    if (mm != null) {
                        for (Field s : mm) {
                            if (s.getName().toLowerCase().startsWith(keyword))
                                ms.add(PackageUtil.buildJavaField(s));
                        }
                    }
                }
                ArrayList<Method> mm = javaMethodMap.get(j.name);
                if (mm == null)
                    continue;
                for (Method s : mm) {
                    if (s.getName().toLowerCase().startsWith(keyword))
                        ms.add(PackageUtil.buildJavaMethod(s));
                }
                break;
            }
        }
        return ms;
    }

    public static HashMap<String, ArrayList<Pair>> getValueMap() {
        return valueMap;
    }

    public static void reset() {
        if (varList.isEmpty())
            return;
        localMap.clear();
        varList.clear();
        javaVar.clear();
        globalist = new ArrayList<>();
        valueMap = new HashMap<>();
//        LexState.errormsg = null;
//        LexState.globals.clear();
//        LexState.lines.clear();
//        LexState.valueMap.clear();
    }

    public static class Var {
        public String name;
        public String type;
        public int startidx;
        public int endidx;
        public String typename;

        public Var(String n, String t, String m, int s, int e) {
            name = n;
            type = t;
            startidx = s;
            endidx = e;
            typename = m == null ? t : m;
        }

        @Override
        public String toString() {
            return String.format("Var (%s (%s:%s) %s-%s)", name, typename, type, startidx, endidx);
        }
    }

    public static class JavaVar {
        public String name;
        public ArrayList<String> method;
        public int startidx;
        public int endidx;

        public JavaVar(String n, int s, int e) {
            name = n;
            startidx = s;
            endidx = e;
        }
    }

    public static class CharInputSteam extends InputStream {

        private final CharSequence mSrc;
        private final int mLen;
        private int idx = 0;

        public CharInputSteam(CharSequence src) {
            mSrc = src;
            mLen = src.length();
        }

        @Override
        public int read() throws IOException {
            idx++;
            if (idx > mLen)
                return -1;
            return mSrc.charAt(idx - 1);
        }
    }

//    public static boolean lexer(CharSequence src, CompletionPublisher publisher) throws IOException {
//        Log.d("luaj", "lexer: start");
//        try {
//            //Prototype lex = LuaC.lexer(new CharInputSteam(src), "luaj");
//            Prototype lex = LuaC.lexer(src, "luaj", publisher);
//            localMap.clear();
//            varList.clear();
//            javaVar.clear();
//            lexer(lex);
//            if (LexState.erroridx < 0)
//                globalist = new ArrayList<>(LexState.globals);
//            valueMap = new HashMap<>(LexState.valueMap);
//            //Log.i("luaj", "lexer: "+valueMap);
//            return true;
//        } catch (Exception e) {
//            throw e;
//        }
//    }

    public static HashMap<String, ArrayList<Pair>> getLocalMap() {
        return localMap;
    }

//    public static String typename(String n, LocVars l) {
//        VarType type = l.type;
//        // Log.d("luaj", "typename: " + n + ';' + l + l.type);
//        if (type == null)
//            return "";
//        // Log.d("luaj", "typename: " + n + ';' + type.typename);
//        int idx = type.typename.lastIndexOf(".");
//        if (idx < 1)
//            return type.typename;
//        String p = type.typename.substring(0, idx);
//        String c = type.typename.substring(idx + 1);
//        getJavaMethods(c, type.typename);
//        ArrayList<JavaVar> jv = javaVar.get(n);
//        if (jv == null) {
//            jv = new ArrayList<>();
//            javaVar.put(n.toLowerCase(), jv);
//        }
//        jv.add(new JavaVar(type.typename, l.startidx, l.endidx));
//        if (c.equals(n))
//            return p;
//        return c;
//    }
//
//    public static String typename(String n, Upvaldesc l) {
//        VarType type = l.type;
//        if (type == null)
//            return "";
//        //Log.i("luaj", "typename: " + n + ';' + type.typename);
//        int idx = type.typename.lastIndexOf(".");
//        if (idx < 1)
//            return type.typename;
//        String p = type.typename.substring(0, idx);
//        String c = type.typename.substring(idx + 1);
//        if (c.equals(n))
//            return p;
//        return c;
//    }

    private static void getJavaMethods(String c, String typename) {
        ArrayList<Method> ms = javaMethodMap.get(typename);
        if (ms != null)
            return;

        ms = new ArrayList<>();
        try {
            Class<?> clazz = Class.forName(typename);
            Method[] mm = clazz.getMethods();
            for (Method method : mm) {
                String name = method.getName();
                if (ms.contains(name))
                    continue;
                ms.add(method);
            }
            javaMethodMap.put(typename, ms);
        } catch (Exception e) {
            //e.printStackTrace();
        }

        ArrayList<Field> fs = javaFieldMap.get(typename);
        if (fs != null)
            return;
        fs = new ArrayList<>();
        try {
            Class<?> clazz = Class.forName(typename);
            Field[] mm = clazz.getFields();
            for (Field method : mm) {
                String name = method.getName();
                if (fs.contains(name))
                    continue;
                fs.add(method);
            }
            javaFieldMap.put(typename, fs);
        } catch (Exception e) {
            // e.printStackTrace();
        }


    }

//    private static void lexer(Prototype p) {
//        if (p == null)
//            return;
//        LocVars[] ls = p.locvars;
//        int np = p.numparams;
//        Upvaldesc[] us = p.upvalues;
//        for (Upvaldesc l : us) {
//            String n = l.name.tojstring();
//            varList.add(new Var(n, " :upval", l.type == null ? null : l.type.typename, p.startidx, p.endidx));
//        }
//        for (int i = 0; i < ls.length; i++) {
//            LocVars l = ls[i];
//            String n = l.varname.tojstring();
//            if (i < np) {
//                varList.add(new Var(n, " :arg", l.type == null ? null : l.type.typename, l.startidx, l.endidx));
//            } else {
//                typename(n, l);
//                varList.add(new Var(n, " :local", l.type == null ? null : l.type.typename, l.startidx, l.endidx));
//            }
//            ArrayList<Pair> a = localMap.computeIfAbsent(n, k -> new ArrayList<>());
//            a.add(new Pair(l.startidx, l.endidx));
//        }
//
//        Prototype[] ps = p.p;
//        for (Prototype l : ps) {
//            lexer(l);
//        }
//    }

    private static ArrayList<String> userWord = new ArrayList<>();

    public static void clearUserWord() {
        userWord.clear();
    }

    public static ArrayList<String> getUserWord() {
        return userWord;
    }

    public static void addUserWord(String s) {
        userWord.add(s);
    }

    public static ArrayList<CompletionName> filterLocal(String name, int idx) {
        ArrayList<CompletionName> ret = new ArrayList<>();
        ArrayList<CharSequence> ca = new ArrayList<>();

        for (int i = varList.size() - 1; i >= 0; i--) {
            Var var = varList.get(i);
            if (var.startidx <= idx && var.endidx >= idx) {
                String n = var.name;
                if (ca.contains(n))
                    continue;
                ca.add(n);
                if (n.toLowerCase().startsWith(name))
                    ret.add(new CompletionName(n, var.type.equals(" :arg") ? CompletionItemKind.TypeParameter : CompletionItemKind.Variable, var.typename, n));
                String p = getSpells(n);
                if (TextUtils.isEmpty(p))
                    continue;
                if (p.startsWith(name))
                    ret.add(new CompletionName(n, var.type.equals(" :arg") ? CompletionItemKind.TypeParameter : CompletionItemKind.Variable, var.typename, n));
            }
        }
        ArrayList<LuaString> ks = globalist;
        for (LuaValue k : ks) {
            String n = k.tojstring();
            if (ca.contains(n))
                continue;
            ca.add(n);
            if (n.toLowerCase().startsWith(name))
                ret.add(new CompletionName(n, CompletionItemKind.Field, " :global", n));
            String p = getSpells(n);
            if (TextUtils.isEmpty(p))
                continue;
            if (p.startsWith(name))
                ret.add(new CompletionName(n, CompletionItemKind.Field, " :global", n));
        }
        return ret;
    }

    private static int getType(String type) {
        switch (type) {
            case " :upval":
                return UPVAL;
            case " :arg":
            case " :local":
                return LOCAL;
            case " :global":
                return GLOBAL;
        }
        return 0;
    }

    private static CharSequence getColorText(String text, int color) {
        SpannableString ss = new SpannableString(text);
        ss.setSpan(new ForegroundColorSpan(color), 0, text.length(), 0);
        return ss;
    }

    private static final int GB_SP_DIFF = 160;
    private static final int[] secPosValueList = {1601, 1637, 1833, 2078, 2274, 2302,
            2433, 2594, 2787, 3106, 3212, 3472, 3635, 3722, 3730, 3858, 4027,
            4086, 4390, 4558, 4684, 4925, 5249, 5600};
    private static final char[] firstLetter = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
            'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'w', 'x',
            'y', 'z'};

    private static String getSpells(String characters) {
        try {
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < characters.length(); i++) {
                char ch = characters.charAt(i);
                if (i == 0 && ch < 128) {
                    return null;
                }
                if ((ch >> 7) == 0) {
                    buffer.append(ch);
                } else {
                    char spell = getFirstLetter(ch);
                    if (spell == 0) {
                        continue;
                    }
                    buffer.append(String.valueOf(spell));
                }
            }
            return buffer.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static char getFirstLetter(char ch) {
        byte[] uniCode = null;
        try {
            uniCode = String.valueOf(ch).getBytes("GBK");
        } catch (UnsupportedEncodingException e) {
            //e.printStackTrace();
            return 0;
        }
        if (uniCode[0] < 128 && uniCode[0] > 0) {
            return 0;
        } else {
            return convert(uniCode);
        }
    }

    public static char convert(byte[] bytes) {
        char result = 0;
        int secPosValue = 0;
        int i;
        for (i = 0; i < bytes.length; i++) {
            bytes[i] -= GB_SP_DIFF;
        }
        secPosValue = bytes[0] * 100 + bytes[1];
        for (i = 0; i < 23; i++) {
            if (secPosValue >= secPosValueList[i]
                    && secPosValue < secPosValueList[i + 1]) {
                result = firstLetter[i];
                break;
            }
        }
        return result;
    }


}
