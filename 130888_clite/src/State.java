import java.util.*;

public class State extends HashMap<VariableRef, ArrayList<Value>> {

  public State( ) { }

  public State update(VariableRef key, Value val) {
      if (key instanceof Variable) {
          ArrayList<Value> value = get(key);
          value.set(0, val);
      }
      else 
          throw new IllegalArgumentException("not a variable reference");
      return this;
  }

  public State update (State t) {
      for (VariableRef key : t.keySet( ))
          put(key, t.get(key));
      return this;
  }

  public void display( ) {
      System.out.print("{ ");
      String sep = "";
//      }
      for (VariableRef key : keySet( )) {
          if (key instanceof Variable)
              System.out.print(sep + "<" + key + ", " + get(key).get(0) + ">");
          else
              System.out.print(sep + "<" + key + ", " + get(key) + ">");
          sep = ", ";
      }
      System.out.println(" }");
  }
}

