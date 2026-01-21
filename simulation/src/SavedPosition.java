import java.io.Serializable;

public class SavedPosition implements Serializable {
    private String name;
    private double x;
    private double y;
    private double heading;
    
    public SavedPosition(String name, double x, double y, double heading) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.heading = heading;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getHeading() {
        return heading;
    }
    
    @Override
    public String toString() {
        return String.format("%s (x=%.1f, y=%.1f, h=%.1f°)", name, x, y, heading);
    }
}
