/**
 * @file States.java
 * @author - id19ohn, id19erd, id19llt, id19jlf
 * @date - 2021-10-01
 * @version - v1.0
 */

package aiplanning;
import finitestatemachine.State;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class States implements State {
    private Point point;
    private ArrayList<Point> surroundings;

    public States (Point point) {
        this.point = point;
        surroundings = new ArrayList<>();
    }

    public ArrayList<Point> getSurroundings() {

        surroundings.add(new Point((int)point.getX(), (int)point.getY()-1)); //NORTH
        surroundings.add(new Point((int)point.getX()+1, (int)point.getY())); //EAST
        surroundings.add(new Point((int)point.getX(), (int)point.getY()+1)); //SOUTH
        surroundings.add(new Point((int)point.getX()-1, (int)point.getY())); //WEST

        return surroundings;
    }

    public Point getPoint(){
        return point;
    }

}