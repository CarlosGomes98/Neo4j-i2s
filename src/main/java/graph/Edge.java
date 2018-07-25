package graph;

import java.util.Objects;

/**
 * Describes an edge in the visualization
 * @author i2scmg
 *
 */
public class Edge {
    private final String source;
    private final String target;
    private double averageTime;
    private long instances = 0;
    
    public Edge(String source, String target){
        this.source = source;
        this.target = target;
    }
    
    public String getSource(){
        return source;
    }
    
    public String getTarget(){
        return target;
    }
    
    public void setAverageTime(double time){
        this.averageTime = time;
    }
    
    public double getAverageTime(){
        return averageTime;
    }
    
    public void setInstances(long instances){
        this.instances = instances;
    }
    
    public long getInstances()
    {
        return instances;
    }
    @Override
    public boolean equals(Object o){
        if (o instanceof Edge) {
            Edge c = (Edge) o;
            return this.getSource().equals(c.getSource()) && this.getTarget().equals(c.getTarget());
          }
          return false;  
        }
    
   @Override
   public int hashCode(){
       return Objects.hash(source, target);
   }
}
