// StaticTypeCheck.java

import java.util.*;

// Static type checking for Clite is defined by the functions 
// V and the auxiliary functions typing and typeOf.  These
// functions use the classes in the Abstract Syntax of Clite.


public class StaticTypeCheck {
    private static Type returnType;
    private static boolean returnFound = false;
    private static TypeMap functionMap = new TypeMap();
    private static Functions dtFunction = new Functions();
    
    
    public static TypeMap typing (Declarations d) {
        TypeMap map = new TypeMap();
        for (Declaration di : d)
            if (di instanceof VariableDecl) {
                VariableDecl vd = (VariableDecl) di; 
                map.put(vd.v, vd.t);
            }
            
        return map;
    }

    public static void check(boolean test, String msg) {
        if (test)  return;
        System.err.println(msg);
        System.exit(1);
    }

    public static void V (Declarations d) {
        for (int i=0; i<d.size() - 1; i++)
            for (int j=i+1; j<d.size(); j++) {
                Declaration di = d.get(i);
                Declaration dj = d.get(j);
                check( ! (di.v.equals(dj.v)),
                       "duplicate declaration: " + dj.v);
            }
    }
    
    public static void V (Function f){
        
        Declarations ds = new Declarations();
        ds.addAll(f.params);
        ds.addAll(f.locals);
        V(ds); 
    }
    
    public static void V(Program p, TypeMap GM) {
        System.out.println("Globals = {");
        GM.display(null,functionMap);
        dtFunction.addAll(p.functions);
        //타입 규칙 10.1
        Declarations ds = new Declarations();
        ds.addAll(p.globals);
        for (int i=0; i<p.functions.size(); i++) {
            
            Variable fl = new Variable(p.functions.get(i).id);
            ds.add(new VariableDecl(fl, p.functions.get(i).type));
            functionMap.put(fl, p.functions.get(i).type);
        }
        V(ds);
        //타입 규칙 10.2
        for (Function func : p.functions){
            V(func);
        }
        //타입규칙 10.3
        V(p.functions,GM);
    }

    public static void V (Functions f, TypeMap tm) {
        for (Function func : f) {
            TypeMap fMap = new TypeMap();
            fMap.putAll(tm);
            fMap.putAll(typing(func.params));
            fMap.putAll(typing(func.locals));
            
            V(func, fMap);
            fMap.putAll(functionMap);
            System.out.println("Function " + func.id + " = {");
            fMap.display(f,functionMap);
        }
    }
    
    public static void V(Function f, TypeMap tm){
        returnType = f.type;
        returnFound = false;
        V(f.body,tm);

        //타입 규칙 10.4

        if (!(returnType.equals(Type.VOID)) && !f.id.equals("main")){
            check((returnFound == true),
                f.id + " is a non-Void function with no Return Statement");
        }
    }
    
    public static void V (Statement s, TypeMap tm) {
        if ( s == null )
            throw new IllegalArgumentException( "error: null statement");
        if (s instanceof Skip) return;
        if (s instanceof Assignment) {
            Assignment a = (Assignment)s;
            V(a.target, tm);
            V(a.source, tm);
            Type ttype = (Type)tm.get(a.target);
            Type srctype = typeOf(a.source, tm, functionMap);
            if (ttype != srctype) {
                if (ttype == Type.FLOAT)
                    check( srctype == Type.INT
                           , "mixed mode assignment to " + a.target);
                else if (ttype == Type.INT)
                    check( srctype == Type.CHAR
                           , "mixed mode assignment to " + a.target);
                else
                    check( false
                           , "mixed mode assignment to " + a.target);
            }
            return;
        }
        if (s instanceof Conditional) {
            Conditional c = (Conditional)s;
            V (c.test, tm);
            check( typeOf(c.test, tm, functionMap)== Type.BOOL ,
                   "non-bool test in conditional");
            V (c.thenbranch, tm);
            V (c.elsebranch, tm);
            return;
        }
        if (s instanceof Loop) {
            Loop l = (Loop)s;
            V (l.test, tm);
            check(  typeOf(l.test, tm, functionMap)== Type.BOOL ,
                    "loop has non-bool test");
            V (l.body, tm);
            return;
        }
        if (s instanceof Block) {
            Block b = (Block)s;
            for (int j=0; j < b.members.size(); j++)
                V((Statement)(b.members.get(j)), tm);
            return;
        }
        if (s instanceof Return)
        {
            //타입 규칙 10.5
            check(!(returnType.equals(Type.VOID)),    
                "Return is not a valid Statement in a Void Function");
            Return r = (Return)s;
            //타입 규칙 10.4
            check(returnType.equals(typeOf(r.returned,tm, functionMap)),
                "The returned type does not match the fuction type;");
            returnFound = true;
            return;
        }
        if (s instanceof StatementCall) {
            StatementCall c = (StatementCall)s;
            check((functionMap.get(new Variable(c.id))).equals(Type.VOID),
                    "Statement Calls can only be to Void statements");
            for (Function func : dtFunction){
                if (func.id.equals(c.id)){
                    check (c.arg.size() == func.params.size(),
                            "Arguments and Parameters are different size.");
                    for(int i = 0; i < c.arg.size(); i++){
                        Type ti =((Type)func.params.get(i).t);
                        Type tj = typeOf(c.arg.get(i),tm, functionMap); 
                        check(ti.equals(tj)
                                , func.params.get(i).t + " is not equal to " + typeOf(c.arg.get(i),tm, functionMap));
                    }
                }
            }
            return;
        }
        throw new IllegalArgumentException("should never reach here");
    }

    public static void V (Expression e, TypeMap tm) {
        if (e instanceof Value)
            return;
        if (e instanceof Variable) {
            Variable v = (Variable)e;
            check( tm.containsKey(v), "undeclared variable: " + v);
            return;
        }
        if (e instanceof ExpressionCall){
            ExpressionCall c = (ExpressionCall)e;
            check(!(functionMap.get(new Variable(c.id))).equals(Type.VOID),
                    "Expression Calls must have a return type.");
            for (Function func : dtFunction){
                if (func.id.equals(c.id)){
                    check (c.arg.size() == func.params.size(),
                            "Arguments and Parameters are different size.");
                    for(int i = 0; i < c.arg.size(); i++){
                        Type ti =((Type)func.params.get(i).t);
                        Type tj = typeOf(c.arg.get(i),tm, functionMap); 
                        check(ti.equals(tj)
                                , func.params.get(i).t + " is not equal to " + typeOf(c.arg.get(i),tm, functionMap));
                    }
                }
            }
            return;
        }
        if (e instanceof Binary) {
            Binary b = (Binary) e;
            Type typ1 = typeOf(b.term1, tm, functionMap);
            Type typ2 = typeOf(b.term2, tm, functionMap);
            V (b.term1, tm);
            V (b.term2, tm);
            if (b.op.ArithmeticOp( ))
                if (b.op.ModOp())
                    check(typ1 == typ2 && typ1 == Type.INT
                       , "type error for " + b.op);
                else
                    check( typ1 == typ2 &&
                       (typ1 == Type.INT || typ1 == Type.FLOAT)
                       , "type error for " + b.op);
            else if (b.op.RelationalOp( ))
                check( typ1 == typ2 , "type error for " + b.op);
            else if (b.op.BooleanOp( ))
                check( typ1 == Type.BOOL && typ2 == Type.BOOL,
                       b.op + ": non-bool operand");
            else
                throw new IllegalArgumentException("should never reach here");
            return;
        }
        if (e instanceof Unary) {
            Unary u = (Unary) e;
            Type typ1 = typeOf(u.term, tm, functionMap);
            V(u.term, tm);
            if (u.op.NotOp())
                check( typ1 == Type.BOOL , "! has non-bool operand");
            else if (u.op.NegateOp())
                check( typ1 == Type.INT || typ1 == Type.FLOAT
                       , "Unary - has non-int/float operand");
            else if (u.op.floatOp())
                check( typ1== Type.INT, "float() has non-int operand");
            else if (u.op.charOp())
                check( typ1== Type.INT , "char() has non-int operand");
            else if (u.op.intOp())
                check( typ1== Type.FLOAT || typ1== Type.CHAR
                       , "int() has non-float/char operand");
            else
                throw new IllegalArgumentException("should never reach here");
            return;
        }
        throw new IllegalArgumentException("should never reach here");
    }
    
    public static Type typeOf (Expression e, TypeMap tm, TypeMap fm) {
        if (e instanceof Value) return ((Value)e).type;
        if (e instanceof Variable) {
            Variable v = (Variable)e;
            check (tm.containsKey(v), "undefined variable: " + v);
            return (Type) tm.get(v);
        }
        if (e instanceof ExpressionCall){
            ExpressionCall c = (ExpressionCall)e;
            if (functionMap.isEmpty()){
               functionMap = new TypeMap();
               functionMap.putAll(fm);
            }
            check (functionMap.containsKey(new Variable(c.id)), "undefined variable: " + c.id);
            return (Type) functionMap.get(new Variable(c.id));
        }
        if (e instanceof Binary) {
            Binary b = (Binary)e;
            if (b.op.ArithmeticOp( ))
                if (typeOf(b.term1,tm, functionMap)== Type.FLOAT)
                    return (Type.FLOAT);
                else return (Type.INT);
            if (b.op.RelationalOp( ) || b.op.BooleanOp( ))
                return (Type.BOOL);
        }
        if (e instanceof Unary) {
            Unary u = (Unary)e;
            if (u.op.NotOp( ))        return (Type.BOOL);
            else if (u.op.NegateOp( )) return typeOf(u.term,tm , functionMap);
            else if (u.op.intOp( ))    return (Type.INT);
            else if (u.op.floatOp( )) return (Type.FLOAT);
            else if (u.op.charOp( ))  return (Type.CHAR);
        }
        throw new IllegalArgumentException("should never reach here");
    }

} // class StaticTypeCheck